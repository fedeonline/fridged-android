package io.github.fedeonline.fridged.model

/**
 * Summary of a history range. Samples are evenly spaced (one per upload round), so plain
 * averages over samples are time averages.
 */
data class HistoryStats(
    /** °C; null without readings. */
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    /** Share of samples with the compressor on, 0..1; null without samples. */
    val dutyCycle: Double?,
) {
    companion object {
        fun of(samples: List<Sample>): HistoryStats {
            val readings = samples.mapNotNull { it.temperature }
            return HistoryStats(
                min = readings.minOrNull(),
                max = readings.maxOrNull(),
                mean = readings.takeIf { it.isNotEmpty() }?.average(),
                dutyCycle = samples.takeIf { it.isNotEmpty() }
                    ?.let { all -> all.count { it.relay == RelayState.ON }.toDouble() / all.size },
            )
        }
    }
}
