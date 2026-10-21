package io.github.fedeonline.fridged.model

import io.github.fedeonline.fridged.data.fake.FakeData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HistoryStatsTest {
    private val base = FakeData.history(Instant.parse("2026-10-20T08:00:00Z"), span = Duration.ZERO).single()

    private fun sample(t: Double?, relay: RelayState) = base.copy(temperature = t, relay = relay, wanted = relay)

    @Test
    fun `min, max, mean and duty cycle`() {
        val stats = HistoryStats.of(
            listOf(
                sample(3.0, RelayState.OFF),
                sample(5.0, RelayState.ON),
                sample(null, RelayState.ON),
                sample(4.0, RelayState.OFF),
            ),
        )
        assertEquals(HistoryStats(min = 3.0, max = 5.0, mean = 4.0, dutyCycle = 0.5), stats)
    }

    @Test
    fun `no readings`() {
        assertEquals(
            HistoryStats(null, null, null, dutyCycle = 1.0),
            HistoryStats.of(listOf(sample(null, RelayState.ON))),
        )
        assertEquals(HistoryStats(null, null, null, null), HistoryStats.of(emptyList()))
    }
}
