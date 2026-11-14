package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bumbaiya.lanplay.game.combinemaths.GameEngine
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.game.combinemaths.Settings
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.QrCode
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.Avatar
import com.bumbaiya.lanplay.ui.components.CapsLabel
import com.bumbaiya.lanplay.ui.components.Chip
import com.bumbaiya.lanplay.ui.components.Chunk
import com.bumbaiya.lanplay.ui.components.ChunkButton
import com.bumbaiya.lanplay.ui.components.CombineArt
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.Frame
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.components.SquareButton
import com.bumbaiya.lanplay.ui.components.StepperCard
import com.bumbaiya.lanplay.ui.components.ToggleSwitch
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.AmberSoft
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Inter
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.PanelBg
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Sand
import com.bumbaiya.lanplay.ui.theme.Teal
import com.bumbaiya.lanplay.ui.theme.TealDark
import com.bumbaiya.lanplay.ui.theme.Terra

val PlayerColors = listOf(Amber, Teal, Rose, Amber, Terra, Teal)

/** A centred card over a dimmed screen. Used for the kick prompt, the big QR code and the standings. */
@Composable
fun ModalCard(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismiss, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(Modifier.padding(24.dp), padding = 18.dp) { content() }
    }
}

@Composable
fun LobbyScreen(client: GameClient, state: GameState, onLeave: () -> Unit) {
    val me by client.myId.collectAsState()
    val link by Session.hostLink.collectAsState()
    val isAdmin = me == state.adminId
    val mine = state.player(me ?: "")
    val s = state.settings
    val canStart = isAdmin && state.players.size >= GameEngine.MIN_PLAYERS && state.players.all { it.ready }
    val waitingFor = state.players.firstOrNull { !it.ready }

    var editing by remember { mutableStateOf(false) }
    var kickId by remember { mutableStateOf<String?>(null) }
    var showQr by remember { mutableStateOf(false) }

    if (editing && isAdmin) {
        GameSettingsScreen(s, { client.send(Intent.UpdateSettings(it)) }, { editing = false })
        return
    }

    state.player(kickId ?: "")?.let { who ->
        ModalCard({ kickId = null }) {
            Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Remove ${who.name}?", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                Text("They will be taken out of the room. They can only come back by entering the room ID or scanning the QR code again.", fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, color = Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    ChunkButton("Keep", { kickId = null }, fill = Sand, icon = DI.Check, height = 42.dp)
                    ChunkButton("Remove", { client.send(Intent.Kick(who.id)); kickId = null }, fill = Rose, icon = DI.Close, height = 42.dp)
                }
            }
        }
    }
    link?.takeIf { showQr }?.let { l ->
        ModalCard({ showQr = false }) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                val qr = remember(l) { QrCode.generate(l.toText()) }
                Image(qr, "QR code to join room ${l.room}", Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp), filterQuality = FilterQuality.None)
                Column(Modifier.width(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scan to join", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                    Text(l.room, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = 5.sp, color = TealDark)
                    Text("Or type this room ID on the Join screen.", fontFamily = Inter, fontSize = 12.sp, color = Muted)
                    ChunkButton("Close", { showQr = false }, fill = Sand, height = 42.dp)
                }
            }
        }
    }

    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Lobby", onLeave, subtitle = "Combine · ${GameEngine.MIN_PLAYERS} to ${GameEngine.MAX_PLAYERS} players",
                lead = { Box(Modifier.size(44.dp).background(AmberSoft, CircleShape).border(2.dp, Ink, CircleShape)) { CombineArt(Modifier.fillMaxSize().padding(5.dp)) } },
                trailing = {
                    Chunk(Modifier.height(44.dp), fill = Amber, radius = 14.dp, shadow = 4.dp, contentAlignment = Alignment.Center) {
                        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ROOM", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp, color = Ink.copy(alpha = 0.7f))
                            Text(state.roomId, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 3.sp, color = Ink)
                        }
                    }
                    if (link != null) SquareButton(DI.Scan, { showQr = true }, size = 44.dp, fill = PanelBg)
                },
            )
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        CapsLabel("Players · ${state.players.size} of ${GameEngine.MAX_PLAYERS}")
                        Spacer(Modifier.weight(1f))
                        Text("${state.players.count { it.ready }} READY", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = TealDark)
                    }
                    val cells = state.players.size + if (state.players.size < GameEngine.MAX_PLAYERS) 1 else 0
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { r ->
                            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(2) { c ->
                                    val i = r * 2 + c
                                    val cell = Modifier.weight(1f).fillMaxHeight()
                                    val p = state.players.getOrNull(i)
                                    when {
                                        p != null -> PlayerCell(cell, p.name, i, p.ready, p.id == me, p.id == state.adminId, isAdmin && p.id != me) { kickId = p.id }
                                        i < cells -> OpenSeat(cell)
                                        else -> Spacer(cell)
                                    }
                                }
                            }
                        }
                    }
                }
                Column(Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Panel(Modifier.weight(1f).fillMaxWidth(), padding = 12.dp) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CapsLabel("Game settings", color = Ink)
                                Spacer(Modifier.weight(1f))
                                if (isAdmin) Text("Admin only", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Terra)
                            }
                            Row {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    KV("Turn timer", "${s.turnSeconds} s"); KV("Penalty", "${s.penaltySeconds} s"); KV("Round win", "${s.roundWinPoints}")
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    KV("Catch window", "${s.catchWindowSeconds} s"); KV("Points to win", "${s.winScore}"); KV("Catch", "${s.catchPoints}")
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            if (isAdmin) ChunkButton("Change settings", { editing = true }, Modifier.fillMaxWidth(), fill = Sand, icon = DI.Sliders, height = 40.dp, fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (mine?.ready == true) ChunkButton("I'm not ready", { client.send(Intent.SetReady(false)) }, Modifier.weight(1f), fill = Sand, height = 44.dp, fontSize = 12.sp)
                        else ChunkButton("I'm ready", { client.send(Intent.SetReady(true)) }, Modifier.weight(1f), fill = Amber, icon = DI.Check, height = 44.dp, fontSize = 12.sp)
                        if (isAdmin) ChunkButton("Start game", { client.send(Intent.Start) }, Modifier.weight(1f), icon = DI.Play, enabled = canStart, height = 44.dp, fontSize = 12.sp)
                    }
                    Text(
                        when {
                            canStart -> "Everyone is ready. Start when you like."
                            state.players.size < GameEngine.MIN_PLAYERS -> "Waiting for players. You need at least ${GameEngine.MIN_PLAYERS}."
                            waitingFor != null -> "Waiting for ${waitingFor.name} to be ready."
                            else -> "Waiting for the admin to start."
                        },
                        Modifier.fillMaxWidth(), fontFamily = Inter, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center, maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun KV(k: String, v: String) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(k, Modifier.weight(1f), fontFamily = Inter, fontSize = 11.sp, color = Muted, maxLines = 1)
    Text(v, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
}

@Composable
private fun PlayerCell(modifier: Modifier, name: String, index: Int, ready: Boolean, isMe: Boolean, isAdmin: Boolean, canKick: Boolean, onKick: () -> Unit) {
    Chunk(modifier, fill = if (isMe) AmberSoft else Sand, radius = 14.dp, border = 2.dp, shadow = 3.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(name, PlayerColors[index % PlayerColors.size], 34.dp)
            Column(Modifier.weight(1f)) {
                Text(name, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                val tag = listOfNotNull("Admin".takeIf { isAdmin }, "You".takeIf { isMe }).joinToString(" · ")
                if (tag.isNotEmpty()) Text(tag, fontFamily = Inter, fontSize = 10.sp, color = Muted)
            }
            if (ready) Box(Modifier.size(24.dp).background(Teal, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) { DIcon(DI.Check, tint = Ink, size = 13.dp) }
            else Box(Modifier.drawBehind { drawRoundRect(Muted, cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))) }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("Waiting", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Muted)
            }
            if (canKick) SquareButton(DI.Close, onKick, size = 26.dp, fill = Rose, tint = Ink)
        }
    }
}

@Composable
private fun OpenSeat(modifier: Modifier) {
    Box(modifier.drawBehind { drawRoundRect(Ink, cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))) }, contentAlignment = Alignment.Center) {
        Text("Open seat", fontFamily = Inter, fontSize = 11.sp, color = Muted)
    }
}

/** The host's settings screen: six stepper cards, the live-result switch and Done. */
@Composable
fun GameSettingsScreen(s: Settings, change: (Settings) -> Unit, onDone: () -> Unit) {
    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Game settings", onDone, trailing = { Chip("Host only") })
            Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperCard("Turn timer", "${s.turnSeconds}", "s", { change(s.copy(turnSeconds = s.turnSeconds - 5)) }, { change(s.copy(turnSeconds = s.turnSeconds + 5)) }, Modifier.weight(1f))
                StepperCard("Catch window", "${s.catchWindowSeconds}", "s", { change(s.copy(catchWindowSeconds = s.catchWindowSeconds - 1)) }, { change(s.copy(catchWindowSeconds = s.catchWindowSeconds + 1)) }, Modifier.weight(1f))
                StepperCard("Wrong answer penalty", "${s.penaltySeconds}", "s", { change(s.copy(penaltySeconds = s.penaltySeconds - 1)) }, { change(s.copy(penaltySeconds = s.penaltySeconds + 1)) }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperCard("Points to win", "${s.winScore}", "pts", { change(s.copy(winScore = s.winScore - 1)) }, { change(s.copy(winScore = s.winScore + 1)) }, Modifier.weight(1f))
                StepperCard("Points for a round win", "${s.roundWinPoints}", "pts", { change(s.copy(roundWinPoints = s.roundWinPoints - 1)) }, { change(s.copy(roundWinPoints = s.roundWinPoints + 1)) }, Modifier.weight(1f))
                StepperCard("Points for a catch", "${s.catchPoints}", "pts", { change(s.copy(catchPoints = s.catchPoints - 1)) }, { change(s.copy(catchPoints = s.catchPoints + 1)) }, Modifier.weight(1f))
            }
            Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show live result while building", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                        Text("Off by default. Keep it off so players do the maths in their heads.", fontFamily = Inter, fontSize = 11.sp, color = Muted)
                    }
                    ToggleSwitch(s.showLiveResult, { change(s.copy(showLiveResult = it)) })
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ChunkButton("Done", onDone, icon = DI.Check) }
        }
    }
}
