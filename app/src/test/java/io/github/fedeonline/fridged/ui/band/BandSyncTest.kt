package io.github.fedeonline.fridged.ui.band

import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.Band
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BandSyncTest {
    private val t0 = Instant.parse("2026-10-21T09:00:00Z")
    private val status = FakeData.history(t0, span = Duration.ZERO).single().copy(band = Band(3.0, 5.0))
    private fun statusAt(minutes: Long, band: Band? = status.band) =
        status.copy(time = t0.plus(Duration.ofMinutes(minutes)), band = band)

    @Test
    fun `steps stay on the half-degree grid`() {
        assertEquals(3.5, stepUp(3.0), 0.0)
        assertEquals(2.5, stepDown(3.0), 0.0)
        assertEquals(3.5, stepUp(3.3), 0.0)
        assertEquals(3.0, stepDown(3.3), 0.0)
        assertEquals(0.5, stepUp(0.0), 0.0)
        assertEquals(24.5, stepDown(25.0), 0.0)
        // Every grid band 0.5 apart is accepted, unlike some one-decimal pairs.
        var min = 0.0
        while (min <= 24.5) {
            assertTrue("$min", Band.problems(min, stepUp(min)).isEmpty())
            min = stepUp(min)
        }
    }

    @Test
    fun `waiting until the status reports the target`() {
        val target = Band(2.5, 4.5)
        val waiting = Sync.Waiting(target)
        assertEquals(waiting, Sync.next(waiting, statusAt(1), target))
        assertEquals(Sync.Applied(target, statusAt(2).time), Sync.next(waiting, statusAt(2, target), target))
    }

    @Test
    fun `settings that differ from the applied band mean waiting, even after a restart`() {
        val target = Band(2.5, 4.5)
        assertEquals(Sync.Waiting(target), Sync.next(Sync.Idle, statusAt(0), target))
        assertEquals(Sync.Idle, Sync.next(Sync.Idle, statusAt(0), status.band))
        assertEquals(Sync.Idle, Sync.next(Sync.Idle, statusAt(0), null))
    }

    @Test
    fun `restoring confirms when the band changes`() {
        val restoring = Sync.Restoring(since = t0, previous = Band(2.5, 4.5), lastSeen = t0)
        assertEquals(
            Sync.Applied(Band(3.0, 5.0), statusAt(1).time),
            Sync.next(restoring, statusAt(1, Band(3.0, 5.0)), null),
        )
    }

    @Test
    fun `restoring to the band already in force confirms on the second new status`() {
        var sync: Sync = Sync.Restoring(since = t0, previous = status.band, lastSeen = t0)
        sync = Sync.next(sync, statusAt(0), null)
        assertTrue(sync is Sync.Restoring)
        sync = Sync.next(sync, statusAt(1), null)
        sync = Sync.next(sync, statusAt(1), null) // same status again: not new
        assertTrue(sync is Sync.Restoring)
        sync = Sync.next(sync, statusAt(2), null)
        assertEquals(Sync.Applied(status.band, statusAt(2).time), sync)
    }
}
