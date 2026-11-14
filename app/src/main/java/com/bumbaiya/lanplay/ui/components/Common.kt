package com.bumbaiya.lanplay.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The frame every screen sits in. On a phone it fills the width; on a tablet or a phone turned
 * sideways the content stays a comfortable 600dp wide and centred. It scrolls, so small screens
 * and big text sizes never cut anything off.
 */
/** The system-bar / camera cut-out insets. Screens draw their background edge to edge and pad their content by this. */
val LocalSafePadding = androidx.compose.runtime.compositionLocalOf { androidx.compose.foundation.layout.PaddingValues(0.dp) }

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, gap: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(LocalSafePadding.current), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }
}

@Composable
fun SectionLabel(text: String) = Text(
    text.uppercase(),
    fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing = 1.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** A label on the left and its control on the right. Every setting in the app uses this. */
@Composable
fun SettingRow(label: String, modifier: Modifier = Modifier, control: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
        control()
    }
}

@Composable
fun CheckRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) =
    SettingRow(label) { Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled) }

/** "-  30 s  +" control inside a bordered row, as in the lobby design. */
@Composable
fun StepperRow(label: String, value: String, enabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) =
    Panel(Modifier) {
        Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, modifier = Modifier.weight(1f))
            if (enabled) StepButton("−", onMinus)
            Text(value, fontFamily = com.bumbaiya.lanplay.ui.theme.SpaceGrotesk, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.widthIn(min = 52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (enabled) StepButton("+", onPlus)
        }
    }

@Composable
private fun StepButton(sign: String, onClick: () -> Unit) = androidx.compose.material3.Surface(
    onClick = onClick, modifier = Modifier.size(44.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
) { Box(contentAlignment = Alignment.Center) { Text(sign, fontSize = 20.sp) } }

/** Design line icons keyed by the icon name the call sites already use. */
private fun gameIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector = when (name) {
    "Filled.Star" -> DI.Tour
    "Filled.Info" -> DI.Book
    "Filled.Home" -> DI.Host
    "Filled.Person" -> DI.Join
    "Filled.PlayArrow" -> DI.Play
    "Filled.Add" -> DI.DrawCard
    "Filled.Check" -> DI.Check
    "Filled.Close" -> DI.Close
    "Filled.Refresh" -> DI.Spinner
    "Filled.Search" -> DI.Scan
    "Filled.Warning" -> DI.Torch
    "Filled.Settings" -> DI.Sliders
    "AutoMirrored.Filled.ArrowBack" -> DI.Back
    "AutoMirrored.Filled.ArrowForward" -> DI.Right
    "AutoMirrored.Filled.ExitToApp" -> DI.Leave
    else -> DI.Right
}

/** A button label with one of the design's line icons in front. */
@androidx.compose.runtime.Composable
fun IconLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.Icon(gameIcon(icon.name), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = style, color = color)
    }
}
