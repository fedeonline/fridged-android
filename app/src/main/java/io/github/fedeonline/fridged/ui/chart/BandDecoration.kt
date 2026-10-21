package io.github.fedeonline.fridged.ui.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import io.github.fedeonline.fridged.ui.history.Segment

/** Shades the band recorded in each segment under a Vico line chart. */
internal class BandDecoration(private val segments: List<Segment>, color: Color) : Decoration {
    private val paint = Paint().apply { this.color = color }

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

        for (s in segments) {
            val left = xPx(s.startX)
            val right = xPx(s.endX)
            if (right <= left) continue
            s.band?.let { context.canvas.drawRect(left, yPx(it.max), right, yPx(it.min), paint) }
        }
    }
}
