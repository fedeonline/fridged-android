package io.github.fedeonline.fridged.ui.history

import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class HistoryBarsTest {
    private val utc = ZoneOffset.UTC
    private val end = Instant.parse("2026-10-21T19:28:00Z")
    private val base = FakeData.history(end, span = Duration.ZERO).single().copy(band = Band(8.0, 11.0))

    private fun at(time: String, t: Double?, relay: RelayState = RelayState.OFF, mode: Mode = Mode.NORMAL): Sample =
        base.copy(time = Instant.parse(time), temperature = t, relay = relay, wanted = relay, mode = mode)

    @Test
    fun `buckets per range, aligned to the clock, the last one holding now`() {
        val day = HistoryBars.of(emptyList(), end, HistoryRange.DAY, utc)
        assertEquals(24, day.bars.size)
        assertEquals(Instant.parse("2026-10-21T19:00:00Z"), day.bars.last().start)
        assertEquals(Instant.parse("2026-10-20T20:00:00Z"), day.bars.first().start)

        assertEquals(24, HistoryBars.of(emptyList(), end, HistoryRange.SIX_HOURS, utc).bars.size)
        assertEquals(Instant.parse("2026-10-21T19:15:00Z"), HistoryBars.of(emptyList(), end, HistoryRange.SIX_HOURS, utc).bars.last().start)

        val week = HistoryBars.of(emptyList(), end, HistoryRange.WEEK, utc)
        assertEquals(28, week.bars.size)
        assertEquals(Instant.parse("2026-10-21T18:00:00Z"), week.bars.last().start)
    }

    @Test
    fun `alignment follows local time`() {
        val rome = ZoneId.of("Europe/Rome") // UTC+2 on this date
        assertEquals(
            Instant.parse("2026-10-21T16:00:00Z"), // 18:00 local
            HistoryBars.floorToBucket(end, Duration.ofHours(6), rome),
        )
    }

    @Test
    fun `mean, duty cycle and tone per bucket`() {
        val bars = HistoryBars.of(
            listOf(
                at("2026-10-21T18:05:00Z", 9.0, RelayState.ON),
                at("2026-10-21T18:35:00Z", 10.0, RelayState.OFF),
                at("2026-10-21T19:05:00Z", 11.5, RelayState.ON),
                at("2026-10-21T19:10:00Z", 11.7, RelayState.ON),
            ),
            end,
            HistoryRange.DAY,
            utc,
        ).bars
        val eighteen = bars[bars.size - 2]
        assertEquals(9.5, eighteen.mean!!, 1e-9)
        assertEquals(0.5, eighteen.dutyCycle!!, 1e-9)
        assertEquals(BarTone.IN_BAND, eighteen.tone)
        assertEquals(BarTone.ABOVE, bars.last().tone)
        assertEquals(1.0, bars.last().dutyCycle!!, 1e-9)
    }

    @Test
    fun `below the band, SAFE mode, empty buckets`() {
        val bars = HistoryBars.of(
            listOf(
                at("2026-10-21T17:05:00Z", 7.6),
                at("2026-10-21T18:05:00Z", null, RelayState.ON, Mode.SAFE),
            ),
            end,
            HistoryRange.DAY,
            utc,
        ).bars
        assertEquals(BarTone.BELOW, bars[bars.size - 3].tone)
        assertEquals(BarTone.SAFE, bars[bars.size - 2].tone)
        assertNull(bars[bars.size - 2].mean)
        val empty = bars.last()
        assertNull(empty.mean)
        assertNull(empty.dutyCycle)
        assertNull(empty.tone)
    }

    @Test
    fun `y range covers the means and the band with a margin`() {
        val chart = HistoryBars.of(listOf(at("2026-10-21T19:05:00Z", 6.2)), end, HistoryRange.DAY, utc)
        assertEquals(5.0, chart.minY, 0.0)
        assertEquals(12.0, chart.maxY, 0.0)
        assertEquals(Band(8.0, 11.0), chart.band)
    }

    @Test
    fun `a week of fake data fills every bucket`() {
        val week = HistoryBars.of(FakeData.history(end), end, HistoryRange.WEEK, utc)
        assertTrue(week.bars.all { it.dutyCycle != null })
        assertTrue(week.bars.any { it.tone == BarTone.SAFE })
    }
}
