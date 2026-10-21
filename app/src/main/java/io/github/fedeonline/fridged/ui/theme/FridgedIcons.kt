package io.github.fedeonline.fridged.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons of the design (24×24, 2 px lines); tinted by `Icon`. */
object FridgedIcons {
    private fun icon(name: String, vararg paths: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    val Thermometer = icon("Thermometer", "M14 14.8V4a2 2 0 1 0-4 0v10.8a4 4 0 1 0 4 0z")
    val Chart = icon("Chart", "M3 3v18h18", "M7 15l4-5 3 3 5-6")
    val Sliders = icon(
        "Sliders",
        "M4 8h16M4 16h16",
        "M7 8a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
        "M13 16a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
    )
    val Snowflake = icon("Snowflake", "M12 2v20M4.9 6l14.2 12M19.1 6L4.9 18", "M9 4l3 2 3-2M9 20l3-2 3 2")
    val Check = icon("Check", "M20 6L9 17l-5-5")
    val More = icon("More", "M12 5h.01M12 12h.01M12 19h.01")
}
