package com.bumbaiya.lanplay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.MenuTile
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.bumbaiya.lanplay.game.combinemaths.Phase
import com.bumbaiya.lanplay.net.ConnectionStatus
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class Screen { Games, GameMenu, Settings, Host, Join, Solo, HowTo, Tour }

/**
 * The app's navigation. If we're inside a multiplayer game, the game's phase decides what to show
 * (connecting, lobby, table). Otherwise, the menu screens apply, and Back goes up one level.
 */
@Composable
fun LanPlayApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Games) }
    val client by Session.client.collectAsState()
    val inGame = client != null
    BackHandler(enabled = !inGame && screen != Screen.Games) {
        screen = when (screen) { Screen.Solo, Screen.Host, Screen.Join, Screen.HowTo, Screen.Tour -> Screen.GameMenu; else -> Screen.Games }
    }
    BackHandler(enabled = inGame) { Session.leave() }

    val c = client
    if (c != null) { GameFlow(c); return }
    when (screen) {
        Screen.Games -> GamesScreen(onOpen = { screen = Screen.GameMenu }, onSettings = { screen = Screen.Settings })
        Screen.GameMenu -> GameMenuScreen(onBack = { screen = Screen.Games }, onSolo = { screen = Screen.Solo }, onHost = { screen = Screen.Host }, onJoin = { screen = Screen.Join }, onHowTo = { screen = Screen.HowTo }, onTour = { screen = Screen.Tour })
        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Games })
        Screen.Host -> HostScreen(onBack = { screen = Screen.GameMenu })
        Screen.Join -> JoinScreen(onBack = { screen = Screen.GameMenu })
        Screen.Tour -> TourScreen(onBack = { screen = Screen.GameMenu }, onFullRules = { screen = Screen.HowTo })
        Screen.HowTo -> HowToPlayScreen(onBack = { screen = Screen.GameMenu })
        Screen.Solo -> SoloScreen(onBack = { screen = Screen.GameMenu })
    }
}

/** Inside a multiplayer game: connecting, then lobby, then the table. */
@Composable
private fun GameFlow(client: GameClient) {
    val state by client.state.collectAsState()
    val status by client.status.collectAsState()
    val s = state
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(client) {
        client.messages.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
    }
    when {
        s != null && status is ConnectionStatus.Failed -> ScreenColumn(gap = 20.dp) {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 56.dp))
            Text(
                if ((status as ConnectionStatus.Failed).reason.contains("removed")) "You were removed" else "The host left the game", fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold, fontSize = 26.sp,
            )
            Text((status as ConnectionStatus.Failed).reason.let { if (it.contains("removed")) "$it." else "$it. The room has closed." }, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BigButton("Back to home", { Session.leave() }, icon = DI.Close, height = 56.dp)
        }
        s == null -> ConnectingScreen(status)
        s.phase == Phase.Lobby -> LobbyScreen(client, s, onLeave = { Session.leave() })
        else -> TableScreen(client, s, onLeave = { Session.leave() })
    }
    if (s != null && status is ConnectionStatus.Reconnecting) {
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                "Reconnecting…", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** Shown from tapping Join until the lobby arrives: which room, what is happening, and a way out. */
@Composable
private fun ConnectingScreen(status: ConnectionStatus) {
    val target by Session.joining.collectAsState()
    val failed = status as? ConnectionStatus.Failed
    val link = target?.first
    ScreenColumn(gap = 24.dp) {
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 40.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                if (failed == null) androidx.compose.material3.CircularProgressIndicator(Modifier.size(88.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 5.dp, trackColor = MaterialTheme.colorScheme.outline)
                else Box(Modifier.size(88.dp).background(MaterialTheme.lanPlayColors.dangerContainer, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                    DIcon(DI.Close, tint = MaterialTheme.lanPlayColors.onDangerContainer, size = 36.dp)
                }
            }
            Text(
                if (failed == null) "Joining room" else "Could not join",
                fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp,
            )
            link?.takeIf { it.room != "DEBUG" }?.let {
                Text(it.room, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = 8.sp, color = MaterialTheme.lanPlayColors.accentText)
            }
            Text(
                failed?.reason ?: "Connecting to the host…", fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (link != null) com.bumbaiya.lanplay.ui.components.Panel(Modifier) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Host address" to "${link.host}:${link.port}", "Playing as" to (target?.second ?: "")).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(k, Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        if (failed != null) {
            Text("Make sure both phones are on the same Wi-Fi or hotspot. Some Wi-Fi networks stop phones from talking to each other; if so, use the host's hotspot instead.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            BigButton("Try again", { Session.retryJoin() }, icon = DI.Join)
        }
        BigButton(if (failed != null) "Back" else "Cancel", { Session.leave() }, icon = DI.Close, primary = false, height = 52.dp, fontSize = 16)
    }
}

/** Three cards (6, +, 9) shown on the games list and the game menu, exactly as in the design. */
@Composable
private fun CardTrio() = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    DeckCard("6", CardKind.Number, size = DeckCardSize.Large)
    DeckCard("+", CardKind.Operator, size = DeckCardSize.Large)
    DeckCard("9", CardKind.Number, size = DeckCardSize.Large)
}

@Composable
private fun GamesScreen(onOpen: () -> Unit, onSettings: () -> Unit) {
    ScreenColumn(gap = 28.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DIcon(DI.Logo, tint = MaterialTheme.lanPlayColors.accentText, size = 32.dp)
                    Text("LanPlay", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp)
                }
                Box(Modifier.size(44.dp).clickable(onClick = onSettings), contentAlignment = Alignment.Center) { DIcon(DI.Sliders) }
            }
            Text("Games for the room you're in. One hotspot, no internet, no accounts.", fontSize = 16.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Choose a game", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Surface(
                onClick = onOpen, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CardTrio()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Dhappa", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("Build the target number from your cards, using your own mental math. Catch anyone who passes.", fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DIcon(DI.Right, tint = MaterialTheme.lanPlayColors.accentText)
                    }
                }
            }
            val dash = MaterialTheme.colorScheme.outline
            Box(
                Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(horizontal = 0.dp).drawBehind {
                    drawRoundRect(dash, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center,
            ) { Text("More games coming. Any game, cards or not.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}

@Composable
private fun GameMenuScreen(onBack: () -> Unit, onSolo: () -> Unit, onHost: () -> Unit, onJoin: () -> Unit, onHowTo: () -> Unit, onTour: () -> Unit) {
    ScreenColumn(gap = 24.dp) {
        Header("", onBack, caption = "Games")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CardTrio()
            Text("Dhappa", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Text("Combine your number and operator cards to hit the target. You don't have to use every card. Spot a player who missed it and catch them.", fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuTile("Host a game", "Create a room, friends join you", DI.Host, onHost, primary = true)
            MenuTile("Join a game", "Enter the room ID from the host", DI.Join, onJoin)
            MenuTile("Play solo", "Set your own target, race the timer", DI.Solo, onSolo)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("How to play", onTour, Modifier.weight(1f), icon = DI.Tour, primary = false, height = 48.dp, fontSize = 15)
            BigButton("Full rules", onHowTo, Modifier.weight(1f), icon = DI.Book, primary = false, height = 48.dp, fontSize = 15)
        }
    }
}
