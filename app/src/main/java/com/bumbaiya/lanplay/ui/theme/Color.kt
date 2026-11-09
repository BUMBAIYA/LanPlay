package com.bumbaiya.lanplay.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Brand lime. Used for filled buttons and highlights, always with dark text on top. */
val Lime = Color(0xFFB6F24A)

/**
 * Colors that Material3's ColorScheme has no slot for. Values come from the LanPlay design
 * (dark and light boards). Read them with [MaterialTheme.lanPlayColors].
 */
@Immutable
data class LanPlayColors(
    /** Accent used for text, rings and borders. Lime fails contrast on white, so light uses a deeper green. */
    val accentText: Color,
    val cardFace: Color,
    val cardBorder: Color,
    val cardNumber: Color,
    val cardOperator: Color,
    /** Blue and red used for labels and icons outside the cards, tuned for the screen background. */
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

val DarkLanPlayColors = LanPlayColors(
    accentText = Lime,
    cardFace = Color(0xFF05070A),
    cardBorder = Color(0xFF2C3440),
    cardNumber = Color(0xFF5BA3E6),
    cardOperator = Color(0xFFF26B5B),
    numberLabel = Color(0xFF5BA3E6),
    operatorLabel = Color(0xFFF26B5B),
    successContainer = Color(0xFF1E3A24),
    onSuccessContainer = Color(0xFF8EE5A0),
    dangerContainer = Color(0xFF3A1F1C),
    onDangerContainer = Color(0xFFF5A196),
    danger = Color(0xFFF26B5B),
    onDanger = Color(0xFF0E1116),
    disabledContainer = Color(0xFF2A2F3A),
    onDisabledContainer = Color(0xFF9AA3B2),
)

val LightLanPlayColors = LanPlayColors(
    accentText = Color(0xFF4C7A00),
    cardFace = Color(0xFFEEF2F8),
    cardBorder = Color(0xFFB4BDCB),
    cardNumber = Color(0xFF1F6FBF),
    cardOperator = Color(0xFFC2402F),
    numberLabel = Color(0xFF1F6FBF),
    operatorLabel = Color(0xFFC2402F),
    successContainer = Color(0xFFDDF3E1),
    onSuccessContainer = Color(0xFF1B6B2F),
    dangerContainer = Color(0xFFFDECEA),
    onDangerContainer = Color(0xFFA32B1D),
    danger = Color(0xFFD93F2C),
    onDanger = Color(0xFFFFFFFF),
    disabledContainer = Color(0xFFE3E7ED),
    onDisabledContainer = Color(0xFF566071),
)
