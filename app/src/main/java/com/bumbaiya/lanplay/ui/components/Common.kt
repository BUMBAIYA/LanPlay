package com.bumbaiya.lanplay.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

/**
 * The frame every screen sits in. On a phone it fills the width; on a tablet or a phone turned
 * sideways the content stays a comfortable 600dp wide and centred. It scrolls, so small screens
 * and big text sizes never cut anything off.
 */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
fun SectionLabel(text: String) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun BackLink(onClick: () -> Unit, label: String = "Back") = TextButton(
    onClick = onClick,
    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.lanPlayColors.accentText),
) { IconLabel(Icons.AutoMirrored.Filled.ArrowBack, label) }

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

/** "-  30s  +" control. */
@Composable
fun StepperRow(label: String, value: String, enabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) =
    SettingRow(label) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onMinus, enabled = enabled, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.widthIn(min = 40.dp)) { Text("−") }
            Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 56.dp).padding(horizontal = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            OutlinedButton(onClick = onPlus, enabled = enabled, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.widthIn(min = 40.dp)) { Text("+") }
        }
    }

/** Solid, rounded icons with a game feel, keyed by the icon name the call sites already use. */
private fun gameIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val r = androidx.compose.material.icons.Icons.Rounded
    val m = androidx.compose.material.icons.Icons.AutoMirrored.Rounded
    return when (name) {
        "Filled.Star" -> r.SportsEsports
        "Filled.Info" -> r.MenuBook
        "Filled.Home" -> r.Casino
        "Filled.Person" -> r.Groups
        "Filled.PlayArrow" -> r.PlayArrow
        "Filled.Add" -> r.Style
        "Filled.Check" -> r.Check
        "Filled.Close" -> r.Close
        "Filled.Refresh" -> r.Casino
        "Filled.Search" -> r.QrCodeScanner
        "Filled.Warning" -> r.Bolt
        "Filled.Settings" -> r.Settings
        "AutoMirrored.Filled.ArrowBack" -> m.ArrowBack
        "AutoMirrored.Filled.ArrowForward" -> m.ArrowForward
        "AutoMirrored.Filled.ExitToApp" -> m.ExitToApp
        else -> r.Star
    }
}

/** A button label with a solid game-style icon in front, so buttons read at a glance. */
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
