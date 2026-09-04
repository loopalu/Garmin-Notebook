package ee.loopalu.garmin.notebook.image

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class EncodedImage(
    val name: String,
    val width: Int,
    val height: Int,
    val palette: List<Int>,
    // Index values on palette for given pixels
    val pixelPaletteIndexes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as EncodedImage

        if (width != other.width) {
            return false
        }
        if (height != other.height) {
            return false
        }
        if (name != other.name) {
            return false
        }
        if (palette != other.palette) {
            return false
        }
        if (!pixelPaletteIndexes.contentEquals(other.pixelPaletteIndexes)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + name.hashCode()
        result = 31 * result + palette.hashCode()
        result = 31 * result + pixelPaletteIndexes.contentHashCode()
        return result
    }
}

private data class WatchImageDimensions(val width: Int, val height: Int)

fun interface SelectedImageEncoder {
    fun encode(uri: Uri): EncodedImage
}

class ImageEncoder(
    private val contentResolver: ContentResolver,
    private val maximumImageDimension: Int,
) : SelectedImageEncoder {

    init {
        require(maximumImageDimension > 0) {
            "Maximum image dimension must be positive."
        }
    }

    override fun encode(uri: Uri): EncodedImage {
        val originalImage = contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) {
                "Unable to open the selected image."
            }
            requireNotNull(BitmapFactory.decodeStream(stream)) {
                "The selected file is not a supported image."
            }
        }

        try {
            val scaledDimensions = scaleDimensions(originalImage.width, originalImage.height)
            val width = scaledDimensions.width
            val height = scaledDimensions.height
            val bitmap = originalImage.scale(width, height)
            try {
                val sourcePixels = IntArray(width * height)
                bitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
                return EncodedImage(
                    name = getDisplayName(uri),
                    width = width,
                    height = height,
                    palette = Rgb222Encoder.palette,
                    pixelPaletteIndexes = Rgb222Encoder.encode(width, height, sourcePixels)
                )
            } finally {
                if (bitmap !== originalImage) {
                    bitmap.recycle()
                }
            }
        } finally {
            originalImage.recycle()
        }
    }

    private fun getDisplayName(imagePath: Uri): String {
        contentResolver.query(imagePath, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)?.takeIf(String::isNotBlank) ?: "image"
                }
            }
        }
        return "image"
    }

    private fun scaleDimensions(width: Int, height: Int): WatchImageDimensions {
        require(width > 0 && height > 0) {
            "Image dimensions must be positive."
        }

        val scale = min(1.0, maximumImageDimension.toDouble() / max(width, height))
        return WatchImageDimensions(
            width = (width * scale).roundToInt().coerceIn(1, maximumImageDimension),
            height = (height * scale).roundToInt().coerceIn(1, maximumImageDimension)
        )
    }
}
