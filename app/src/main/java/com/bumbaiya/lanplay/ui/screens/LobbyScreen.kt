package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.bumbaiya.lanplay.ui.components.IconLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.GameEngine
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.QrCode
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.CheckRow
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.StepperRow
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

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

    ScreenColumn {
        BackLink(onLeave, "Leave room")
        SectionLabel("Room ID")
        Text(state.roomId, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.lanPlayColors.accentText)
        link?.let { l ->
            // Only the host knows the address, so only the host shows the QR code.
            val qr = remember(l) { QrCode.generate(l.toText()) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(qr, "QR code to join room ${l.room}", Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp))
                Text("Friends can scan this, or type the room ID. Hotspot address ${l.host}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionLabel("Players (${state.players.size})")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.players.forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name + (if (p.id == state.adminId) "  (admin)" else "") + (if (p.id == me) "  (you)" else ""), style = MaterialTheme.typography.bodyLarge)
                    Text(if (p.ready) "Ready" else "Not ready", color = if (p.ready) MaterialTheme.lanPlayColors.accentText else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (mine?.ready == true) OutlinedButton(onClick = { client.send(Intent.SetReady(false)) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { IconLabel(Icons.Filled.Close, "I'm not ready") }
        else Button(onClick = { client.send(Intent.SetReady(true)) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { IconLabel(Icons.Filled.Check, "I'm ready", style = MaterialTheme.typography.titleMedium) }

        HorizontalDivider()
        SectionLabel(if (isAdmin) "Game settings" else "Game settings (set by the admin)")
        StepperRow("Turn timer", "${s.turnSeconds}s", isAdmin, { change(s.copy(turnSeconds = s.turnSeconds - 5)) }, { change(s.copy(turnSeconds = s.turnSeconds + 5)) })
        StepperRow("Catch window", "${s.catchWindowSeconds}s", isAdmin, { change(s.copy(catchWindowSeconds = s.catchWindowSeconds - 1)) }, { change(s.copy(catchWindowSeconds = s.catchWindowSeconds + 1)) })
        StepperRow("Wrong answer penalty", "${s.penaltySeconds}s", isAdmin, { change(s.copy(penaltySeconds = s.penaltySeconds - 1)) }, { change(s.copy(penaltySeconds = s.penaltySeconds + 1)) })
        StepperRow("Points to win", "${s.winScore}", isAdmin, { change(s.copy(winScore = s.winScore - 1)) }, { change(s.copy(winScore = s.winScore + 1)) })
        StepperRow("Points for a round win", "${s.roundWinPoints}", isAdmin, { change(s.copy(roundWinPoints = s.roundWinPoints - 1)) }, { change(s.copy(roundWinPoints = s.roundWinPoints + 1)) })
        StepperRow("Points for a catch", "${s.catchPoints}", isAdmin, { change(s.copy(catchPoints = s.catchPoints - 1)) }, { change(s.copy(catchPoints = s.catchPoints + 1)) })
        CheckRow("Show live result while building", s.showLiveResult, enabled = isAdmin) { change(s.copy(showLiveResult = it)) }

        if (isAdmin) {
            Button(
                enabled = canStart, onClick = { client.send(Intent.Start) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { IconLabel(Icons.Filled.PlayArrow, if (canStart) "Start game" else if (state.players.size < GameEngine.MIN_PLAYERS) "Waiting for players" else "Waiting for everyone to be ready", style = MaterialTheme.typography.titleMedium) }
        } else Text("Waiting for the admin to start...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}
