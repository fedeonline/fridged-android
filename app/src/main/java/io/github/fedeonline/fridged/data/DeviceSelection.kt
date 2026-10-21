package io.github.fedeonline.fridged.data

import android.content.Context
import androidx.core.content.edit
import io.github.fedeonline.fridged.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which fridge the app shows. Switchable only in debug builds (docs/PLAN.md section 6). */
interface DeviceSelection {
    val device: StateFlow<Device>

    /** Null when the device is fixed (release builds). */
    val select: ((Device) -> Unit)?
}

class FixedDeviceSelection(device: Device) : DeviceSelection {
    override val device: StateFlow<Device> = MutableStateFlow(device)
    override val select: ((Device) -> Unit)? = null
}

/** Remembers the choice across restarts. */
class PreferencesDeviceSelection(context: Context, default: Device) : DeviceSelection {
    private val prefs = context.getSharedPreferences("device_selection", Context.MODE_PRIVATE)
    private val selected = MutableStateFlow(
        Device.entries.firstOrNull { it.id == prefs.getString(KEY, null) } ?: default,
    )

    override val device: StateFlow<Device> = selected.asStateFlow()

    override val select: ((Device) -> Unit) = { device ->
        prefs.edit { putString(KEY, device.id) }
        selected.value = device
    }

    private companion object {
        const val KEY = "device"
    }
}
