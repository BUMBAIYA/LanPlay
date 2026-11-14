package com.bumbaiya.lanplay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Headings, numbers, buttons and card glyphs. */
val Baloo = FontFamily(
    variableFont(R.font.baloo2, FontWeight.Medium),
    variableFont(R.font.baloo2, FontWeight.SemiBold),
    variableFont(R.font.baloo2, FontWeight.Bold),
)

/** Older call sites still say SpaceGrotesk; it is Baloo 2 now. */
val SpaceGrotesk = Baloo

/** Body text and labels. */
val Inter = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold),
    variableFont(R.font.inter, FontWeight.Bold),
)

private val Base = Typography()
private fun TextStyle.heading() = copy(fontFamily = Baloo, fontWeight = FontWeight.Bold)

val Typography = Typography(
    displayLarge = Base.displayLarge.heading(),
    displayMedium = Base.displayMedium.heading(),
    displaySmall = Base.displaySmall.heading(),
    headlineLarge = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp),
    headlineSmall = Base.headlineSmall.heading(),
    titleLarge = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp),
    titleSmall = Base.titleSmall.heading(),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp),
)
