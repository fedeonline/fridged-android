package io.github.fedeonline.fridged.data.firestore

import java.time.Duration
import java.time.Instant

/**
 * Which part of a history range must come from the server (docs/PLAN.md section 5:
 * "history queries fetch only samples newer than the latest one already cached").
 */
object IncrementalFetch {
    /**
     * Re-read this much before the latest cached sample, in case a late upload (spool
     * backlog, retries) landed just before it.
     */
    val OVERLAP: Duration = Duration.ofMinutes(30)

    /**
     * Where the server query starts for a range beginning at [since].
     *
     * @param coveredSince the cache holds every sample from here to [latestCached].
     * @param latestCached the newest sample in the cache for this range, if any.
     */
    fun serverStart(since: Instant, coveredSince: Instant?, latestCached: Instant?): Instant {
        if (coveredSince == null || coveredSince > since || latestCached == null) return since
        return maxOf(since, latestCached.minus(OVERLAP))
    }

    /** The coverage after the server answered a query starting at [serverStart]. */
    fun coverageAfter(since: Instant, serverStart: Instant, coveredSince: Instant?): Instant? = when {
        // The whole range came from the server: it is now covered.
        serverStart == since -> minOf(since, coveredSince ?: since)
        else -> coveredSince
    }
}
