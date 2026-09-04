package ee.loopalu.garmin.notebook.image

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import ee.loopalu.garmin.notebook.R
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageEncoderTest {

    private companion object {
        const val RED_PALETTE_INDEX = 48
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val application: Application by lazy {
        RuntimeEnvironment.getApplication()
    }
    private val maximumImageDimension by lazy {
        application.resources.getInteger(R.integer.maximum_image_dimension_pixels)
    }
    private val encoder by lazy {
        ImageEncoder(
            contentResolver = application.contentResolver,
            maximumImageDimension = maximumImageDimension
        )
    }

    @Test
    fun encodesLandscapeImageWithinWatchDimensions() {
        val image = createEncodedImage(width = 400, height = 300)

        assertEncodedImage(
            image,
            expectedWidth = maximumImageDimension,
            expectedHeight = (maximumImageDimension * 0.75).roundToInt()
        )
    }

    @Test
    fun encodesPortraitImageWithinWatchDimensions() {
        val image = createEncodedImage(width = 300, height = 400)

        assertEncodedImage(
            image,
            expectedWidth = (maximumImageDimension * 0.75).roundToInt(),
            expectedHeight = maximumImageDimension
        )
    }

    @Test
    fun preservesPanoramicAspectRatioWhileEncoding() {
        val image = createEncodedImage(width = 400, height = 10)

        assertEncodedImage(
            image,
            expectedWidth = maximumImageDimension,
            expectedHeight = (maximumImageDimension * 10.0 / 400).roundToInt().coerceAtLeast(1)
        )
    }

    @Test
    fun doesNotEnlargeSmallImage() {
        val image = createEncodedImage(width = 32, height = 20)

        assertEncodedImage(image, expectedWidth = 32, expectedHeight = 20)
    }

    private fun createEncodedImage(width: Int, height: Int): EncodedImage {
        val file = createPng(width, height)
        return encoder.encode(Uri.fromFile(file))
    }

    private fun createPng(width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val file = temporaryFolder.newFile("${width}x$height.png")
        try {
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    private fun assertEncodedImage(image: EncodedImage, expectedWidth: Int, expectedHeight: Int) {
        assertEquals(expectedWidth, image.width)
        assertEquals(expectedHeight, image.height)
        assertEquals(expectedWidth * expectedHeight, image.pixelPaletteIndexes.size)
        assertEquals(Rgb222Encoder.palette, image.palette)
        assertTrue(image.pixelPaletteIndexes.all {
            (it.toInt() and 0xFF) == RED_PALETTE_INDEX
        })
    }
}
