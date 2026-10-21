package io.github.fedeonline.fridged.data.fake

import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Reason
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/** Deterministic, plausible fridge data for previews and tests. */
object FakeData {
    val DEFAULT_BAND = Band(3.0, 5.0)
    val ROUND: Duration = Duration.ofMinutes(5)

    private const val BOOT_ID = "fake0001"
    private const val WARMING_PER_ROUND = 0.15
    private const val COOLING_PER_ROUND = 0.35

    /** MCP9808 resolution. */
    private fun quantize(t: Double) = (t / 0.0625).roundToInt() * 0.0625

    /**
     * One sample per upload round over [span] up to [end], from a simple thermostat.
     * Includes the cases charts must handle: a gap without readings (`STALE`) about two
     * days before [end], and a SAFE period about four days before.
     */
    fun history(end: Instant, span: Duration = Duration.ofDays(7), band: Band = DEFAULT_BAND): List<Sample> {
        val rounds = (span.toMinutes() / ROUND.toMinutes()).toInt()
        val gap = (rounds - 2 * 288)..(rounds - 2 * 288 + 6)
        val safe = (rounds - 4 * 288)..(rounds - 4 * 288 + 12)
        var temperature = (band.min + band.max) / 2
        var relay = RelayState.OFF
        return (0..rounds).map { i ->
            temperature += if (relay == RelayState.ON) -COOLING_PER_ROUND else WARMING_PER_ROUND
            val inSafe = i in safe
            // The thermostat keeps regulating during the gap, as the daemon does on `stale`:
            // only the reading is missing.
            var reason = when {
                temperature > band.max -> Reason.ABOVE_MAX.also { relay = RelayState.ON }
                temperature <= band.min -> Reason.AT_OR_BELOW_MIN.also { relay = RelayState.OFF }
                else -> Reason.IN_BAND
            }
            if (inSafe) {
                relay = if (i % 2 == 0) RelayState.ON else RelayState.OFF
                reason = if (relay == RelayState.ON) Reason.SAFE_ON else Reason.SAFE_OFF
            } else if (i in gap) {
                reason = Reason.STALE
            }
            val reading = if (inSafe || i in gap) null else quantize(temperature)
            val mode = if (inSafe) Mode.SAFE else Mode.NORMAL
            Sample(
                time = end.minus(ROUND.multipliedBy((rounds - i).toLong())),
                clockSynced = true,
                timeCorrected = false,
                bootId = BOOT_ID,
                uptimeMs = (i + 1) * ROUND.toMillis(),
                temperature = reading,
                relay = relay,
                wanted = relay,
                reason = reason,
                mode = mode,
                overrunFault = false,
                band = band,
            )
        }
    }
}
