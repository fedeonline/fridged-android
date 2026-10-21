package io.github.fedeonline.fridged.ui.history

import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ChartDataTest {
    private val end = Instant.parse("2026-10-20T08:00:00Z")
    private val utc = ZoneOffset.UTC
    private val base = FakeData.history(end, span = Duration.ZERO).single()

    /** One sample per 5 minutes ending at [end], built from [specs]. */
    private fun samples(vararg specs: Sample.() -> Sample): List<Sample> = specs.mapIndexed { i, spec ->
        base.copy(time = end.minus(Duration.ofMinutes(5L * (specs.size - 1 - i)))).spec()
    }

    private fun data(samples: List<Sample>) = ChartData.of(samples, end, Duration.ofHours(1), utc)

    @Test
    fun `x is local wall-clock minutes`() {
        assertEquals((end.epochSecond / 60).toDouble(), chartX(end, utc), 0.0)
        assertEquals((end.epochSecond / 60 + 120).toDouble(), chartX(end, ZoneOffset.ofHours(2)), 0.0)
    }

    @Test
    fun `missing readings split the curve`() {
        val runs = data(
            samples(
                { copy(temperature = 4.0) },
                { copy(temperature = 4.5) },
                { copy(temperature = null) },
                { copy(temperature = 5.0) },
                { copy(temperature = 5.5) },
            ),
        ).runs
        assertEquals(listOf(listOf(4.0, 4.5), listOf(5.0, 5.5)), runs.map { run -> run.map { it.second } })
    }

    @Test
    fun `a hole in the samples splits the curve and leaves no background`() {
        val first = base.copy(time = end.minus(Duration.ofMinutes(50)), temperature = 4.0, relay = RelayState.ON)
        val second = base.copy(time = end, temperature = 4.0, relay = RelayState.ON)
        val data = data(listOf(first, second))
        assertEquals(2, data.runs.size)
        // The first sample stands for at most 15 minutes, not for the whole hole.
        assertEquals(15.0, data.segments.first().let { it.endX - it.startX }, 0.0)
    }

    @Test
    fun `segments merge while nothing changes and split on band, compressor or mode`() {
        val segments = data(
            samples(
                { copy(relay = RelayState.OFF) },
                { copy(relay = RelayState.OFF) },
                { copy(relay = RelayState.ON) },
                { copy(relay = RelayState.ON, band = Band(2.0, 4.0)) },
                { copy(relay = RelayState.ON, band = Band(2.0, 4.0), mode = Mode.SAFE) },
                // The last sample sits on the end of the range: it has no width.
                { copy(relay = RelayState.ON, band = Band(2.0, 4.0)) },
            ),
        ).segments
        assertEquals(
            listOf(false to 10.0, true to 5.0, true to 5.0, true to 5.0),
            segments.map { it.relayOn to it.endX - it.startX },
        )
        assertEquals(listOf(null, null, Band(2.0, 4.0), Band(2.0, 4.0)).map { it ?: base.band }, segments.map { it.band })
        assertEquals(listOf(false, false, false, true), segments.map { it.safe })
    }

    @Test
    fun `y range covers readings and band with a margin`() {
        val data = data(samples({ copy(temperature = 6.2, band = Band(3.0, 5.0)) }))
        assertEquals(2.0, data.minY, 0.0)
        assertEquals(7.0, data.maxY, 0.0)
    }

    @Test
    fun `a week of fake data has gaps and SAFE segments`() {
        val week = ChartData.of(FakeData.history(end), end, Duration.ofDays(7), utc)
        assertTrue(week.runs.size > 1)
        assertTrue(week.segments.any { it.safe })
        assertTrue(week.segments.any { it.relayOn })
    }
}
