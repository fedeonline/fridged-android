package io.github.fedeonline.fridged.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.ui.dial.DialScale
import io.github.fedeonline.fridged.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's dial as a bitmap: Glance widgets cannot draw arcs. Same [DialScale] geometry
 * as the Compose dial; a snowflake in the middle while the compressor runs.
 */
object DialBitmap {
    fun render(sizePx: Int, band: Band?, temperature: Double?, compressorOn: Boolean): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.1f
        val inset = stroke / 2 + sizePx * 0.04f
        val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val radius = oval.width() / 2
        val scale = DialScale.around(band, temperature)
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }

        arc.color = Palette.Track.toArgb()
        canvas.drawArc(oval, DialScale.START_ANGLE, DialScale.SWEEP, false, arc)
        band?.let {
            arc.color = Palette.Mint.toArgb()
            val from = scale.angle(it.min)
            canvas.drawArc(oval, from, scale.angle(it.max) - from, false, arc)
        }
        temperature?.let {
            val radians = Math.toRadians(scale.angle(it).toDouble())
            val x = oval.centerX() + radius * cos(radians).toFloat()
            val y = oval.centerY() + radius * sin(radians).toFloat()
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            dot.color = Palette.Background.toArgb()
            canvas.drawCircle(x, y, stroke * 0.95f, dot)
            dot.color = Palette.OnSurface.toArgb()
            canvas.drawCircle(x, y, stroke * 0.68f, dot)
        }
        if (compressorOn) drawSnowflake(canvas, oval.centerX(), oval.centerY(), sizePx * 0.17f)
        return bitmap
    }

    private fun drawSnowflake(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Palette.Blue.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = r * 0.22f
            strokeCap = Paint.Cap.ROUND
        }
        for (degrees in listOf(90.0, 30.0, 150.0)) {
            val a = Math.toRadians(degrees)
            val dx = (r * cos(a)).toFloat()
            val dy = (r * sin(a)).toFloat()
            canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, line)
        }
    }
}
