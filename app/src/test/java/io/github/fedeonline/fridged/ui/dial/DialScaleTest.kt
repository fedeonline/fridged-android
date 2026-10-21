package io.github.fedeonline.fridged.ui.dial

import io.github.fedeonline.fridged.model.Band
import org.junit.Assert.assertEquals
import org.junit.Test

class DialScaleTest {
    @Test
    fun `scale is 10 degrees wide, centred on the band`() {
        assertEquals(DialScale(4.0, 14.0), DialScale.around(Band(8.0, 11.0), 8.6))
        assertEquals(DialScale(0.0, 10.0), DialScale.around(Band(3.0, 5.0), null))
    }

    @Test
    fun `scale stays within 0 to 25`() {
        assertEquals(DialScale(0.0, 10.0), DialScale.around(Band(0.0, 1.0), null))
        assertEquals(DialScale(15.0, 25.0), DialScale.around(Band(24.0, 25.0), null))
    }

    @Test
    fun `without a band the scale centres on the temperature`() {
        assertEquals(DialScale(1.0, 11.0), DialScale.around(null, 6.3))
    }

    @Test
    fun `angles run clockwise from the bottom left over 270 degrees`() {
        val scale = DialScale(4.0, 14.0)
        assertEquals(135f, scale.angle(4.0), 0.001f)
        assertEquals(270f, scale.angle(9.0), 0.001f)
        assertEquals(405f, scale.angle(14.0), 0.001f)
        // Values off the scale stick to its ends.
        assertEquals(135f, scale.angle(-2.0), 0.001f)
        assertEquals(405f, scale.angle(30.0), 0.001f)
    }
}
