package com.bumbaiya.lanplay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.bumbaiya.lanplay.game.combinemaths.Phase
import com.bumbaiya.lanplay.net.ConnectionStatus
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class Screen { Games, GameMenu, Settings, Host, Join, Solo, HowTo }

/**
 * The app's navigation. If we're inside a multiplayer game, the game's phase decides what to show
 * (connecting, lobby, table). Otherwise the menu screens apply, and Back goes up one level.
 */
@Composable
fun LanPlayApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Games) }
    val client by Session.client.collectAsState()
    val inGame = client != null
    BackHandler(enabled = !inGame && screen != Screen.Games) {
        screen = when (screen) { Screen.Solo, Screen.Host, Screen.Join, Screen.HowTo -> Screen.GameMenu; else -> Screen.Games }
    }
    BackHandler(enabled = inGame) { Session.leave() }

    val c = client
    if (c != null) { GameFlow(c); return }
    when (screen) {
        Screen.Games -> GamesScreen(onOpen = { screen = Screen.GameMenu }, onSettings = { screen = Screen.Settings })
        Screen.GameMenu -> GameMenuScreen(onBack = { screen = Screen.Games }, onSolo = { screen = Screen.Solo }, onHost = { screen = Screen.Host }, onJoin = { screen = Screen.Join }, onHowTo = { screen = Screen.HowTo })
        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Games })
        Screen.Host -> HostScreen(onBack = { screen = Screen.GameMenu })
        Screen.Join -> JoinScreen(onBack = { screen = Screen.GameMenu })
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
        s == null -> ScreenColumn {
            Text(if (status is ConnectionStatus.Failed) (status as ConnectionStatus.Failed).reason else "Connecting...", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { Session.leave() }) { Text("Cancel") }
        }
        s.phase == Phase.Lobby -> LobbyScreen(client, s, onLeave = { Session.leave() })
        else -> TableScreen(client, s, onLeave = { Session.leave() })
    }
    if (s != null && status is ConnectionStatus.Reconnecting) {
        Text("Reconnecting...", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun GamesScreen(onOpen: () -> Unit, onSettings: () -> Unit) {
    ScreenColumn {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LanPlay", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = onSettings, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.lanPlayColors.accentText)) { Text("Settings") }
        }
        Text(
            "Games for the room you're in. One hotspot, no internet, no accounts.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("GAMES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            onClick = onOpen,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Combine Maths", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Build the target number from your cards before anyone else. Call DHAPPA when someone misses it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GameMenuScreen(onBack: () -> Unit, onSolo: () -> Unit, onHost: () -> Unit, onJoin: () -> Unit, onHowTo: () -> Unit) {
    ScreenColumn {
        BackLink(onBack)
        Text("Combine Maths", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = onHowTo, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("How to play", style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = onHost, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Host a game", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Join a game", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onSolo, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Play solo", style = MaterialTheme.typography.titleMedium)
        }
    }
}
