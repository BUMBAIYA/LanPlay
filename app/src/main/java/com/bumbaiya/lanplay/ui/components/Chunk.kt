package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.platform.Sfx
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.DisabledFill
import com.bumbaiya.lanplay.ui.theme.DisabledText
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Inter
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.PanelBg
import com.bumbaiya.lanplay.ui.theme.Sand
import com.bumbaiya.lanplay.ui.theme.Sand2
import kotlin.math.roundToInt

/**
 * The one building block of the design: a flat fill, an ink outline and a hard ink shadow underneath.
 * When it can be tapped it plays the press: the face sinks onto its shadow and springs back, with a tick
 * on the way down and a pop on release. The shadow is drawn in the extra [shadow] of height under the face.
 */
@Composable
fun Chunk(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fill: Color = PanelBg,
    radius: Dp = 14.dp,
    border: Dp = 2.5.dp,
    shadow: Dp = 4.dp,
    enabled: Boolean = true,
    borderColor: Color = Ink,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val active = onClick != null && enabled
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val depth by animateFloatAsState(if (pressed && active) 1f else 0f, spring(dampingRatio = 0.42f, stiffness = 1100f), label = "press")
    LaunchedEffect(pressed) { if (pressed && active) Sfx.down() }
    val shape = RoundedCornerShape(radius)
    val px = with(LocalDensity.current) { shadow.toPx() }
    Box(
        modifier
            .padding(bottom = shadow)
            .drawBehind { if (px > 0f) drawRoundRect(Ink, topLeft = Offset(0f, px), size = size, cornerRadius = CornerRadius(radius.toPx())) }
            .graphicsLayer { translationY = depth * px; scaleX = 1f - 0.02f * depth; scaleY = 1f - 0.02f * depth }
            .background(fill, shape)
            .then(if (border > 0.dp) Modifier.border(border, borderColor, shape) else Modifier)
            .then(if (active) Modifier.clickable(interactionSource = source, indication = null) { Sfx.up(); onClick?.invoke() } else Modifier)
            .clip(shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** A solid action button. Takes only the width it needs; give it a width or weight to change that. */
@Composable
fun ChunkButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, fill: Color = Amber, icon: ImageVector? = null,
    enabled: Boolean = true, height: Dp = 46.dp, fontSize: TextUnit = 15.sp, padding: Dp = 22.dp, ink: Color = Ink,
) {
    val fg = if (enabled) ink else DisabledText
    Chunk(
        modifier.height(height + 4.dp), onClick = onClick, fill = if (enabled) fill else DisabledFill, enabled = enabled,
        border = if (enabled) 2.5.dp else 0.dp, shadow = if (enabled) 4.dp else 4.dp, contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = padding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) DIcon(icon, tint = fg, size = 18.dp)
            Text(text, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = fontSize, color = fg, maxLines = 1, softWrap = false)
        }
    }
}

/** A square icon button (back, sound, standings...). */
@Composable
fun SquareButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, fill: Color = Sand, size: Dp = 40.dp, tint: Color = Ink) {
    Chunk(modifier.size(size + 3.dp), onClick = onClick, fill = fill, radius = 12.dp, border = 2.dp, shadow = 3.dp, contentAlignment = Alignment.Center) {
        DIcon(icon, tint = tint, size = 20.dp)
    }
}

/** A flat panel (form, list, card) with the ink outline and hard shadow. */
@Composable
fun Panel(
    modifier: Modifier = Modifier, fill: Color = PanelBg, radius: Dp = 16.dp, padding: Dp = 12.dp,
    contentAlignment: Alignment = Alignment.TopStart, content: @Composable BoxScope.() -> Unit,
) = Chunk(modifier, fill = fill, radius = radius, border = 2.5.dp, shadow = 4.dp, contentAlignment = contentAlignment) {
    Box(Modifier.padding(padding), contentAlignment = contentAlignment, content = content)
}

/** Small rounded label. */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, fill: Color = Sand, fontSize: TextUnit = 11.sp, icon: ImageVector? = null) {
    Row(
        modifier.background(fill, RoundedCornerShape(9.dp)).border(2.dp, Ink, RoundedCornerShape(9.dp)).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) DIcon(icon, tint = Ink, size = 13.dp)
        Text(text, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = fontSize, color = Ink, maxLines = 1, softWrap = false)
    }
}

/** Letter-spaced small caps used for section labels. */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) =
    Text(text.uppercase(), modifier, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp, color = color, maxLines = 1)

@Composable
fun Avatar(letter: String, color: Color, size: Dp = 30.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(color, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
        Text(letter.take(1).uppercase(), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp, color = Ink)
    }
}

/** The row at the top of every screen: back button, title, an optional middle slot, and a trailing slot. */
@Composable
fun Header(
    title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, subtitle: String? = null, lead: @Composable (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) SquareButton(DI.Back, onBack)
        lead?.invoke()
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink, maxLines = 1, softWrap = false, lineHeight = 19.sp)
            if (subtitle != null) Text(subtitle, fontFamily = Inter, fontSize = 11.sp, color = Muted, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** A round tinted disc holding an icon: the header badge on Join, Host, Settings. */
@Composable
fun IconDisc(icon: ImageVector, ring: Color, inner: Color, size: Dp = 44.dp, tint: Color = Ink) {
    Box(Modifier.size(size).background(ring, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 0.78f).background(inner, CircleShape), contentAlignment = Alignment.Center) { DIcon(icon, tint = tint, size = size * 0.46f) }
    }
}

/** The text field: white, ink outline, an optional small label inside. */
@Composable
fun InkField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null, label: String? = null,
    height: Dp = 46.dp, fontSize: TextUnit = 15.sp, maxLength: Int = Int.MAX_VALUE, center: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value, onValueChange = { if (it.length <= maxLength) onChange(it) }, singleLine = true,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        textStyle = TextStyle(color = Ink, fontSize = fontSize, fontFamily = Inter, fontWeight = FontWeight.SemiBold, textAlign = if (center) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start),
        cursorBrush = SolidColor(Ink),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(height).background(Color.White, shape).border(2.5.dp, Ink, shape).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (label != null) Text(label, fontFamily = Inter, fontSize = 10.sp, color = Muted, maxLines = 1)
                Box(Modifier.weight(1f), contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) Text(placeholder, fontFamily = Inter, fontSize = fontSize, color = Muted)
                    inner()
                }
            }
        },
    )
}

/** The on/off switch. */
@Composable
fun ToggleSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val knob by animateDpAsState(if (checked) 24.dp else 2.dp, spring(dampingRatio = 0.5f, stiffness = 700f), label = "knob")
    Chunk(modifier.size(width = 50.dp, height = 31.dp), onClick = { onChange(!checked) }, fill = if (checked) Amber else Sand2, radius = 15.dp, border = 2.5.dp, shadow = 3.dp) {
        Box(Modifier.offset { IntOffset(knob.roundToPx(), 0) }.padding(top = 2.dp).size(21.dp).background(if (checked) Ink else Color.White, CircleShape).border(2.dp, Ink, CircleShape))
    }
}

/** A row of exclusive options, the chosen one pressed out in amber. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: BooleanArray? = null) {
    Row(modifier.background(Sand2, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val ok = enabled?.get(i) ?: true
            if (on) Chunk(Modifier.height(36.dp), fill = Amber, radius = 11.dp, shadow = 3.dp, contentAlignment = Alignment.Center) {
                Text(label, Modifier.padding(horizontal = 14.dp), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink, maxLines = 1)
            } else Box(
                Modifier.height(36.dp).then(if (ok) Modifier.clickable { Sfx.up(); onSelect(i) } else Modifier).padding(horizontal = 14.dp), contentAlignment = Alignment.Center,
            ) { Text(label, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (ok) Muted else Muted.copy(alpha = 0.7f), maxLines = 1) }
        }
    }
}

/** "- 30 s +" tile used by the game settings. */
@Composable
fun StepperCard(label: String, value: String, unit: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Panel(modifier, padding = 10.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Muted, maxLines = 1)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SquareButton(DI.Minus, onMinus, size = 36.dp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Ink, lineHeight = 28.sp)
                    Text(" $unit", fontFamily = Inter, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
                }
                SquareButton(DI.Plus, onPlus, size = 36.dp, fill = com.bumbaiya.lanplay.ui.theme.Teal)
            }
        }
    }
}
