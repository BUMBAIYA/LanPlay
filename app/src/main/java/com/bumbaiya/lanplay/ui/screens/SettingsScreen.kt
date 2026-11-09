package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.bumbaiya.lanplay.ui.components.IconLabel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.SettingRow
import com.bumbaiya.lanplay.ui.theme.ThemeMode

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = Session.settings
    val name by settings.name.collectAsState()
    val theme by settings.theme.collectAsState()
    val sound by settings.sound.collectAsState()
    val haptics by settings.haptics.collectAsState()
    ScreenColumn {
        BackLink(onBack)
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        SectionLabel("Your name")
        OutlinedTextField(
            value = name, onValueChange = { settings.setName(it.take(16)) }, singleLine = true,
            label = { Text("Used in every room") }, modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Theme")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(selected = theme == m, onClick = { settings.setTheme(m) }, label = { Text(m.name) })
            }
        }
        SectionLabel("Feedback")
        SettingRow("Sound on turn change and draws") { Switch(checked = sound, onCheckedChange = settings::setSound) }
        SettingRow("Vibration") { Switch(checked = haptics, onCheckedChange = settings::setHaptics) }
    }
}
