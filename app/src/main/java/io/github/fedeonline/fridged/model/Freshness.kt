package io.github.fedeonline.fridged.model

import java.time.Duration
import java.time.Instant

/** Three missed upload rounds of 5 minutes (docs/PLAN.md section 2). */
val STALE_AFTER: Duration = Duration.ofMinutes(15)

/**
 * How old the sample is at [now]. Never negative: a phone clock slightly behind the Pi
 * would otherwise show "updated in 1 min".
 */
fun Sample.age(now: Instant): Duration = Duration.between(time, now).coerceAtLeast(Duration.ZERO)

/** True when the sample is more than [STALE_AFTER] old: the daemon stopped publishing. */
fun Sample.isStale(now: Instant): Boolean = age(now) > STALE_AFTER
