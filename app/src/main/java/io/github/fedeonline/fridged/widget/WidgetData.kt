package io.github.fedeonline.fridged.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.STALE_AFTER
import io.github.fedeonline.fridged.model.Sample
import java.time.Duration
import java.time.Instant

/** What the widget shows, as saved in its Glance state by [WidgetRefreshWorker]. */
sealed interface WidgetData {
    data object SignedOut : WidgetData

    data object NoData : WidgetData

    data class Status(
        val time: Instant,
        /** °C; null without a reading. */
        val temperature: Double?,
        val compressorOn: Boolean,
        val safe: Boolean,
        val overrunFault: Boolean,
        /** When the widget last refreshed: staleness is judged against this. */
        val checkedAt: Instant,
    ) : WidgetData {
        /** The line under the temperature, most important first. */
        val line: Line
            get() = when {
                Duration.between(time, checkedAt) > STALE_AFTER -> Line.STALE
                overrunFault -> Line.OVERRUN
                safe -> Line.SAFE
                compressorOn -> Line.COMPRESSOR_ON
                else -> Line.COMPRESSOR_OFF
            }

        val isWarning: Boolean get() = line in setOf(Line.STALE, Line.OVERRUN, Line.SAFE)
    }

    enum class Line { COMPRESSOR_ON, COMPRESSOR_OFF, SAFE, OVERRUN, STALE }

    companion object {
        private val KIND = stringPreferencesKey("kind")
        private val TIME = longPreferencesKey("time")
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val COMPRESSOR_ON = booleanPreferencesKey("compressorOn")
        private val SAFE = booleanPreferencesKey("safe")
        private val OVERRUN = booleanPreferencesKey("overrun")
        private val CHECKED_AT = longPreferencesKey("checkedAt")

        fun of(sample: Sample?, checkedAt: Instant): WidgetData = sample?.let {
            Status(
                time = it.time,
                temperature = it.temperature,
                compressorOn = it.relay == RelayState.ON,
                safe = it.mode == Mode.SAFE,
                overrunFault = it.overrunFault,
                checkedAt = checkedAt,
            )
        } ?: NoData

        /** Null when nothing was saved yet (the widget was just added). */
        fun read(prefs: Preferences): WidgetData? = when (prefs[KIND]) {
            "signedOut" -> SignedOut
            "noData" -> NoData
            "status" -> Status(
                time = Instant.ofEpochMilli(prefs[TIME] ?: return null),
                temperature = prefs[TEMPERATURE],
                compressorOn = prefs[COMPRESSOR_ON] ?: false,
                safe = prefs[SAFE] ?: false,
                overrunFault = prefs[OVERRUN] ?: false,
                checkedAt = Instant.ofEpochMilli(prefs[CHECKED_AT] ?: return null),
            )
            else -> null
        }

        fun write(data: WidgetData, prefs: MutablePreferences) {
            prefs.clear()
            when (data) {
                SignedOut -> prefs[KIND] = "signedOut"
                NoData -> prefs[KIND] = "noData"
                is Status -> {
                    prefs[KIND] = "status"
                    prefs[TIME] = data.time.toEpochMilli()
                    data.temperature?.let { prefs[TEMPERATURE] = it }
                    prefs[COMPRESSOR_ON] = data.compressorOn
                    prefs[SAFE] = data.safe
                    prefs[OVERRUN] = data.overrunFault
                    prefs[CHECKED_AT] = data.checkedAt.toEpochMilli()
                }
            }
        }
    }
}
