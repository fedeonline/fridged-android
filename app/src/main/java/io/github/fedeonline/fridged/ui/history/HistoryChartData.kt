package io.github.fedeonline.fridged.ui.history

import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.STALE_AFTER
import io.github.fedeonline.fridged.model.Sample
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/** Chart x values are local wall-clock minutes, so axis labels fall on whole hours and days. */
fun chartX(time: Instant, zone: ZoneOffset): Double = ((time.epochSecond + zone.totalSeconds) / 60).toDouble()

/** A time span on the chart where the band, the compressor and the mode do not change. */
data class Segment(val startX: Double, val endX: Double, val band: Band?, val relayOn: Boolean, val safe: Boolean)

data class ChartData(
    /** Continuous stretches of readings, each one a line series of (x, °C). */
    val runs: List<List<Pair<Double, Double>>>,
    val segments: List<Segment>,
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
) {
    companion object {
        /**
         * Chart data for [samples] (oldest first) over [span] up to [end]. A sample stands
         * for the time until the next one; a hole longer than [STALE_AFTER] (daemon offline)
         * breaks the line and leaves the background empty.
         */
        fun of(samples: List<Sample>, end: Instant, span: Duration, zone: ZoneOffset): ChartData {
            val maxGap = STALE_AFTER.toMinutes().toDouble()
            val xs = samples.map { chartX(it.time, zone) }
            val maxX = chartX(end, zone)
            val minX = chartX(end.minus(span), zone)

            val runs = mutableListOf<MutableList<Pair<Double, Double>>>()
            var previousX: Double? = null
            samples.forEachIndexed { i, s ->
                val x = xs[i]
                val t = s.temperature
                val gap = previousX?.let { x - it }
                val continues = t != null && gap != null && gap <= maxGap && samples[i - 1].temperature != null
                if (t != null) {
                    if (continues) runs.last().add(x to t) else runs.add(mutableListOf(x to t))
                }
                previousX = x
            }

            val segments = mutableListOf<Segment>()
            samples.forEachIndexed { i, s ->
                val start = xs[i]
                val next = xs.getOrNull(i + 1) ?: maxX
                val stop = minOf(next, start + maxGap, maxX)
                if (stop <= start) return@forEachIndexed
                val segment = Segment(start, stop, s.band, s.relay == RelayState.ON, s.mode == Mode.SAFE)
                val last = segments.lastOrNull()
                if (last != null && last.endX == start && last.band == segment.band &&
                    last.relayOn == segment.relayOn && last.safe == segment.safe
                ) {
                    segments[segments.lastIndex] = last.copy(endX = stop)
                } else {
                    segments.add(segment)
                }
            }

            val values = samples.mapNotNull { it.temperature } +
                samples.mapNotNull { it.band }.flatMap { listOf(it.min, it.max) }
            return ChartData(
                runs = runs,
                segments = segments,
                minX = minX,
                maxX = maxX,
                minY = values.minOrNull()?.let { kotlin.math.floor(it - 0.5) } ?: 0.0,
                maxY = values.maxOrNull()?.let { kotlin.math.ceil(it + 0.5) } ?: 10.0,
            )
        }
    }
}
