package io.github.fedeonline.fridged.data.firestore

import io.github.fedeonline.fridged.data.firestore.IncrementalFetch.OVERLAP
import io.github.fedeonline.fridged.data.firestore.IncrementalFetch.coverageAfter
import io.github.fedeonline.fridged.data.firestore.IncrementalFetch.serverStart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class IncrementalFetchTest {
    private val now = Instant.parse("2026-10-21T12:00:00Z")
    private val dayAgo = now.minus(Duration.ofDays(1))
    private val weekAgo = now.minus(Duration.ofDays(7))
    private val latest = now.minus(Duration.ofHours(2))

    @Test
    fun `first open reads the whole range from the server`() {
        assertEquals(dayAgo, serverStart(dayAgo, coveredSince = null, latestCached = null))
        assertEquals(dayAgo, coverageAfter(dayAgo, dayAgo, coveredSince = null))
    }

    @Test
    fun `reopening a covered range reads only after the latest cached sample`() {
        assertEquals(latest.minus(OVERLAP), serverStart(dayAgo, coveredSince = weekAgo, latestCached = latest))
        assertEquals(weekAgo, coverageAfter(dayAgo, latest.minus(OVERLAP), weekAgo))
    }

    @Test
    fun `a wider range than the cache covers is read whole, then covered`() {
        assertEquals(weekAgo, serverStart(weekAgo, coveredSince = dayAgo, latestCached = latest))
        assertEquals(weekAgo, coverageAfter(weekAgo, weekAgo, dayAgo))
    }

    @Test
    fun `an empty cache is read whole even if marked covered`() {
        assertEquals(dayAgo, serverStart(dayAgo, coveredSince = weekAgo, latestCached = null))
    }

    @Test
    fun `the overlap never starts before the range`() {
        val justAfter = dayAgo.plus(Duration.ofMinutes(5))
        assertEquals(dayAgo, serverStart(dayAgo, coveredSince = weekAgo, latestCached = justAfter))
    }
}
