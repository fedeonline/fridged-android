package io.github.fedeonline.fridged.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.fedeonline.fridged.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/** Bar color for each tone, shared with the legend. */
fun BarTone.color(): Color = when (this) {
    BarTone.IN_BAND -> Palette.Mint
    BarTone.BELOW -> Palette.Blue
    BarTone.ABOVE -> Palette.Peach
    BarTone.SAFE -> Palette.Lilac
}

/**
 * Mean temperature per bucket as rounded bars over the shaded band, and the compressor's
 * duty cycle per bucket as a row of blue columns underneath (design "barre con compressore").
 * [compact] is the Status preview: shorter, without axis labels.
 */
@Composable
fun BarChart(
    chart: HistoryBars,
    range: HistoryRange,
    description: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = Palette.Faint)
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember { ZoneId.systemDefault() }
    val format = remember(range, locale) { DateTimeFormatter.ofPattern(range.labelPattern, locale).withZone(zone) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(if (compact) 88.dp else 236.dp)
            .semantics { contentDescription = description },
    ) {
        val axis = if (compact) 0f else 28.dp.toPx()
        val top = 8.dp.toPx()
        val plotHeight = (if (compact) 62.dp else 160.dp).toPx()
        val tempBottom = top + plotHeight
        val dutyTop = tempBottom + (if (compact) 6.dp else 12.dp).toPx()
        val dutyHeight = (if (compact) 10.dp else 28.dp).toPx()
        val labelTop = dutyTop + dutyHeight + 10.dp.toPx()

        val plotWidth = size.width - axis
        val slot = plotWidth / chart.bars.size
        val barWidth = slot * 0.66f
        fun y(celsius: Double) = (tempBottom - (celsius - chart.minY) / (chart.maxY - chart.minY) * plotHeight).toFloat()
        fun barLeft(i: Int) = axis + i * slot + (slot - barWidth) / 2

        // Band as a step line: each bucket shades the band in force then, and the dashed edges
        // follow, stepping where the band changed.
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
        val edge = Palette.Mint.copy(alpha = 0.35f)
        chart.bars.forEachIndexed { i, bar ->
            val band = bar.band ?: return@forEachIndexed
            val from = axis + i * slot
            val to = from + slot
            val bandTop = y(band.max).coerceIn(top, tempBottom)
            val bandBottom = y(band.min).coerceIn(top, tempBottom)
            drawRect(Palette.Mint.copy(alpha = 0.07f), Offset(from, bandTop), Size(slot, bandBottom - bandTop))
            for (level in listOf(bandTop, bandBottom)) {
                drawLine(edge, Offset(from, level), Offset(to, level), 1.dp.toPx(), pathEffect = dash)
            }
            val previous = chart.bars.getOrNull(i - 1)?.band
            if (previous != null && previous != band) {
                for ((a, b) in listOf(y(previous.max) to bandTop, y(previous.min) to bandBottom)) {
                    if (a != b) drawLine(edge, Offset(from, a.coerceIn(top, tempBottom)), Offset(from, b), 1.dp.toPx())
                }
            }
        }

        // The bars stand on minY: a baseline marks it.
        drawLine(Palette.Outline, Offset(axis, tempBottom), Offset(size.width, tempBottom), 1.dp.toPx())

        // Y labels on whole degrees from the base up (every 2 when the range is wide), so the
        // height of a bar can be read against them.
        val step = if (chart.maxY - chart.minY > 6) 2 else 1
        var degree = ceil(chart.minY).toInt()
        while (!compact && degree <= chart.maxY) {
            val label = measurer.measure("$degree°", labelStyle)
            drawText(label, topLeft = Offset(axis - label.size.width - 6.dp.toPx(), y(degree.toDouble()) - label.size.height / 2))
            degree += step
        }

        val radius = CornerRadius(barWidth / 3, barWidth / 3)
        // A label that would touch the previous one is skipped (e.g. a midnight right after the start).
        var labelsEnd = Float.NEGATIVE_INFINITY
        chart.bars.forEachIndexed { i, bar ->
            val left = barLeft(i)
            // Temperature bar, from the bottom of the plot to the bucket's mean.
            val tone = bar.tone
            if (bar.mean != null && tone != null) {
                val barTop = y(bar.mean).coerceIn(top, tempBottom - 2.dp.toPx())
                drawRoundRect(tone.color(), Offset(left, barTop), Size(barWidth, tempBottom - barTop), radius)
            } else if (tone == BarTone.SAFE) {
                drawRoundRect(tone.color(), Offset(left, tempBottom - 6.dp.toPx()), Size(barWidth, 6.dp.toPx()), radius)
            }
            // Compressor column: its height is the share of the bucket with the compressor on.
            val duty = bar.dutyCycle
            if (duty != null && duty > 0) {
                val h = (duty * dutyHeight).toFloat().coerceAtLeast(2.dp.toPx())
                drawRoundRect(Palette.Blue, Offset(left, dutyTop + dutyHeight - h), Size(barWidth, h), CornerRadius(2.dp.toPx()))
            } else {
                drawRoundRect(Palette.SurfaceHigh, Offset(left, dutyTop + dutyHeight - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()), CornerRadius(1.dp.toPx()))
            }
            // Time labels on whole hours or days, centred under the bar they name, with a tick.
            if (!compact && range.isLabelled(bar.start, zone)) {
                val label = measurer.measure(format.format(bar.start), labelStyle)
                val centre = left + barWidth / 2
                val x = (centre - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
                if (x >= labelsEnd + 6.dp.toPx()) {
                    drawLine(Palette.Faint, Offset(centre, labelTop - 5.dp.toPx()), Offset(centre, labelTop - 1.dp.toPx()), 1.dp.toPx())
                    drawText(label, topLeft = Offset(x, labelTop))
                    labelsEnd = x + label.size.width
                }
            }
        }

        // A small snowflake marks the compressor row.
        if (compact) return@Canvas
        val c = Offset(axis - 13.dp.toPx(), dutyTop + dutyHeight / 2)
        val r = 6.dp.toPx()
        for (degrees in listOf(90.0, 30.0, 150.0)) {
            val a = Math.toRadians(degrees)
            val d = Offset((r * cos(a)).toFloat(), (r * sin(a)).toFloat())
            drawLine(Palette.Blue, c - d, c + d, 1.5.dp.toPx(), StrokeCap.Round)
        }
    }
}

/** Which bucket starts get a time label: every hour (6 h), every 4 hours (24 h), every midnight (7 days). */
private fun HistoryRange.isLabelled(start: Instant, zone: ZoneId): Boolean {
    val local = start.atZone(zone)
    return when (this) {
        HistoryRange.SIX_HOURS -> local.minute == 0
        HistoryRange.DAY -> local.minute == 0 && local.hour % 4 == 0
        HistoryRange.WEEK -> local.hour == 0
    }
}
