package io.github.fedeonline.fridged.ui.history

import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

/** How a bar is colored: where its mean sits against the band, or SAFE mode in the bucket. */
enum class BarTone { IN_BAND, BELOW, ABOVE, SAFE }

/** One bucket of the history chart. */
data class Bar(
    val start: Instant,
    /** °C; null without readings in the bucket. */
    val mean: Double?,
    /** Share of samples with the compressor on, 0..1; null without samples. */
    val dutyCycle: Double?,
    /** Null for an empty bucket. */
    val tone: BarTone?,
)

/**
 * The History chart as bars: [range] split into buckets aligned to the local clock, the last
 * one holding [end]. Each bar is the mean temperature of its bucket, with the compressor's
 * duty cycle drawn underneath.
 */
data class HistoryBars(
    val bars: List<Bar>,
    val bucket: Duration,
    /** The band of the latest sample, shaded behind the bars. */
    val band: Band?,
    val minY: Double,
    val maxY: Double,
) {
    companion object {
        fun bucketFor(range: HistoryRange): Duration = when (range) {
            HistoryRange.SIX_HOURS -> Duration.ofMinutes(15)
            HistoryRange.DAY -> Duration.ofHours(1)
            HistoryRange.WEEK -> Duration.ofHours(6)
        }

        fun of(samples: List<Sample>, end: Instant, range: HistoryRange, zone: ZoneId): HistoryBars {
            val bucket = bucketFor(range)
            val count = (range.span.seconds / bucket.seconds).toInt()
            val lastStart = floorToBucket(end, bucket, zone)
            val firstStart = lastStart.minus(bucket.multipliedBy((count - 1).toLong()))

            val groups = samples
                .filter { it.time >= firstStart && it.time <= end }
                .groupBy { (Duration.between(firstStart, it.time).seconds / bucket.seconds).toInt() }
            val band = samples.lastOrNull { it.band != null }?.band

            val bars = (0 until count).map { i ->
                val inBucket = groups[i].orEmpty()
                val readings = inBucket.mapNotNull { it.temperature }
                val mean = readings.takeIf { it.isNotEmpty() }?.average()
                val bucketBand = inBucket.lastOrNull { it.band != null }?.band ?: band
                Bar(
                    start = firstStart.plus(bucket.multipliedBy(i.toLong())),
                    mean = mean,
                    dutyCycle = inBucket.takeIf { it.isNotEmpty() }
                        ?.let { all -> all.count { it.relay == RelayState.ON }.toDouble() / all.size },
                    tone = when {
                        inBucket.any { it.mode == Mode.SAFE } -> BarTone.SAFE
                        mean == null -> null
                        bucketBand == null -> BarTone.IN_BAND
                        mean < bucketBand.min -> BarTone.BELOW
                        mean > bucketBand.max -> BarTone.ABOVE
                        else -> BarTone.IN_BAND
                    },
                )
            }

            val values = bars.mapNotNull { it.mean } + listOfNotNull(band?.min, band?.max)
            val low = values.minOrNull()?.let { floor(it - 0.5) } ?: 0.0
            val high = values.maxOrNull()?.let { ceil(it + 0.5) } ?: 10.0
            return HistoryBars(bars, bucket, band, minY = low, maxY = maxOf(high, low + 2))
        }

        /** Start of the bucket holding [time], aligned to whole hours or days of local time. */
        fun floorToBucket(time: Instant, bucket: Duration, zone: ZoneId): Instant {
            val offset = zone.rules.getOffset(time).totalSeconds
            val local = time.epochSecond + offset
            return Instant.ofEpochSecond(Math.floorDiv(local, bucket.seconds) * bucket.seconds - offset)
        }
    }
}
