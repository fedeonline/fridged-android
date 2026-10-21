package io.github.fedeonline.fridged.ui.dial

import io.github.fedeonline.fridged.model.Band
import kotlin.math.floor

/**
 * The dial's temperature scale: a 270° arc open at the bottom, [SPAN] °C wide, centred on
 * the band so the band always sits in the upper part of the arc.
 */
data class DialScale(val low: Double, val high: Double) {

    /** Where [celsius] falls on the scale, 0..1; values off the scale stick to its ends. */
    fun fraction(celsius: Double): Float = ((celsius - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()

    /** Angle in degrees, clockwise from 3 o'clock, as Canvas arcs expect. */
    fun angle(celsius: Double): Float = START_ANGLE + fraction(celsius) * SWEEP

    companion object {
        const val START_ANGLE = 135f
        const val SWEEP = 270f
        const val SPAN = 10.0

        fun around(band: Band?, temperature: Double?): DialScale {
            val centre = band?.let { (it.min + it.max) / 2 } ?: temperature ?: 5.0
            val low = (floor(centre) - SPAN / 2).coerceIn(Band.LOWEST, Band.HIGHEST - SPAN)
            return DialScale(low, low + SPAN)
        }
    }
}
