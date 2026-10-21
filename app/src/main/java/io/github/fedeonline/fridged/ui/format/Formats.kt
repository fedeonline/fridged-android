package io.github.fedeonline.fridged.ui.format

import io.github.fedeonline.fridged.model.Band
import java.util.Locale

/** `4.3 °C` (`4,3 °C` in Italian). The sensor resolution is 0.0625 °C: one decimal is enough. */
fun formatCelsius(value: Double, locale: Locale): String = String.format(locale, "%.1f °C", value)

/** `3.0 – 5.0 °C`. */
fun formatBand(band: Band, locale: Locale): String =
    String.format(locale, "%.1f – %.1f °C", band.min, band.max)
