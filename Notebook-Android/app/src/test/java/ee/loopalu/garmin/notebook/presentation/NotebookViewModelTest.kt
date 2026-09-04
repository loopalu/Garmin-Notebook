package ee.loopalu.garmin.notebook.presentation

import android.app.Application
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.LIST_DIRECTORIES
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.VERSION
import ee.loopalu.garmin.notebook.domain.model.GarminConnectionState
import ee.loopalu.garmin.notebook.testsupport.FakeGarminClientInterface
import ee.loopalu.garmin.notebook.testsupport.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotebookViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun connectedWatchIsRequestedAndSnapshotUpdatesUiState() = runTest(mainDispatcherRule.dispatcher) {
        val garminClient = FakeGarminClientInterface()
        val notebookView = viewModel(garminClient)
        runCurrent()

        garminClient.mutableConnection.value = GarminConnectionState.Connected("Test watch")
        runCurrent()
        val request = garminClient.sentRequests.single()
        assertEquals(LIST_DIRECTORIES, request["operation"])
        assertTrue(notebookView.uiState.value.isBusy)

        garminClient.emitMessage(createSuccessfulSnapshotResponse(request))
        runCurrent()

        assertEquals("Trips", notebookView.uiState.value.directories.single().name)
        assertFalse(notebookView.uiState.value.isBusy)
    }

    @Test
    fun busyNotebookViewDoesNotSendAnOverlappingOperation() = runTest(mainDispatcherRule.dispatcher) {
        val garminClient = FakeGarminClientInterface()
        val notebookView = viewModel(garminClient)
        runCurrent()

        garminClient.mutableConnection.value = GarminConnectionState.Connected("Test watch")
        runCurrent()

        notebookView.createDirectory("Another")

        assertEquals(1, garminClient.sentRequests.size)
        assertEquals("Wait for the current watch operation to finish", notebookView.uiState.value.message)
    }

    private fun viewModel(client: FakeGarminClientInterface) = NotebookViewModel(
        Application(),
        client
    ) {
        error("Image encoding is not used by this test")
    }

    private fun createSuccessfulSnapshotResponse(request: Map<String, Any>): Map<String, Any> = mapOf(
        "v" to VERSION,
        "type" to "response",
        "requestId" to request.getValue("requestId"),
        "operation" to request.getValue("operation"),
        "ok" to true,
        "directories" to listOf(mapOf("id" to "d1", "name" to "Trips", "items" to emptyList<Any>()))
    )
}
