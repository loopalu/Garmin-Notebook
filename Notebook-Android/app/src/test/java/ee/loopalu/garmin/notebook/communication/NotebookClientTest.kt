package ee.loopalu.garmin.notebook.communication

import ee.loopalu.garmin.notebook.testsupport.FakeGarminClientInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class NotebookClientTest {

    @Test
    fun acceptsOnlyOneRequestUntilMatchingResponseArrives() = runTest {
        val clientInterface = FakeGarminClientInterface()
        val responses = mutableListOf<NotebookResponse>()
        val client = NotebookClient(
            clientInterface,
            this,
            onResponse = responses::add,
            onFailure = {}
        )
        val listDirectoriesRequest = NotebookCommunicationService.listDirectoriesRequest()

        assertTrue(client.sendRequest(listDirectoriesRequest))
        assertFalse(client.sendRequest(NotebookCommunicationService.listDirectoriesRequest()))
        val response = successfulResponse(listDirectoriesRequest)
        assertTrue(client.handleResponse(response))
        assertEquals(listOf(response), responses)
        assertFalse(client.isRequestTransmissionOngoing)
    }

    @Test
    fun failsRequestWhenWatchDoesNotRespond() = runTest {
        val garminClient = FakeGarminClientInterface()
        val failures = mutableListOf<String>()
        val notebookClient = NotebookClient(
            garminClient,
            this,
            responseTimeoutMs = 100,
            onResponse = {},
            onFailure = failures::add
        )

        notebookClient.sendRequest(NotebookCommunicationService.listDirectoriesRequest())
        garminClient.completeLastSend()
        advanceTimeBy(100.milliseconds)
        runCurrent()

        assertEquals(listOf("The watch did not respond to the request."), failures)
        assertFalse(notebookClient.isRequestTransmissionOngoing)
    }

    private fun successfulResponse(request: Map<String, Any>) = NotebookResponse(
        requestId = request.getValue("requestId") as String,
        operation = request.getValue("operation") as String,
        isSuccess = true,
        error = null,
        directories = emptyList()
    )
}
