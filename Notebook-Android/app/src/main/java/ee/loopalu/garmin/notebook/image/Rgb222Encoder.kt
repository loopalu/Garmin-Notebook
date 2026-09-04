package ee.loopalu.garmin.notebook.image

import kotlin.math.max
import kotlin.math.min

/** Converts ARGB pixels to Garmin's 64-color RGB222 transport palette. */
object Rgb222Encoder {
    /** Intensity step. In RGB222 there are 64 colors: 4x4x4 where 4 is the number of possible
     * intensities of red/green/blue color channel. 0x55 hexadecimal stands for 85. And that 85
     * is the allowed intensity change step: 0, 85, 170, 255. The rest 256 intensity values are
     * rounded to the closest intensity change value. */
    private const val CHANNEL_STEP = 0x55
    /** 0xFF hexadecimal stands for 255 which is the maximum intensity value of a color channel. */
    private const val MAX_CHANNEL_VALUE = 0xFF
    private const val NUMBER_OF_PERMITTED_INTENSITY_LEVELS_IN_CHANNEL = 4

    /** Common denominator of the Floyd-Steinberg weights used to store and recover color error. */
    private const val ERROR_SCALE = 16

    /** Half of [ERROR_SCALE], added before division to round error adjustments to the nearest integer. */
    private const val ERROR_ROUNDING_OFFSET = ERROR_SCALE / 2

    /** Numerator of the 7/16 error share assigned to the pixel immediately to the right. */
    private const val RIGHT_PIXEL_WEIGHT = 7

    /** Numerator of the 3/16 error share assigned to the next row's left-hand pixel. */
    private const val NEXT_LEFT_PIXEL_WEIGHT = 3

    /** Numerator of the 5/16 error share assigned to the pixel directly below. */
    private const val NEXT_PIXEL_WEIGHT = 5

    /** Numerator of the 1/16 error share assigned to the next row's right-hand pixel. */
    private const val NEXT_RIGHT_PIXEL_WEIGHT = 1

    /** Extra buffer positions that allow left- and right-neighbour indexing at image boundaries. */
    private const val ERROR_BUFFER_PADDING = 2

    val palette: List<Int> = buildList(64) {
        for (red in 0 until NUMBER_OF_PERMITTED_INTENSITY_LEVELS_IN_CHANNEL) {
            for (green in 0 until NUMBER_OF_PERMITTED_INTENSITY_LEVELS_IN_CHANNEL) {
                for (blue in 0 until NUMBER_OF_PERMITTED_INTENSITY_LEVELS_IN_CHANNEL) {
                    add((red * CHANNEL_STEP shl 16) or (green * CHANNEL_STEP shl 8) or (blue * CHANNEL_STEP))
                }
            }
        }
    }

    /** Encodes the image using Floyd-Steinberg dithering. */
    fun encode(width: Int, height: Int, sourcePixels: IntArray): ByteArray {
        require(width > 0 && height > 0) {
            "Image dimensions must be positive."
        }
        require(sourcePixels.size == width * height) {
            "Pixel data does not match image dimensions."
        }

        val output = ByteArray(sourcePixels.size)
        var currentRed = IntArray(width + ERROR_BUFFER_PADDING)
        var currentGreen = IntArray(width + ERROR_BUFFER_PADDING)
        var currentBlue = IntArray(width + ERROR_BUFFER_PADDING)
        var nextRed = IntArray(width + ERROR_BUFFER_PADDING)
        var nextGreen = IntArray(width + ERROR_BUFFER_PADDING)
        var nextBlue = IntArray(width + ERROR_BUFFER_PADDING)

        for (y in 0 until height) {
            nextRed.fill(0)
            nextGreen.fill(0)
            nextBlue.fill(0)

            for (x in 0 until width) {
                val color = sourcePixels[y * width + x]
                val alpha = color ushr 24 and MAX_CHANNEL_VALUE
                val red = adjustChannel((color ushr 16 and MAX_CHANNEL_VALUE) * alpha /
                        MAX_CHANNEL_VALUE, currentRed[x + 1])
                val green = adjustChannel((color ushr 8 and MAX_CHANNEL_VALUE) * alpha /
                        MAX_CHANNEL_VALUE, currentGreen[x + 1])
                val blue = adjustChannel((color and MAX_CHANNEL_VALUE) * alpha /
                        MAX_CHANNEL_VALUE, currentBlue[x + 1])

                val redLevel = quantize(red)
                val greenLevel = quantize(green)
                val blueLevel = quantize(blue)
                output[y * width + x] = (redLevel * 16 + greenLevel * 4 + blueLevel).toByte()

                // TODO Make diffusion optional and selectable with an option to show image preview
                // TODO before the image is sent to the watch. For QR codex, text, icons and
                // TODO limited color images diffusion is unwanted
                diffuse(red - redLevel * CHANNEL_STEP, x, currentRed, nextRed)
                diffuse(green - greenLevel * CHANNEL_STEP, x, currentGreen, nextGreen)
                diffuse(blue - blueLevel * CHANNEL_STEP, x, currentBlue, nextBlue)
            }

            currentRed = nextRed.also {
                nextRed = currentRed
            }
            currentGreen = nextGreen.also {
                nextGreen = currentGreen
            }
            currentBlue = nextBlue.also {
                nextBlue = currentBlue
            }
        }
        return output
    }

    /**
     * Applies error from previously encoded pixels to one source color channel.
     *
     * @param channel source channel intensity in the range from zero through [MAX_CHANNEL_VALUE].
     * @param accumulatedError weighted Floyd-Steinberg error stored at the current pixel.
     * @return error-adjusted channel intensity mapped to 8-bit range.
     */
    private fun adjustChannel(channel: Int, accumulatedError: Int): Int =
        min(MAX_CHANNEL_VALUE, max(0, channel + divideBy16Rounded(accumulatedError)))

    /**
     * Converts a weighted error into a channel-intensity adjustment by dividing it by
     * [ERROR_SCALE] and rounding to the nearest integer.
     *
     * @param value accumulated error multiplied by Floyd-Steinberg weight numerators.
     * @return the signed, rounded error adjustment to apply to a color channel.
     */
    private fun divideBy16Rounded(value: Int): Int =
        if (value >= 0) {
            (value + ERROR_ROUNDING_OFFSET) / ERROR_SCALE
        } else {
            (value - ERROR_ROUNDING_OFFSET) / ERROR_SCALE
        }

    /**
     * Maps an 8-bit channel intensity to the nearest RGB222 intensity level.
     *
     * @param channel channel intensity in the range from zero through [MAX_CHANNEL_VALUE].
     * @return the RGB222 level index in the range from zero through three.
     */
    private fun quantize(channel: Int): Int =
        ((channel + CHANNEL_STEP / 2) / CHANNEL_STEP)
            .coerceIn(0, NUMBER_OF_PERMITTED_INTENSITY_LEVELS_IN_CHANNEL - 1)

    /**
     * Distributes one channel's quantization error to the four unprocessed neighbouring pixels.
     *
     * The weighted values remain multiplied by [ERROR_SCALE] while stored in the buffers.
     *
     * @param error difference between the adjusted source intensity and its RGB222 intensity.
     * @param x horizontal position of the pixel that produced the error.
     * @param current error buffer for the row currently being encoded.
     * @param next error buffer for the following row.
     */
    private fun diffuse(error: Int, x: Int, current: IntArray, next: IntArray) {
        current[x + 2] += error * RIGHT_PIXEL_WEIGHT
        next[x] += error * NEXT_LEFT_PIXEL_WEIGHT
        next[x + 1] += error * NEXT_PIXEL_WEIGHT
        next[x + 2] += error * NEXT_RIGHT_PIXEL_WEIGHT
    }
}
