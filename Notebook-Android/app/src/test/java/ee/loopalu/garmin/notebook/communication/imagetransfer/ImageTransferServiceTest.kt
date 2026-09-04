package ee.loopalu.garmin.notebook.communication.imagetransfer

import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.BEGIN_IMAGE_TRANSFER
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.COMMIT_IMAGE_TRANSFER
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.IMAGE_CHUNK
import ee.loopalu.garmin.notebook.communication.NotebookResponse
import ee.loopalu.garmin.notebook.image.EncodedImage
import ee.loopalu.garmin.notebook.image.Rgb222Encoder
import ee.loopalu.garmin.notebook.testsupport.FakeGarminClientInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageTransferServiceTest {
    private val image = EncodedImage(
        "image",
        2,
        1,
        Rgb222Encoder.palette,
        byteArrayOf(1, 2)
    )

    @Test
    fun sendsBeginChunkAndCommitAndReportsCommitAsCompleted() = runTest {
        val garminClient = FakeGarminClientInterface()
        val progress = mutableListOf<Int>()
        val completed = mutableListOf<NotebookResponse>()
        val imageTransferService = ImageTransferService(
            garminClient,
            this,
            onProgress = progress::add,
            onCompleted = completed::add,
            onFailure = {}
        )

        assertTrue(imageTransferService.startImageTransfer(
            ImageTransferPlanner.createTransferPlan("d1", image, "transfer-1")))
        acknowledgeLastRequest(garminClient, imageTransferService)
        acknowledgeLastRequest(garminClient, imageTransferService)
        assertEquals(listOf(0, 33, 66), progress)
        acknowledgeLastRequest(garminClient, imageTransferService)

        assertEquals(
            listOf(BEGIN_IMAGE_TRANSFER, IMAGE_CHUNK, COMMIT_IMAGE_TRANSFER),
            garminClient.sentRequests.map {
                it["operation"]
            }
        )
        assertEquals(listOf(0, 33, 66, 100), progress)
        assertEquals(1, completed.size)
        assertFalse(imageTransferService.isImageTransferOngoing)
    }

    @Test
    fun retriesFailedTransferAndStopsAfterSetLimit() = runTest {
        val client = FakeGarminClientInterface()
        val failures = mutableListOf<String>()
        val coordinator = ImageTransferService(
            client,
            this,
            maximumAttempts = 2,
            onProgress = {},
            onCompleted = {},
            onFailure = failures::add
        )

        coordinator.startImageTransfer(ImageTransferPlanner.createTransferPlan("d1", image, "transfer-1"))
        client.completeLastSend(result = Result.failure(IllegalStateException("transport failed")))
        runCurrent()
        client.completeLastSend(result = Result.failure(IllegalStateException("transport failed")))
        runCurrent()

        assertEquals(2, client.sentRequests.size)
        assertEquals(listOf("transport failed"), failures)
        assertFalse(coordinator.isImageTransferOngoing)
    }

    private fun acknowledgeLastRequest(client: FakeGarminClientInterface, service: ImageTransferService) {
        val request = client.sentRequests.last()
        client.completeLastSend()
        service.handleWatchResponse(
            NotebookResponse(
                requestId = request.getValue("requestId") as String,
                operation = request.getValue("operation") as String,
                isSuccess = true,
                error = null,
                directories = if (request["operation"] == COMMIT_IMAGE_TRANSFER) emptyList() else null
            )
        )
    }
}
