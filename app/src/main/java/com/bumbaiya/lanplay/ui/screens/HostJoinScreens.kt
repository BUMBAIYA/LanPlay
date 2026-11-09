package com.bumbaiya.lanplay.ui.screens

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.bumbaiya.lanplay.ui.components.IconLabel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.net.JoinLink
import com.bumbaiya.lanplay.platform.QrCode
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel

@Composable
fun HostScreen(onBack: () -> Unit) {
    val saved by Session.settings.name.collectAsState()
    var name by remember(saved) { mutableStateOf(saved) }
    val busy by Session.busy.collectAsState()
    val error by Session.error.collectAsState()
    ScreenColumn {
        BackLink(onBack)
        IconLabel(Icons.Filled.Home, "Host a game", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Turn on your phone's hotspot first. Friends join its Wi-Fi, then enter the room ID or scan the QR code you'll see next.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(value = name, onValueChange = { name = it.take(16) }, singleLine = true, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy && name.isNotBlank(),
            onClick = { Session.settings.setName(name); Session.host(name) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(if (busy) "Creating room..." else "Create room", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun JoinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val saved by Session.settings.name.collectAsState()
    var name by remember(saved) { mutableStateOf(saved) }
    var room by remember { mutableStateOf("") }
    val busy by Session.busy.collectAsState()
    val error by Session.error.collectAsState()
    val rooms by Session.discovery.rooms.collectAsState()

    // While this screen is open, keep looking for rooms on the network so they appear in the list.
    DisposableEffect(Unit) {
        Session.clearError()
        Session.discovery.startSearching()
        onDispose { Session.discovery.stopSearching() }
    }

    fun go(link: JoinLink) { Session.settings.setName(name); Session.join(link, name) }

    ScreenColumn {
        BackLink(onBack)
        IconLabel(Icons.Filled.Person, "Join a game", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Connect to the host's hotspot, then enter the room ID shown on their screen.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(value = name, onValueChange = { name = it.take(16) }, singleLine = true, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = room, onValueChange = { room = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(4) }, singleLine = true,
            label = { Text("Room ID") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (name.isNotBlank()) { Session.settings.setName(name); Session.joinByRoom(room, name) } }),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy && name.isNotBlank() && room.length == 4,
            onClick = { Session.settings.setName(name); Session.joinByRoom(room, name) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(if (busy) "Looking for room..." else "Join room", style = MaterialTheme.typography.titleMedium) }
        OutlinedButton(
            enabled = name.isNotBlank(),
            onClick = {
                QrCode.scan(context,
                    onResult = { text -> JoinLink.parse(text)?.let(::go) ?: Toast.makeText(context, "That is not a LanPlay QR code", Toast.LENGTH_SHORT).show() },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text("Scan QR code") }

        SectionLabel("Rooms nearby")
        if (rooms.isEmpty()) Text("None found yet. Make sure you're on the host's hotspot.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rooms.values.forEach { link ->
                OutlinedButton(onClick = { if (name.isNotBlank()) go(link) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Room ${link.room}", style = MaterialTheme.typography.titleMedium)
                        Text(link.host, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
