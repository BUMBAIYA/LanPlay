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

/** Headings, numbers and card glyphs. */
val Baloo = FontFamily(
    variableFont(R.font.baloo2, FontWeight.Medium),
    variableFont(R.font.baloo2, FontWeight.Bold),
)

/** Body text and labels. */
val Nunito = FontFamily(
    variableFont(R.font.nunito, FontWeight.Normal),
    variableFont(R.font.nunito, FontWeight.Medium),
    variableFont(R.font.nunito, FontWeight.SemiBold),
)

/**
 * Starts from Material's full set of text styles and then swaps in our two fonts, so no text
 * anywhere (dialog titles, the big room ID, text fields) falls back to the Android system font.
 * Headings and numbers use Baloo 2, everything you read uses Nunito, 
 */
private val Base = Typography()
private fun androidx.compose.ui.text.TextStyle.heading() = copy(fontFamily = Baloo, fontWeight = FontWeight.Bold)
private fun androidx.compose.ui.text.TextStyle.text() = copy(fontFamily = Nunito)

val Typography = Typography(
    displayLarge = Base.displayLarge.heading(),
    displayMedium = Base.displayMedium.heading(),
    displaySmall = Base.displaySmall.heading(),
    headlineLarge = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = Base.headlineSmall.heading(),
    titleLarge = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = Base.titleSmall.heading(),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)
