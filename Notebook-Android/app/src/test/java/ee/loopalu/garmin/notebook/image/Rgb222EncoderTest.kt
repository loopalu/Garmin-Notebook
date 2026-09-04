package ee.loopalu.garmin.notebook.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Rgb222EncoderTest {
    @Test
    fun paletteMatchesRgb222EncodingOrder() {
        assertEquals(64, Rgb222Encoder.palette.size)
        assertEquals(0x000000, Rgb222Encoder.palette.first())
        assertEquals(0x000055, Rgb222Encoder.palette[1])
        assertEquals(0x005500, Rgb222Encoder.palette[4])
        assertEquals(0x550000, Rgb222Encoder.palette[16])
        assertEquals(0xFFFFFF, Rgb222Encoder.palette.last())
    }

    @Test
    fun paletteColorsKeepTheirIndexes() {
        val pixels = intArrayOf(
            0xFF000000.toInt(),
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
            0xFFFFFFFF.toInt()
        )

        assertArrayEquals(byteArrayOf(0, 48, 12, 3, 63), Rgb222Encoder.encode(5, 1, pixels))
    }

    @Test
    fun ditheringUsesNeighboringLevelsForMidGray() {
        val encodedIndexes = Rgb222Encoder.encode(8, 8, IntArray(64) { 0xFF808080.toInt() })
        val paletteIndexesInEncodedImage = encodedIndexes.map {
            it.toInt() and 0xFF
        }.toSet()

        assertTrue(21 in paletteIndexesInEncodedImage)
        assertTrue(42 in paletteIndexesInEncodedImage)
        assertTrue(encodedIndexes.all {
            (it.toInt() and 0xFF) in 0..63
        })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMismatchedPixelData() {
        Rgb222Encoder.encode(2, 2, IntArray(3))
    }
}
