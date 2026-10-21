package io.github.fedeonline.fridged.data.firestore

import com.google.firebase.Timestamp
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Reason
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FirestoreMappingTest {
    private fun sample(fixture: String): Sample = checkNotNull(FirestoreMapping.sample(Fixtures.fields(fixture)))

    @Test
    fun `maps every field of a status document`() {
        assertEquals(
            Sample(
                time = Instant.parse("2026-10-20T08:15:00Z"),
                clockSynced = true,
                timeCorrected = false,
                bootId = "204aae0c",
                uptimeMs = 3_600_000,
                temperature = 6.3125,
                relay = RelayState.ON,
                wanted = RelayState.ON,
                reason = Reason.ABOVE_MAX,
                mode = Mode.NORMAL,
                overrunFault = false,
                band = Band(3.0, 5.0),
            ),
            sample("status-cooling.json"),
        )
    }

    @Test
    fun `status held by protections`() {
        val s = sample("status-held.json")
        assertEquals(RelayState.ON, s.wanted)
        assertEquals(RelayState.OFF, s.relay)
        assertTrue(s.heldByProtections)
    }

    @Test
    fun `SAFE mode without a reading`() {
        val s = sample("status-safe-no-reading.json")
        assertEquals(Mode.SAFE, s.mode)
        assertEquals(Reason.SAFE_OFF, s.reason)
        assertNull(s.temperature)
    }

    @Test
    fun `overrun fault`() {
        assertTrue(sample("status-overrun.json").overrunFault)
    }

    @Test
    fun `legacy sample without band keeps millisecond time`() {
        val s = sample("sample-legacy-no-band.json")
        assertNull(s.band)
        assertTrue(s.timeCorrected)
        assertEquals(Instant.parse("2026-10-15T19:58:00.123Z"), s.time)
    }

    @Test
    fun `settings written by the app`() {
        assertEquals(Band(2.5, 4.5), FirestoreMapping.band(Fixtures.fields("settings-double.json")))
    }

    @Test
    fun `settings typed in the console as integers`() {
        assertEquals(Band(3.0, 6.0), FirestoreMapping.band(Fixtures.fields("settings-console-integers.json")))
    }

    @Test
    fun `settings data has exactly the two band fields`() {
        assertEquals(
            mapOf("targetMin" to 2.5, "targetMax" to 4.5),
            FirestoreMapping.settingsData(Band(2.5, 4.5)),
        )
    }

    @Test
    fun `unknown reason does not hide the status`() {
        val fields = Fixtures.fields("status-cooling.json") + ("reason" to "defrost")
        assertEquals(Reason.UNKNOWN, FirestoreMapping.sample(fields)?.reason)
    }

    @Test
    fun `missing timeCorrected defaults to false`() {
        val fields = Fixtures.fields("status-cooling.json") - "timeCorrected"
        assertFalse(checkNotNull(FirestoreMapping.sample(fields)).timeCorrected)
    }

    @Test
    fun `malformed documents map to null`() {
        val good = Fixtures.fields("status-cooling.json")
        for (broken in listOf(
            good - "time",
            good + ("time" to "2026-10-20T08:15:00Z"),
            good - "bootId",
            good + ("relay" to "MAYBE"),
            good + ("mode" to null),
            good + ("uptimeMs" to "3600000"),
            emptyMap(),
        )) {
            assertNull(broken.toString(), FirestoreMapping.sample(broken))
        }
    }

    @Test
    fun `band needs both numeric bounds`() {
        assertNull(FirestoreMapping.band(mapOf("targetMin" to 3.0)))
        assertNull(FirestoreMapping.band(mapOf("targetMin" to "3", "targetMax" to 5.0)))
        assertNull(FirestoreMapping.band(emptyMap()))
    }

    @Test
    fun `time comes from the timestamp, not timeMs`() {
        val fields = Fixtures.fields("status-cooling.json")
        assertEquals((fields["time"] as Timestamp).toInstant().toEpochMilli(), fields["timeMs"])
    }
}
