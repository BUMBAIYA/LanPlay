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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class Screen { Games, GameMenu, Solo }

/** The app's navigation: one screen shown at a time, and Back goes up one level. */
@Composable
fun LanPlayApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Games) }
    BackHandler(enabled = screen != Screen.Games) {
        screen = if (screen == Screen.Solo) Screen.GameMenu else Screen.Games
    }
    when (screen) {
        Screen.Games -> GamesScreen(onOpen = { screen = Screen.GameMenu })
        Screen.GameMenu -> GameMenuScreen(onBack = { screen = Screen.Games }, onSolo = { screen = Screen.Solo })
        Screen.Solo -> SoloScreen(onBack = { screen = Screen.GameMenu })
    }
}

@Composable
private fun GamesScreen(onOpen: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("LanPlay", style = MaterialTheme.typography.headlineLarge)
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
private fun GameMenuScreen(onBack: () -> Unit, onSolo: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.lanPlayColors.accentText)) { Text("< Back") }
        Text("Combine Maths", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onSolo, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Play solo", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Host a game (coming soon)")
        }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Join a game (coming soon)")
        }
    }
}
