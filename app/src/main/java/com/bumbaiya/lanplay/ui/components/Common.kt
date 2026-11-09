package com.bumbaiya.lanplay.ui.components

import androidx.compose.foundation.layout.Arrangement
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
fun BackLink(onClick: () -> Unit, label: String = "< Back") = TextButton(
    onClick = onClick,
    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.lanPlayColors.accentText),
) { Text(label) }

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
