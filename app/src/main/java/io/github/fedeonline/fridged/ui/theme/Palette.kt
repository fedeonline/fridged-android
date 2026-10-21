package io.github.fedeonline.fridged.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fixed dark-and-pastel palette (design direction A, "Quadrante"). Every pastel has a dark
 * "on" color for text on top of it; pastel text on [Background] or [Surface] passes 4.5:1.
 */
object Palette {
    val Background = Color(0xFF15171C)
    val Surface = Color(0xFF20232A)
    val SurfaceHigh = Color(0xFF2B2F39)
    val Track = Color(0xFF262A33)
    val Outline = Color(0xFF3A3F4B)
    val OnSurface = Color(0xFFECEEF2)
    val Muted = Color(0xFF9AA0AC)
    val Faint = Color(0xFF6B7180)

    /** The band, "all good", the selected tab, the main action. */
    val Mint = Color(0xFFA8E6CF)
    val OnMint = Color(0xFF133528)

    /** The compressor, the minimum. */
    val Blue = Color(0xFFA0C4FF)
    val OnBlue = Color(0xFF14243F)

    /** The maximum; stale data. */
    val Peach = Color(0xFFFFD6BA)
    val OnPeach = Color(0xFF4A2410)

    /** Temperature curves, secondary actions; SAFE mode. */
    val Lilac = Color(0xFFCDB4DB)
    val OnLilac = Color(0xFF34213F)

    /** Faults and errors. */
    val Red = Color(0xFFFFB3B3)
    val OnRed = Color(0xFF4A1414)
}
