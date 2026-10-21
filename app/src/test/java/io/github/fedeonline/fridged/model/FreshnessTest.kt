package io.github.fedeonline.fridged.model

import io.github.fedeonline.fridged.data.fake.FakeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FreshnessTest {
    private val time = Instant.parse("2026-10-20T08:00:00Z")
    private val sample = FakeData.history(end = time, span = Duration.ZERO).single()

    @Test
    fun `age is the time since the sample`() {
        assertEquals(Duration.ofMinutes(7), sample.age(time.plus(Duration.ofMinutes(7))))
    }

    @Test
    fun `fresh up to exactly 15 minutes`() {
        assertFalse(sample.isStale(time))
        assertFalse(sample.isStale(time.plus(Duration.ofMinutes(15))))
    }

    @Test
    fun `stale after 15 minutes`() {
        assertTrue(sample.isStale(time.plus(Duration.ofMinutes(15)).plusMillis(1)))
        assertTrue(sample.isStale(time.plus(Duration.ofHours(3))))
    }

    @Test
    fun `phone clock behind the Pi gives age zero, not negative`() {
        val before = time.minus(Duration.ofMinutes(2))
        assertEquals(Duration.ZERO, sample.age(before))
        assertFalse(sample.isStale(before))
    }
}
