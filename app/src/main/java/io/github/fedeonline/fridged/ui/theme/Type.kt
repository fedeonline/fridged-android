package io.github.fedeonline.fridged.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fedeonline.fridged.R

/** Manrope, variable font (SIL OFL, licence in assets/licenses). */
@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(R.font.manrope, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    },
)

private val base = Typography()

private fun TextStyle.manrope() = copy(fontFamily = Manrope)

val FridgedTypography = Typography(
    displayLarge = base.displayLarge.manrope().copy(fontSize = 76.sp, lineHeight = 76.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em),
    displayMedium = base.displayMedium.manrope().copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
    displaySmall = base.displaySmall.manrope().copy(fontWeight = FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.manrope().copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.manrope().copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.manrope().copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.manrope().copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.manrope().copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.manrope().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.manrope(),
    bodyMedium = base.bodyMedium.manrope(),
    bodySmall = base.bodySmall.manrope(),
    labelLarge = base.labelLarge.manrope().copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.manrope(),
    labelSmall = base.labelSmall.manrope(),
)
