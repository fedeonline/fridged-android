package io.github.fedeonline.fridged.ui.dial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The thermostat dial: a track, the [band] as a mint arc, and either a [needle] at the
 * current temperature or, with [knobs], the band's ends (blue minimum, peach maximum).
 * [content] is drawn in the middle.
 */
@Composable
fun Dial(
    scale: DialScale,
    band: Band?,
    modifier: Modifier = Modifier,
    needle: Double? = null,
    knobs: Boolean = false,
    stroke: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val diameter = size.minDimension - width
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            fun arc(color: Color, from: Float, sweep: Float) = drawArc(
                color = color,
                startAngle = from,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            fun dot(angle: Float, fill: Color) {
                val radians = Math.toRadians(angle.toDouble())
                val c = Offset(
                    center.x + diameter / 2 * cos(radians).toFloat(),
                    center.y + diameter / 2 * sin(radians).toFloat(),
                )
                drawCircle(Palette.Background, radius = width * 0.95f, center = c)
                drawCircle(fill, radius = width * 0.68f, center = c)
            }

            arc(Palette.Track, DialScale.START_ANGLE, DialScale.SWEEP)
            band?.let {
                val from = scale.angle(it.min)
                arc(Palette.Mint, from, scale.angle(it.max) - from)
                if (knobs) {
                    dot(scale.angle(it.min), Palette.Blue)
                    dot(scale.angle(it.max), Palette.Peach)
                }
            }
            needle?.let { dot(scale.angle(it), Palette.OnSurface) }
        }
        ScaleLabel(scale.low, Alignment.BottomStart)
        ScaleLabel(scale.high, Alignment.BottomEnd)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
    }
}

@Composable
private fun BoxScope.ScaleLabel(value: Double, alignment: Alignment) {
    Text(
        "${value.roundToInt()}°",
        style = MaterialTheme.typography.labelSmall,
        color = Palette.Faint,
        modifier = Modifier.align(alignment).padding(horizontal = 36.dp, vertical = 8.dp),
    )
}

