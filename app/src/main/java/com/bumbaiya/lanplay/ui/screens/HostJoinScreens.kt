package com.bumbaiya.lanplay.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.net.JoinLink
import com.bumbaiya.lanplay.platform.QrCode
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.TextBox
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

@Composable
fun HostScreen(onBack: () -> Unit) {
    val saved by Session.settings.name.collectAsState()
    var name by remember(saved) { mutableStateOf(saved) }
    val busy by Session.busy.collectAsState()
    val error by Session.error.collectAsState()
    ScreenColumn(gap = 24.dp) {
        Header("Host a game", onBack)
        Text("Turn on your phone's hotspot first. Friends join its Wi-Fi, then enter the room ID or scan the QR code you'll see next.", fontSize = 16.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your name · from Settings, editable for this room", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextBox(name, { name = it.take(16) })
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        BigButton(if (busy) "Creating room..." else "Create room", { Session.settings.setName(name); Session.host(name) }, icon = DI.Host, enabled = !busy && name.isNotBlank())
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
    val accent = MaterialTheme.colorScheme.primary

    // While this screen is open, keep looking for rooms on the network so they appear in the list.
    DisposableEffect(Unit) {
        Session.clearError()
        Session.discovery.startSearching()
        onDispose { Session.discovery.stopSearching() }
    }

    fun go(link: JoinLink) { Session.settings.setName(name); Session.join(link, name) }
    fun scan() = QrCode.scan(context,
        onResult = { text -> JoinLink.parse(text)?.let(::go) ?: Toast.makeText(context, "That is not a LanPlay QR code", Toast.LENGTH_SHORT).show() },
        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })

    ScreenColumn(gap = 24.dp) {
        Header("Join a room", onBack)
        BigButton("Scan the host's QR code", ::scan, icon = DI.Scan, primary = false)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Or enter the room ID", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { i ->
                    val ch = room.getOrNull(i)
                    Box(
                        Modifier.weight(1f).height(72.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .border(if (ch != null) 2.dp else 1.dp, if (ch != null) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(ch?.toString() ?: "_", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = if (ch != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.lanPlayColors.onDisabledContainer) }
                }
            }
            TextBox(
                room, { room = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(4) }, height = 48.dp,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (name.isNotBlank() && room.length == 4) { Session.settings.setName(name); Session.joinByRoom(room, name) } }),
            )
            Text("Ask the host for the 4-character room ID shown on their screen.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your name · from Settings, editable for this room", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextBox(name, { name = it.take(16) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Rooms found nearby")
            rooms.values.forEach { link ->
                Surface(
                    onClick = { if (name.isNotBlank()) go(link) }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(link.room, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Dhappa · host ${link.host}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Join", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.lanPlayColors.accentText)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 2.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                DIcon(DI.Spinner, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                Text("Searching the hotspot network…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(if (busy) "Looking for room..." else "Join room", { Session.settings.setName(name); Session.joinByRoom(room, name) }, icon = DI.Join, enabled = !busy && name.isNotBlank() && room.length == 4)
            Text("Connect to the host's hotspot in Wi-Fi settings first.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
