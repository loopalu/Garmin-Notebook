package ee.loopalu.garmin.notebook.communication.imagetransfer

import android.app.Application
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.BEGIN_IMAGE_TRANSFER
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.COMMIT_IMAGE_TRANSFER
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.IMAGE_CHUNK
import ee.loopalu.garmin.notebook.image.EncodedImage
import ee.loopalu.garmin.notebook.image.Rgb222Encoder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageTransferPlannerTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    private val maximumImageDimension =
        application.resources.getInteger(R.integer.maximum_image_dimension_pixels)

    private val image = EncodedImage(
        name = "photo.png",
        width = maximumImageDimension,
        height = maximumImageDimension,
        palette = Rgb222Encoder.palette,
        pixelPaletteIndexes = ByteArray(maximumImageDimension * maximumImageDimension) {
            (it % 64).toByte()
        }
    )

    private val expectedPixelCount = maximumImageDimension * maximumImageDimension

    private val expectedChunkCount = (expectedPixelCount + ImageTransferPlanner.CHUNK_SIZE - 1) /
                ImageTransferPlanner.CHUNK_SIZE

    @Test
    fun splitsMaximumSizeImageIntoChunks() {
        val transferPlan = ImageTransferPlanner.createTransferPlan("d1", image, "transfer-1")

        assertEquals(expectedChunkCount, transferPlan.chunkCount)
        assertEquals((expectedChunkCount + 2), transferPlan.stepCount)
        val chunks = (0 until transferPlan.chunkCount).map(transferPlan::getChunkOfPaletteIndexes)
        assertTrue(chunks.all {
            it.size <= ImageTransferPlanner.CHUNK_SIZE
        })
        assertArrayEquals(image.pixelPaletteIndexes,
            chunks.fold(ByteArray(0)) {
                all, chunk ->
                all + chunk
            })
    }
    @Test
    fun createsBeginChunkAndCommitRequests() {
        val transferPlan = ImageTransferPlanner.createTransferPlan("d1", image, "transfer-1")
        val beginRequest = transferPlan.createImageTransferRequest(0)
        val chunkRequest = transferPlan.createImageTransferRequest(1)
        val commitRequest = transferPlan.createImageTransferRequest(transferPlan.stepCount - 1)

        assertEquals(BEGIN_IMAGE_TRANSFER, beginRequest["operation"])
        assertEquals(expectedPixelCount, beginRequest["totalBytes"])
        assertEquals(expectedChunkCount, beginRequest["totalChunks"])
        assertEquals(IMAGE_CHUNK, chunkRequest["operation"])
        assertEquals(2_048, Base64.getDecoder().decode(chunkRequest["data"] as String).size)
        assertEquals(COMMIT_IMAGE_TRANSFER, commitRequest["operation"])
        assertEquals(transferPlan.checksum.sumOfByteValues, commitRequest["checksumA"])
        assertEquals(transferPlan.checksum.cumulativeSum, commitRequest["checksumB"])
    }

    @Test
    fun checksumMatchesKnownAdlerComponents() {
        assertEquals(ImageChecksum(7, 13),
            ImageTransferPlanner.getChecksum(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun reportsChunkProgress() {
        val transferPlan = ImageTransferPlanner.createTransferPlan("d1", image, "transfer-1")

        assertEquals(10, transferPlan.calculateImageTransferPercentage(0))
        assertEquals(20, transferPlan.calculateImageTransferPercentage(1))
        assertEquals(90, transferPlan.calculateImageTransferPercentage(8))
        assertEquals(100, transferPlan.calculateImageTransferPercentage(9))
    }
}
