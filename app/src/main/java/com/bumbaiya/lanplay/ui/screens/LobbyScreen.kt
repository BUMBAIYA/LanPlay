package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.bumbaiya.lanplay.game.combinemaths.GameEngine
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.QrCode
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.StepperRow
import com.bumbaiya.lanplay.ui.components.ToggleRow
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

private val Avatars = listOf(Color(0xFFB6F24A), Color(0xFF5BA3E6), Color(0xFFF26B5B), Color(0xFFF2B84B), Color(0xFFE879B9), Color(0xFF9B6CFF), Color(0xFF4DD2C0), Color(0xFFFF9E57))

@Composable
fun LobbyScreen(client: GameClient, state: GameState, onLeave: () -> Unit) {
    val me by client.myId.collectAsState()
    val link by Session.hostLink.collectAsState()
    val isAdmin = me == state.adminId
    val mine = state.player(me ?: "")
    val s = state.settings
    fun change(new: com.bumbaiya.lanplay.game.combinemaths.Settings) = client.send(Intent.UpdateSettings(new))
    val everyoneReady = state.players.all { it.ready }
    val canStart = isAdmin && state.players.size >= GameEngine.MIN_PLAYERS && everyoneReady
    val waitingFor = state.players.firstOrNull { !it.ready }

    var editing by remember { mutableStateOf(false) }
    var kickId by remember { mutableStateOf<String?>(null) }
    var showQr by remember { mutableStateOf(false) }
    state.player(kickId ?: "")?.let { who ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { kickId = null },
            containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Remove ${who.name}?") },
            text = { Text("They will be taken out of the room. They can only join again by entering the room ID or scanning the QR code anew.") },
            confirmButton = { androidx.compose.material3.TextButton({ client.send(Intent.Kick(who.id)); kickId = null }) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { com.bumbaiya.lanplay.ui.components.DIcon(com.bumbaiya.lanplay.ui.components.DI.Close, tint = MaterialTheme.lanPlayColors.danger, size = 18.dp); Text("Remove", color = MaterialTheme.lanPlayColors.danger, fontWeight = FontWeight.SemiBold) } } },
            dismissButton = { androidx.compose.material3.TextButton({ kickId = null }) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { com.bumbaiya.lanplay.ui.components.DIcon(com.bumbaiya.lanplay.ui.components.DI.Check, tint = MaterialTheme.colorScheme.onSurface, size = 18.dp); Text("Keep", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) } } },
        )
    }
    link?.takeIf { showQr }?.let { l ->
        androidx.compose.ui.window.Dialog({ showQr = false }) {
            Column(
                Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Scan to join", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                val qr = remember(l) { QrCode.generate(l.toText()) }
                Image(qr, "Large QR code to join room ${l.room}", Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(10.dp), filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
                Text(l.room, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 6.sp, color = MaterialTheme.lanPlayColors.accentText)
                Text("Or type this room ID on the Join screen.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                BigButton("Close", { showQr = false }, primary = false, height = 48.dp, fontSize = 16)
            }
        }
    }
    if (editing && isAdmin) {
        ScreenColumn(gap = 12.dp) {
            Header("Game settings", { editing = false })
        run {
            StepperRow("Turn timer", "${s.turnSeconds} s", true, { change(s.copy(turnSeconds = s.turnSeconds - 5)) }, { change(s.copy(turnSeconds = s.turnSeconds + 5)) })
            StepperRow("Catch window", "${s.catchWindowSeconds} s", true, { change(s.copy(catchWindowSeconds = s.catchWindowSeconds - 1)) }, { change(s.copy(catchWindowSeconds = s.catchWindowSeconds + 1)) })
            StepperRow("Wrong answer penalty", "${s.penaltySeconds} s", true, { change(s.copy(penaltySeconds = s.penaltySeconds - 1)) }, { change(s.copy(penaltySeconds = s.penaltySeconds + 1)) })
            StepperRow("Points to win", "${s.winScore}", true, { change(s.copy(winScore = s.winScore - 1)) }, { change(s.copy(winScore = s.winScore + 1)) })
            StepperRow("Points for a round win", "${s.roundWinPoints}", true, { change(s.copy(roundWinPoints = s.roundWinPoints - 1)) }, { change(s.copy(roundWinPoints = s.roundWinPoints + 1)) })
            StepperRow("Points for a catch", "${s.catchPoints}", true, { change(s.copy(catchPoints = s.catchPoints - 1)) }, { change(s.copy(catchPoints = s.catchPoints + 1)) })
            ToggleRow("Show live result while building", "Off by default. Keep it off so players do the math in their heads.", s.showLiveResult, enabled = true) { change(s.copy(showLiveResult = it)) }
        }

            BigButton("Done", { editing = false }, icon = DI.Check)
        }
        return
    }

    ScreenColumn(gap = 20.dp) {
        Header("Lobby", onLeave)

        Panel(Modifier, radius = 16.dp) {
            Row(Modifier.padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Room ID", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.roomId, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = 6.sp, color = MaterialTheme.lanPlayColors.accentText)
                    Text("Dhappa · ${GameEngine.MIN_PLAYERS} to ${GameEngine.MAX_PLAYERS} players", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                link?.let { l ->
                    // Only the host knows the address, so only the host shows the QR code.
                    val qr = remember(l) { QrCode.generate(l.toText()) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { showQr = true }) {
                        Image(qr, "QR code to join room ${l.room}. Tap to enlarge.", Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(6.dp), filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
                        Text("Tap to enlarge", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                SectionLabel("Players · ${state.players.size} of ${GameEngine.MAX_PLAYERS}")
                Text("${state.players.count { it.ready }} ready", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.players.forEachIndexed { i, p ->
                Panel(Modifier) {
                    Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Avatars[i % Avatars.size]), contentAlignment = Alignment.Center) {
                            Text(p.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF0E1116))
                        }
                        Text(
                            buildAnnotatedName(p.name, when { p.id == state.adminId && p.id == me -> "Admin, You"; p.id == state.adminId -> "Admin"; p.id == me -> "You"; else -> null }),
                            Modifier.weight(1f), fontWeight = FontWeight.Medium,
                        )
                        val colors = MaterialTheme.lanPlayColors
                        Text(
                            if (p.ready) "Ready" else "Not ready", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (p.ready) colors.onSuccessContainer else Color(0xFFC9D0DB).takeIf { isDark() } ?: colors.onDisabledContainer,
                            modifier = Modifier.background(if (p.ready) colors.successContainer else colors.disabledContainer, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                        if (isAdmin && p.id != me) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(colors.dangerContainer).clickable { kickId = p.id },
                                contentAlignment = Alignment.Center,
                            ) { com.bumbaiya.lanplay.ui.components.DIcon(DI.Close, tint = colors.onDangerContainer, size = 18.dp) }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Game settings")
            Panel(Modifier) {
                listOf(
                    "Turn timer" to "${s.turnSeconds} s", "Catch window" to "${s.catchWindowSeconds} s", "Wrong answer penalty" to "${s.penaltySeconds} s",
                    "Points to win" to "${s.winScore}", "Points for a round win" to "${s.roundWinPoints}", "Points for a catch" to "${s.catchPoints}",
                    "Live result while building" to if (s.showLiveResult) "On" else "Off",
                ).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(k, Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            if (isAdmin) BigButton("Change settings", { editing = true }, icon = DI.Sliders, primary = false, height = 48.dp, fontSize = 16)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mine?.ready == true) BigButton("I'm not ready", { client.send(Intent.SetReady(false)) }, icon = DI.Close, primary = false, height = 48.dp, fontSize = 16)
            else BigButton("I'm ready", { client.send(Intent.SetReady(true)) }, icon = DI.Check)
            if (isAdmin) BigButton("Start game", { client.send(Intent.Start) }, icon = DI.Play, enabled = canStart)
            Text(
                when {
                    canStart -> "Everyone is ready. Start when you like."
                    state.players.size < GameEngine.MIN_PLAYERS -> "Waiting for players. Everyone must be ready, and at least ${GameEngine.MIN_PLAYERS} players are needed."
                    waitingFor != null -> "Waiting for ${waitingFor.name} to be ready. Everyone must be ready, and at least ${GameEngine.MIN_PLAYERS} players are needed."
                    else -> "Waiting for the admin to start."
                },
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable private fun isDark() = MaterialTheme.colorScheme.background.red < 0.5f

@Composable
private fun buildAnnotatedName(name: String, tag: String?): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    append(name)
    if (tag != null) withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" · $tag") }
}
