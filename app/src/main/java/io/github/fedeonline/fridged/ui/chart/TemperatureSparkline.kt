package io.github.fedeonline.fridged.ui.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import io.github.fedeonline.fridged.model.Sample
import io.github.fedeonline.fridged.ui.history.ChartData
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * A small temperature line over [span] up to [now], without axes. Like the History chart,
 * the line breaks at missing readings and at holes in the samples. Draws nothing without
 * [hasEnoughReadings].
 */
@Composable
fun TemperatureSparkline(samples: List<Sample>, now: Instant, span: Duration, modifier: Modifier = Modifier) {
    // No axis labels here, so wall-clock alignment does not matter.
    val data = remember(samples, now, span) { ChartData.of(samples, now, span, ZoneOffset.UTC) }
    if (data.runs.isEmpty()) return

    val model = remember(data) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                data.runs.forEach { run -> series(run.map { it.first }, run.map { it.second }) }
            },
        )
    }
    // One style for every run: by default each series would get its own color.
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(MaterialTheme.colorScheme.primary)),
    )
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(line),
                rangeProvider = remember(data) {
                    CartesianLayerRangeProvider.fixed(data.minX, data.maxX, data.minY, data.maxY)
                },
            ),
            getXStep = { _, _, _ -> 1.0 },
        ),
        model = model,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** A line needs at least two readings. */
fun hasEnoughReadings(samples: List<Sample>): Boolean = samples.count { it.temperature != null } >= 2
