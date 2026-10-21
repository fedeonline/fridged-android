package io.github.fedeonline.fridged.ui.band

import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Sample
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor

/** Where a band change stands between the app, Firestore and the daemon. */
sealed interface Sync {
    data object Idle : Sync

    /** The daemon has not applied [target] yet. */
    data class Waiting(val target: Band) : Sync

    /**
     * `settings/current` was deleted at [since]; the daemon's default band is unknown to the
     * app, so this waits for the band to change from [previous] or for two newer statuses.
     */
    data class Restoring(val since: Instant, val previous: Band?, val lastSeen: Instant?, val newStatuses: Int = 0) : Sync

    /** The daemon reported [band] in the status taken at [at]. */
    data class Applied(val band: Band?, val at: Instant) : Sync

    /** The write was refused or failed. */
    data object Failed : Sync

    companion object {
        /**
         * The next state when the status or the settings change. Waiting is derived from the
         * data too (settings differ from the applied band), so it survives an app restart.
         */
        fun next(sync: Sync, status: Sample?, settings: Band?): Sync {
            val applied = status?.band
            return when (sync) {
                is Waiting -> if (applied == sync.target) Applied(applied, status.time) else sync
                is Restoring -> {
                    if (status == null) return sync
                    val isNew = status.time > sync.since && status.time != sync.lastSeen
                    val count = sync.newStatuses + if (isNew) 1 else 0
                    when {
                        status.time > sync.since && applied != sync.previous -> Applied(applied, status.time)
                        count >= 2 -> Applied(applied, status.time)
                        else -> sync.copy(lastSeen = status.time, newStatuses = count)
                    }
                }
                Idle, is Applied, Failed ->
                    if (settings != null && status != null && applied != settings) Waiting(settings) else sync
            }
        }
    }
}

/** Band bounds move on a 0.5 °C grid: exact in binary, so no float edge cases (docs/data-contract.md). */
const val STEP = 0.5

/** The next grid value above [value]; an off-grid value moves to the grid. */
fun stepUp(value: Double): Double = (floor(value / STEP) + 1) * STEP

/** The next grid value below [value]; an off-grid value moves to the grid. */
fun stepDown(value: Double): Double = (ceil(value / STEP) - 1) * STEP
