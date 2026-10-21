package io.github.fedeonline.fridged.ui.status

import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class StatusUiStateTest {
    private val now = Instant.parse("2026-10-20T08:00:00Z")
    private val history = FakeData.history(now, span = Duration.ofDays(1))
    private val latest = history.last()

    private fun ready(status: Sample = latest, at: Instant = now) =
        StatusUiState.from(status, history, at) as StatusUiState.Ready

    @Test
    fun `no status document means no data`() {
        assertEquals(StatusUiState.NoData, StatusUiState.from(null, history, now))
    }

    @Test
    fun `a normal fresh status has no banners`() {
        val state = ready()
        assertEquals(emptyList<Banner>(), state.banners)
        assertEquals(Duration.ZERO, state.age)
    }

    @Test
    fun `banners appear when they should, most important first`() {
        assertEquals(listOf(Banner.OVERRUN_FAULT), ready(latest.copy(overrunFault = true)).banners)
        assertEquals(listOf(Banner.SAFE_MODE), ready(latest.copy(mode = Mode.SAFE)).banners)
        assertEquals(listOf(Banner.STALE), ready(at = now.plus(Duration.ofMinutes(16))).banners)
        assertEquals(
            listOf(Banner.OVERRUN_FAULT, Banner.SAFE_MODE, Banner.STALE),
            ready(latest.copy(overrunFault = true, mode = Mode.SAFE), at = now.plus(Duration.ofHours(1))).banners,
        )
    }

    @Test
    fun `not stale at exactly 15 minutes`() {
        assertEquals(emptyList<Banner>(), ready(at = now.plus(Duration.ofMinutes(15))).banners)
    }

    @Test
    fun `recent keeps only the last 6 hours`() {
        val state = ready(at = now)
        assertEquals(6 * 12 + 1, state.recent.size)
        assertTrue(state.recent.all { it.time >= now.minus(RECENT_SPAN) })
    }
}
