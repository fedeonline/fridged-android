package io.github.fedeonline.fridged.data.fake

import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Reason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FakeFridgeRepositoryTest {
    private val now = Instant.parse("2026-10-20T08:00:00Z")
    private val history = FakeData.history(now)
    private val device = Device.SIMULATED

    @Test
    fun `history has one sample per round over a week`() {
        assertEquals(7 * 288 + 1, history.size)
        assertEquals(now, history.last().time)
        assertTrue(history.zipWithNext().all { (a, b) -> Duration.between(a.time, b.time) == FakeData.ROUND })
    }

    @Test
    fun `history is deterministic`() {
        assertEquals(history, FakeData.history(now))
    }

    @Test
    fun `history covers the cases charts must draw`() {
        assertTrue(history.any { it.temperature == null && it.reason == Reason.STALE })
        assertTrue(history.any { it.mode == Mode.SAFE })
        val readings = history.mapNotNull { it.temperature }
        assertTrue(readings.all { it in 2.0..6.0 })
        assertTrue(history.any { it.relay.name == "ON" } && history.any { it.relay.name == "OFF" })
    }

    @Test
    fun `status is the latest sample and samples are filtered by time`() = runTest {
        val repo = FakeFridgeRepository(history)
        assertEquals(history.last(), repo.status(device).first())
        val lastDay = repo.samples(device, now.minus(Duration.ofDays(1))).first()
        assertEquals(289, lastDay.size)
    }

    @Test
    fun `setting and clearing the band`() = runTest {
        val repo = FakeFridgeRepository(history)
        assertNull(repo.settings(device).first())
        repo.setBand(device, Band(2.5, 4.5))
        assertEquals(Band(2.5, 4.5), repo.settings(device).first())
        repo.clearBand(device)
        assertNull(repo.settings(device).first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid bands are refused`() = runTest {
        FakeFridgeRepository(history).setBand(device, Band(3.0, 3.2))
    }

    @Test
    fun `a daemon round applies the settings, or the config band without them`() = runTest {
        val repo = FakeFridgeRepository(history, configBand = Band(4.0, 6.0))
        repo.setBand(device, Band(2.5, 4.5))
        repo.simulateDaemonRound(now.plus(FakeData.ROUND))
        assertEquals(Band(2.5, 4.5), repo.status(device).first()?.band)

        repo.clearBand(device)
        repo.simulateDaemonRound(now.plus(FakeData.ROUND.multipliedBy(2)))
        assertEquals(Band(4.0, 6.0), repo.status(device).first()?.band)
    }
}
