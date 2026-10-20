package io.github.fedeonline.fridged.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTest {
    @Test
    fun `fromId maps the ids published by fridged`() {
        assertEquals(Device.FRIDGE, Device.fromId("fridge"))
        assertEquals(Device.SIMULATED, Device.fromId("fridge-sim"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromId rejects unknown ids`() {
        Device.fromId("freezer")
    }
}
