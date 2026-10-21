package io.github.fedeonline.fridged.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import io.github.fedeonline.fridged.widget.WidgetData.Line
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class WidgetDataTest {
    private val now = Instant.parse("2026-10-21T14:00:00Z")
    private val sample = FakeData.history(now, span = Duration.ZERO).single()

    private fun status(checkedAfter: Duration = Duration.ZERO, edit: (Sample) -> Sample = { it }) =
        WidgetData.of(edit(sample), now.plus(checkedAfter)) as WidgetData.Status

    @Test
    fun `no status means no data`() {
        assertEquals(WidgetData.NoData, WidgetData.of(null, now))
    }

    @Test
    fun `compressor line`() {
        assertEquals(Line.COMPRESSOR_ON, status { it.copy(relay = RelayState.ON) }.line)
        assertEquals(Line.COMPRESSOR_OFF, status { it.copy(relay = RelayState.OFF) }.line)
        assertFalse(status().isWarning)
    }

    @Test
    fun `warnings win, stale first`() {
        assertEquals(Line.SAFE, status { it.copy(mode = Mode.SAFE) }.line)
        assertEquals(Line.OVERRUN, status { it.copy(mode = Mode.SAFE, overrunFault = true) }.line)
        assertEquals(Line.STALE, status(Duration.ofMinutes(16)) { it.copy(overrunFault = true) }.line)
        assertTrue(status(Duration.ofMinutes(16)).isWarning)
    }

    @Test
    fun `fresh up to exactly 15 minutes after the sample`() {
        assertEquals(Line.COMPRESSOR_OFF, status(Duration.ofMinutes(15)) { it.copy(relay = RelayState.OFF) }.line)
    }

    @Test
    fun `state round trip`() {
        for (data in listOf(
            WidgetData.SignedOut,
            WidgetData.NoData,
            status { it.copy(relay = RelayState.ON, mode = Mode.SAFE, overrunFault = true) },
            status { it.copy(temperature = null) },
        )) {
            val prefs = mutablePreferencesOf()
            WidgetData.write(data, prefs)
            assertEquals(data, WidgetData.read(prefs))
        }
    }

    @Test
    fun `nothing saved yet`() {
        assertNull(WidgetData.read(mutablePreferencesOf()))
    }
}
