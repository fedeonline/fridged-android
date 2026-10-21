package io.github.fedeonline.fridged.data

import android.content.Context
import androidx.core.content.edit
import io.github.fedeonline.fridged.model.Device
import java.time.Instant

/**
 * How far back the local Firestore cache holds every sample of a device, up to the latest
 * cached one. Lets history queries ask the server only for what is missing.
 */
interface SampleCoverage {
    fun coveredSince(device: Device): Instant?

    fun setCoveredSince(device: Device, since: Instant)
}

class PreferencesSampleCoverage(context: Context) : SampleCoverage {
    private val prefs = context.getSharedPreferences("sample_coverage", Context.MODE_PRIVATE)

    override fun coveredSince(device: Device): Instant? =
        prefs.getLong(device.id, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)

    override fun setCoveredSince(device: Device, since: Instant) {
        prefs.edit { putLong(device.id, since.toEpochMilli()) }
    }
}
