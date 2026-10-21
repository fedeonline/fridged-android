package io.github.fedeonline.fridged.model

import java.time.Instant

enum class RelayState { ON, OFF }

enum class Mode { NORMAL, SAFE }

/** Why the controller wants what it wants. */
enum class Reason {
    ABOVE_MAX,
    AT_OR_BELOW_MIN,
    IN_BAND,

    /** Reading missing, but not for long enough to give up on the sensor. */
    STALE,
    SAFE_ON,
    SAFE_OFF,

    /** A reason newer than this app: shown generically instead of hiding the status. */
    UNKNOWN,
}

/**
 * One telemetry record published by the daemon (docs/data-contract.md).
 * `status/current` is the latest sample, with the same shape.
 */
data class Sample(
    val time: Instant,
    val clockSynced: Boolean,
    val timeCorrected: Boolean,
    val bootId: String,
    val uptimeMs: Long,
    /** °C; null when the sensor gave no reading on that tick. */
    val temperature: Double?,
    /** What the relay actually is, after the compressor protections. */
    val relay: RelayState,
    /** What the controller asked for. */
    val wanted: RelayState,
    val reason: Reason,
    val mode: Mode,
    val overrunFault: Boolean,
    /** The band in force; null only in samples spooled before the band was recorded. */
    val band: Band?,
) {
    /** The compressor protections are holding the controller's command. */
    val heldByProtections: Boolean get() = wanted != relay
}
