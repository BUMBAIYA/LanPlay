package com.bumbaiya.lanplay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.game.combinemaths.Phase
import com.bumbaiya.lanplay.net.ConnectionStatus
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.Chip
import com.bumbaiya.lanplay.ui.components.ChunkButton
import com.bumbaiya.lanplay.ui.components.Frame
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.StateArt
import com.bumbaiya.lanplay.ui.components.StateArtKind
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Inter
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Sand
import com.bumbaiya.lanplay.ui.theme.Teal

private enum class Screen { Games, GameMenu, Settings, Credits, Host, Join, Solo, HowTo, Tour }

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
        screen = when (screen) {
            Screen.Solo, Screen.Host, Screen.Join, Screen.HowTo, Screen.Tour -> Screen.GameMenu
            Screen.Credits -> Screen.Settings
            else -> Screen.Games
        }
    }
    BackHandler(enabled = inGame) { Session.leave() }

    val c = client
    if (c != null) { GameFlow(c); return }
    when (screen) {
        Screen.Games -> GamesScreen(onOpen = { screen = Screen.GameMenu }, onSettings = { screen = Screen.Settings })
        Screen.GameMenu -> GameMenuScreen(onBack = { screen = Screen.Games }, onSolo = { screen = Screen.Solo }, onHost = { screen = Screen.Host }, onJoin = { screen = Screen.Join }, onHowTo = { screen = Screen.HowTo }, onTour = { screen = Screen.Tour })
        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Games }, onCredits = { screen = Screen.Credits })
        Screen.Credits -> CreditsScreen(onBack = { screen = Screen.Settings })
        Screen.Host -> HostScreen(onBack = { screen = Screen.GameMenu })
        Screen.Join -> JoinScreen(onBack = { screen = Screen.GameMenu })
        Screen.Tour -> TourScreen(onBack = { screen = Screen.GameMenu }, onFullRules = { screen = Screen.HowTo })
        Screen.HowTo -> HowToPlayScreen(onBack = { screen = Screen.GameMenu }, onTour = { screen = Screen.Tour })
        Screen.Solo -> SoloScreen(onBack = { screen = Screen.GameMenu })
    }
}

/** Inside a multiplayer game: connecting, then lobby, then the table. */
@Composable
private fun GameFlow(client: GameClient) {
    val state by client.state.collectAsState()
    val status by client.status.collectAsState()
    val s = state
    val context = LocalContext.current
    LaunchedEffect(client) {
        client.messages.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
    }
    when {
        s != null && status is ConnectionStatus.Failed -> {
            val reason = (status as ConnectionStatus.Failed).reason
            if (reason.contains("removed")) StateScreen(StateArtKind.Kicked, "You were removed", "The host took you out of the room. You can join again with the room ID or QR code.", listOf("Removed" to Rose), "Back to home" to { Session.leave() })
            else StateScreen(StateArtKind.Disconnected, "The host left the game", "$reason. The room has closed.", listOf("Room closed" to Rose), "Back to home" to { Session.leave() })
        }
        s == null -> ConnectingScreen(status)
        s.phase == Phase.Lobby -> LobbyScreen(client, s, onLeave = { Session.leave() })
        else -> TableScreen(client, s, onLeave = { Session.leave() })
    }
    if (s != null && status is ConnectionStatus.Reconnecting) {
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                "Reconnecting…", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink,
                modifier = Modifier.background(Rose, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}

/** A full-screen state: art on the left, a title, a line of text, chips and one or two buttons. */
@Composable
private fun StateScreen(
    kind: StateArtKind, title: String, body: String, chips: List<Pair<String, androidx.compose.ui.graphics.Color>>,
    primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>? = null, primaryFill: androidx.compose.ui.graphics.Color = Amber, back: (() -> Unit)? = null,
) {
    Frame(cards = true) {
        Column(Modifier.fillMaxSize()) {
            Header("Combine", back ?: { Session.leave() })
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                StateArt(kind, Modifier.size(width = 220.dp, height = 165.dp))
                Column(Modifier.padding(start = 40.dp).width(340.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink)
                    Text(body, fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips.forEach { (t, c) -> Chip(t, fill = c, fontSize = 11.sp) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChunkButton(primary.first, primary.second, fill = primaryFill, height = 44.dp, fontSize = 13.sp)
                        if (secondary != null) ChunkButton(secondary.first, secondary.second, fill = Sand, height = 44.dp, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** Shown from tapping Join until the lobby arrives: which room, what is happening, and a way out. */
@Composable
private fun ConnectingScreen(status: ConnectionStatus) {
    val target by Session.joining.collectAsState()
    val failed = status as? ConnectionStatus.Failed
    val link = target?.first
    val chips = buildList {
        link?.takeIf { it.room != "DEBUG" }?.let { add("Room ${it.room}" to Amber) }
        link?.let { add("Host ${it.host}" to androidx.compose.ui.graphics.Color.White) }
        if (failed != null) add(failed.reason.take(28) to Rose)
    }
    if (failed == null) StateScreen(
        StateArtKind.Joining, "Joining the room…", "Looking for ${link?.host?.let { "$it's " } ?: "the "}room on your Wi-Fi. This usually takes a few seconds.", chips,
        "Cancel" to { Session.leave() }, primaryFill = Sand,
    ) else StateScreen(
        StateArtKind.Failed, "Couldn't join the room", "Check you are on the same Wi-Fi or hotspot as the host, and that the room code is right.", chips,
        "Try again" to { Session.retryJoin() }, "Change code" to { Session.leave() }, primaryFill = Teal,
    )
}
