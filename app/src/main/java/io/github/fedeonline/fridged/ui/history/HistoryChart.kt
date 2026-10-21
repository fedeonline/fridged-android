package io.github.fedeonline.fridged.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import io.github.fedeonline.fridged.model.Sample
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Temperature over [range] up to [end]: the curve with gaps, the target band shaded, the
 * compressor-on periods as a strip along the bottom, SAFE periods highlighted. Needs at
 * least one reading in [samples].
 */
@Composable
fun HistoryChart(samples: List<Sample>, end: Instant, range: HistoryRange, modifier: Modifier = Modifier) {
    val zone = remember(end) { ZoneId.systemDefault().rules.getOffset(end) }
    val data = remember(samples, end, range) { ChartData.of(samples, end, range.span, zone) }
    if (data.runs.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    val chartColors = historyChartColors()
    val stripHeight = with(LocalDensity.current) { 6.dp.toPx() }
    val decoration = remember(data, chartColors, stripHeight) {
        HistoryDecoration(data.segments, chartColors, stripHeight)
    }
    val model = remember(data) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                data.runs.forEach { run -> series(run.map { it.first }, run.map { it.second }) }
            },
        )
    }
    val locale = LocalConfiguration.current.locales[0]
    val labelFormat = remember(range, locale) { DateTimeFormatter.ofPattern(range.labelPattern, locale) }
    // Whole hours or days from the first x value, which is not aligned itself.
    val offset = remember(data, range) {
        val spacing = range.labelEveryMinutes
        ((spacing - data.minX.toLong().mod(spacing)) % spacing).toInt()
    }

    val line = LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(colors.primary)))
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(line),
                rangeProvider = remember(data) {
                    CartesianLayerRangeProvider.fixed(data.minX, data.maxX, data.minY, data.maxY)
                },
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = remember(locale) {
                    CartesianValueFormatter { _, y, _ -> String.format(locale, "%.0f°", y) }
                },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = remember(labelFormat) {
                    CartesianValueFormatter { _, x, _ ->
                        LocalDateTime.ofEpochSecond(x.toLong() * 60, 0, ZoneOffset.UTC).format(labelFormat)
                    }
                },
                itemPlacer = remember(range, offset) {
                    HorizontalAxis.ItemPlacer.aligned(
                        spacing = { range.labelEveryMinutes.toInt() },
                        offset = { offset },
                        addExtremeLabelPadding = true,
                    )
                },
            ),
            decorations = listOf(decoration),
            getXStep = { _, _, _ -> 1.0 },
        ),
        model = model,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** Colors of the chart background marks, shared with the legend. */
data class HistoryChartColors(val band: Color, val compressor: Color, val safe: Color)

@Composable
fun historyChartColors(): HistoryChartColors {
    val colors = MaterialTheme.colorScheme
    return HistoryChartColors(
        band = colors.primary.copy(alpha = 0.2f),
        compressor = colors.tertiary,
        safe = colors.error.copy(alpha = 0.15f),
    )
}

/** Draws the band, SAFE periods and compressor strip under the curve. */
private class HistoryDecoration(
    private val segments: List<Segment>,
    colors: HistoryChartColors,
    private val stripHeight: Float,
) : Decoration {
    private val bandPaint = Paint().apply { color = colors.band }
    private val compressorPaint = Paint().apply { color = colors.compressor }
    private val safePaint = Paint().apply { color = colors.safe }

    override fun drawUnderLayers(context: CartesianDrawingContext) {
        val bounds = context.layerBounds
        val ranges = context.ranges
        val yRange = ranges.getYRange(null)
        val dimensions = context.layerDimensions
        fun xPx(x: Double) = (
            bounds.left + dimensions.startPadding +
                ((x - ranges.minX) / ranges.xStep).toFloat() * dimensions.xSpacing - context.scroll
            ).coerceIn(bounds.left, bounds.right)
        fun yPx(y: Double) =
            (bounds.bottom - ((y - yRange.minY) / yRange.length).toFloat() * bounds.height).coerceIn(bounds.top, bounds.bottom)

        val canvas = context.canvas
        for (s in segments) {
            val left = xPx(s.startX)
            val right = xPx(s.endX)
            if (right <= left) continue
            if (s.safe) canvas.drawRect(left, bounds.top, right, bounds.bottom, safePaint)
            s.band?.let { canvas.drawRect(left, yPx(it.max), right, yPx(it.min), bandPaint) }
            if (s.relayOn) canvas.drawRect(left, bounds.bottom - stripHeight, right, bounds.bottom, compressorPaint)
        }
    }
}
