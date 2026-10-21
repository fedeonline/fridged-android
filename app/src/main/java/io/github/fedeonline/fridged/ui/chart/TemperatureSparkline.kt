package io.github.fedeonline.fridged.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import io.github.fedeonline.fridged.model.Sample
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A small temperature line over [span] up to [now], without axes. Missing readings are
 * skipped (the History chart shows them as gaps). Draws nothing without
 * [hasEnoughReadings].
 */
@Composable
fun TemperatureSparkline(samples: List<Sample>, now: Instant, span: Duration, modifier: Modifier = Modifier) {
    val points = remember(samples, now) {
        samples.mapNotNull { s ->
            s.temperature?.let { t -> Duration.between(now, s.time).toMinutes().toDouble() to t }
        }
    }
    if (points.size < 2) return

    val model = remember(points) {
        CartesianChartModel(LineCartesianLayerModel.build { series(points.map { it.first }, points.map { it.second }) })
    }
    val readings = points.map { it.second }
    val range = remember(points, span) {
        CartesianLayerRangeProvider.fixed(
            minX = -span.toMinutes().toDouble(),
            maxX = 0.0,
            minY = floor(readings.min() - 0.5),
            maxY = ceil(readings.max() + 0.5),
        )
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer(rangeProvider = range)),
        model = model,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** A line needs at least two readings. */
fun hasEnoughReadings(samples: List<Sample>): Boolean = samples.count { it.temperature != null } >= 2
