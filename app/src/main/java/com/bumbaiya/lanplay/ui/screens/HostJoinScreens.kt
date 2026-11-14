package com.bumbaiya.lanplay.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.game.combinemaths.PlayerName
import com.bumbaiya.lanplay.net.JoinLink
import com.bumbaiya.lanplay.platform.QrCode
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.Avatar
import com.bumbaiya.lanplay.ui.components.CapsLabel
import com.bumbaiya.lanplay.ui.components.ChunkButton
import com.bumbaiya.lanplay.ui.components.Chunk
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.Frame
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.IconDisc
import com.bumbaiya.lanplay.ui.components.InkField
import com.bumbaiya.lanplay.ui.components.NameMax
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.AmberSoft
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Inter
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Sand
import com.bumbaiya.lanplay.ui.theme.Sand2
import com.bumbaiya.lanplay.ui.theme.Teal
import com.bumbaiya.lanplay.ui.theme.TealDark

@Composable
fun HostScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val saved by Session.settings.name.collectAsState()
    var name by remember(saved) { mutableStateOf(saved) }
    val busy by Session.busy.collectAsState()
    val error by Session.error.collectAsState()
    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Host a game", onBack, subtitle = "Combine · up to 6 players", lead = { IconDisc(DI.Host, Color(0xFFA9DDD5), Teal) })
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Panel(Modifier.weight(1f).fillMaxHeight(), padding = 16.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CapsLabel("Your name in this room", color = Ink)
                        InkField(name, { name = PlayerName.clean(it) }, Modifier.fillMaxWidth(), maxLength = NameMax, height = 44.dp, placeholder = "Your name")
                        Text("Letters only, up to $NameMax. No numbers, spaces or symbols.", fontFamily = Inter, fontSize = 10.sp, color = Muted)
                        CapsLabel("Connection", color = Ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Chunk(Modifier.weight(1f).height(56.dp), fill = Teal, radius = 12.dp, border = 2.dp, shadow = 3.dp) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DIcon(DI.Host, tint = Ink, size = 20.dp)
                                    Column {
                                        Text("Same network", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                                        Text("Wi-Fi or hotspot", fontFamily = Inter, fontSize = 10.sp, color = Ink.copy(alpha = 0.7f), maxLines = 1)
                                    }
                                }
                            }
                            Chunk(Modifier.weight(1f).height(56.dp), onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }, fill = Sand, radius = 12.dp, border = 2.dp, shadow = 3.dp) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DIcon(DI.Leave, tint = Ink, size = 18.dp)
                                    Column {
                                        Text("Use hotspot", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                                        Text("Open settings", fontFamily = Inter, fontSize = 10.sp, color = Muted, maxLines = 1)
                                    }
                                }
                            }
                        }
                        error?.let { Text(it, fontFamily = Inter, fontSize = 11.sp, color = Rose) }
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ChunkButton(if (busy) "Creating room…" else "Create room", { Session.settings.setName(name); Session.host(name) }, icon = DI.Play, enabled = !busy && name.isNotBlank()) }
                    }
                }
                Panel(Modifier.weight(1f).fillMaxHeight(), fill = com.bumbaiya.lanplay.ui.theme.PanelBg, padding = 16.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CapsLabel("How friends join")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).background(Color.White, RoundedCornerShape(12.dp)).border(2.dp, Ink, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { DIcon(DI.Scan, tint = Ink, size = 40.dp) }
                            Text("After you create the room, a QR and a 4-character room ID appear. Friends scan or type it.", fontFamily = Inter, fontSize = 11.sp, lineHeight = 15.sp, color = Ink)
                        }
                        Step(1, Amber, "Everyone joins the same Wi-Fi or hotspot")
                        Step(2, Teal, "They tap Join a game")
                        Step(3, Amber, "Everyone taps Ready, you press Start")
                    }
                }
            }
        }
    }
}

@Composable
private fun Step(n: Int, color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(24.dp).background(color, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
            Text("$n", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Ink)
        }
        Text(text, fontFamily = Inter, fontSize = 11.sp, color = Ink)
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
    fun scan() = QrCode.scan(context,
        onResult = { text -> JoinLink.parse(text)?.let(::go) ?: Toast.makeText(context, "That is not a LanPlay QR code", Toast.LENGTH_SHORT).show() },
        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
    val canJoin = !busy && name.isNotBlank() && room.length == 4

    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Join a game", onBack, subtitle = "Same Wi-Fi or hotspot as the host", lead = { IconDisc(DI.Join, Color(0xFFF4B5C1), Amber) })
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Panel(Modifier.weight(1f).fillMaxHeight(), padding = 16.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CapsLabel("Room ID", color = Ink)
                        Box {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(4) { i ->
                                    val ch = room.getOrNull(i)
                                    Box(Modifier.weight(1f).height(52.dp).background(Color.White, RoundedCornerShape(12.dp)).border(2.5.dp, Ink, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                        Text(ch?.toString() ?: "", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TealDark)
                                    }
                                }
                            }
                            // An invisible field on top of the boxes takes the typing.
                            InkField(
                                room, { room = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(4) }, Modifier.matchParentSize().androidAlpha0(), maxLength = 4, height = 52.dp,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { if (canJoin) { Session.settings.setName(name); Session.joinByRoom(room, name) } }),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            InkField(name, { name = PlayerName.clean(it) }, Modifier.weight(1f), label = "Name", maxLength = NameMax, height = 42.dp, fontSize = 13.sp)
                            Chunk(Modifier.height(42.dp), onClick = ::scan, fill = Ink, radius = 12.dp, border = 2.dp, shadow = 3.dp, contentAlignment = Alignment.Center) {
                                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DIcon(DI.Scan, tint = Color.White, size = 15.dp)
                                    Text("Scan QR", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                        error?.let { Text(it, fontFamily = Inter, fontSize = 11.sp, color = Rose) }
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ChunkButton(if (busy) "Looking for room…" else "Join room", { Session.settings.setName(name); Session.joinByRoom(room, name) }, icon = DI.Check, enabled = canJoin) }
                    }
                }
                Panel(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CapsLabel("Rooms nearby")
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.size(8.dp).background(Amber, CircleShape))
                            Text("  Searching", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = TealDark)
                        }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rooms.values.forEach { link ->
                                val picked = link.room == room
                                Chunk(Modifier.fillMaxWidth().height(52.dp), onClick = { room = link.room }, fill = Sand, radius = 12.dp, border = 2.dp, shadow = 3.dp, borderColor = if (picked) Teal else Ink) {
                                    Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Avatar(link.host, Teal, 30.dp)
                                        Column(Modifier.weight(1f)) {
                                            Text("${link.host}'s room", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                                            Text("Combine", fontFamily = Inter, fontSize = 10.sp, color = Muted)
                                        }
                                        Text(link.room, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp, color = TealDark)
                                    }
                                }
                            }
                            if (rooms.isEmpty()) Box(Modifier.fillMaxWidth().height(60.dp).background(Sand, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("Tap a room to fill the code, or type it on the left.", fontFamily = Inter, fontSize = 10.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.androidAlpha0() = this.then(Modifier.alpha(0f))
