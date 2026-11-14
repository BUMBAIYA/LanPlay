package com.bumbaiya.lanplay.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The LanPlay palette (from the Tandem design). Everything is flat: a fill, a 2-2.5dp [Ink] outline and a
 * hard [Ink] shadow. There are no gradients and no transparent buttons.
 */
val Ink = Color(0xFF241C18)
val Cream = Color(0xFFF6EEE0)
val PanelBg = Color(0xFFFDF7EC)
val Sand = Color(0xFFF4E4CB)
val Sand2 = Color(0xFFEBDCC2)
val Muted = Color(0xFF7A6E63)
val Teal = Color(0xFF3FA89A)
val TealDark = Color(0xFF1F6A61)
val Rose = Color(0xFFE2607A)
val Amber = Color(0xFFE8A33D)
val Terra = Color(0xFFB4552F)
val AmberSoft = Color(0xFFFBE3B0)
val DisabledFill = Color(0xFFEBDCC2)
val DisabledText = Color(0xFF8C8175)

/** Colours Material3's ColorScheme has no slot for. Light only for now; dark mode comes later. */
@Immutable
data class LanPlayColors(
    val accentText: Color,
    val cardFace: Color,
    val cardBorder: Color,
    val cardNumber: Color,
    val cardOperator: Color,
    val numberLabel: Color,
    val operatorLabel: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val disabledContainer: Color,
    val onDisabledContainer: Color,
)

val LightLanPlayColors = LanPlayColors(
    accentText = TealDark,
    cardFace = Color.White,
    cardBorder = Ink,
    cardNumber = TealDark,
    cardOperator = Terra,
    numberLabel = TealDark,
    operatorLabel = Terra,
    successContainer = Teal,
    onSuccessContainer = Ink,
    dangerContainer = Rose,
    onDangerContainer = Ink,
    danger = Rose,
    onDanger = Ink,
    disabledContainer = DisabledFill,
    onDisabledContainer = DisabledText,
)

val DarkLanPlayColors = LightLanPlayColors
