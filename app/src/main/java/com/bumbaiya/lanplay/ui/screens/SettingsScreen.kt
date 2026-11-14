package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.TextBox
import com.bumbaiya.lanplay.ui.components.ToggleRow
import com.bumbaiya.lanplay.ui.theme.ThemeMode

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = Session.settings
    val name by settings.name.collectAsState()
    val theme by settings.theme.collectAsState()
    val sound by settings.sound.collectAsState()
    val haptics by settings.haptics.collectAsState()
    val portrait by settings.portrait.collectAsState()
    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600
    ScreenColumn(gap = 24.dp) {
        Header("Settings", onBack)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Profile")
            Text("Username", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextBox(name, { settings.setName(it.take(16)) })
            Text("Filled in for every room you host or join. You can still change it for a single room when joining.", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Appearance")
            Panel(Modifier) {
                Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Theme", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System).forEach { m ->
                            val on = theme == m
                            Surface(
                                onClick = { settings.setTheme(m) }, shape = RoundedCornerShape(9.dp),
                                color = if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.heightIn(min = 44.dp),
                            ) { Row(Modifier.padding(horizontal = 14.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) { Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) } }
                        }
                    }
                }
            }
            if (isTablet) Panel(Modifier) {
                Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Screen", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(false to "Landscape", true to "Portrait").forEach { (isPortrait, label) ->
                            val on = portrait == isPortrait
                            Surface(
                                onClick = { settings.setPortrait(isPortrait) }, shape = RoundedCornerShape(9.dp),
                                color = if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.heightIn(min = 44.dp),
                            ) { Row(Modifier.padding(horizontal = 14.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) } }
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Sound and feedback · this device only")
            ToggleRow("Sound effects", "Plays when the turn changes and when someone draws a card.", sound, onChange = settings::setSound)
            ToggleRow("Vibration", "A short buzz when it becomes your turn.", haptics, onChange = settings::setHaptics)
            Text("Each player controls their own. You can also mute from the speaker button on the game table.", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
