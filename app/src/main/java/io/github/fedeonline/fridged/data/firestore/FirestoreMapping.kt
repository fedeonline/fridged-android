package io.github.fedeonline.fridged.data.firestore

import com.google.firebase.Timestamp
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Reason
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample

/**
 * Converts document data, as returned by `DocumentSnapshot.getData()`, into models.
 * Malformed documents map to null instead of throwing: one bad sample must not break
 * the whole screen.
 */
object FirestoreMapping {

    fun sample(fields: Map<String, Any?>): Sample? {
        return Sample(
            time = (fields["time"] as? Timestamp)?.toInstant() ?: return null,
            clockSynced = fields["clockSynced"] as? Boolean ?: return null,
            timeCorrected = fields["timeCorrected"] as? Boolean ?: false,
            bootId = fields["bootId"] as? String ?: return null,
            uptimeMs = (fields["uptimeMs"] as? Number)?.toLong() ?: return null,
            temperature = (fields["temperature"] as? Number)?.toDouble(),
            relay = relayState(fields["relay"]) ?: return null,
            wanted = relayState(fields["wanted"]) ?: return null,
            reason = reason(fields["reason"]),
            mode = mode(fields["mode"]) ?: return null,
            overrunFault = fields["overrunFault"] as? Boolean ?: return null,
            band = band(fields),
        )
    }

    /**
     * The band in a settings document or a sample, not validated: the console can store
     * anything, and the UI shows what the daemon will do with it. The console stores
     * whole numbers as integers, hence [Number].
     */
    fun band(fields: Map<String, Any?>): Band? {
        val min = (fields["targetMin"] as? Number)?.toDouble() ?: return null
        val max = (fields["targetMax"] as? Number)?.toDouble() ?: return null
        return Band(min, max)
    }

    /** The data written to `settings/current`: exactly these two fields. */
    fun settingsData(band: Band): Map<String, Any> = mapOf("targetMin" to band.min, "targetMax" to band.max)

    private fun relayState(value: Any?): RelayState? = when (value) {
        "ON" -> RelayState.ON
        "OFF" -> RelayState.OFF
        else -> null
    }

    private fun mode(value: Any?): Mode? = when (value) {
        "NORMAL" -> Mode.NORMAL
        "SAFE" -> Mode.SAFE
        else -> null
    }

    private fun reason(value: Any?): Reason = when (value) {
        "aboveMax" -> Reason.ABOVE_MAX
        "atOrBelowMin" -> Reason.AT_OR_BELOW_MIN
        "inBand" -> Reason.IN_BAND
        "stale" -> Reason.STALE
        "safeOn" -> Reason.SAFE_ON
        "safeOff" -> Reason.SAFE_OFF
        else -> Reason.UNKNOWN
    }
}
