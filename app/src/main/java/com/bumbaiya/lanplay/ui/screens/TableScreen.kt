package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.game.combinemaths.Phase
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.Feedback
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
import com.bumbaiya.lanplay.ui.game.toBCard
import com.bumbaiya.lanplay.ui.game.toEngineHand
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.bumbaiya.lanplay.game.combinemaths.RowSlot
import com.bumbaiya.lanplay.ui.game.WatchedRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TableScreen(client: GameClient, state: GameState, onLeave: () -> Unit) {
    val me by client.myId.collectAsState()
    val myId = me ?: return
    val phase = state.phase
    // The clock only changes once per second as far as the screen is concerned. Rounding down means the
    // value is identical for 4 of every 5 checks, and Compose skips redrawing when a state doesn't change.
    val now by produceState((client.serverNow() / 1000) * 1000) { while (true) { value = (client.serverNow() / 1000) * 1000; delay(200) } }
    var confirmLeave by remember { mutableStateOf(false) }
    var catching by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val isAdmin = myId == state.adminId
    val myHand = state.hands[myId]
    val iAmOut = myHand?.out == true

    // Beep and buzz when the turn becomes mine.
    val currentId = (phase as? Phase.Turns)?.currentId
    LaunchedEffect(currentId) {
        if (currentId == myId) { Feedback.turn(); if (Session.settings.haptics.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    LaunchedEffect(phase is Phase.Turns) { if (phase !is Phase.Turns) catching = null }

    ScreenColumn {
        BackLink({ confirmLeave = true }, "< Leave game")
        Text("Round ${state.round}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.notice.isNotBlank()) Text(state.notice, style = MaterialTheme.typography.titleMedium)

        SectionLabel("Target")
        val shown = (phase as? Phase.DrawingTarget)?.digits ?: state.target
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.isEmpty()) Text("Not drawn yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            shown.forEach { DeckCard(it.toString(), CardKind.Number, size = DeckCardSize.Large) }
        }

        when (phase) {
            is Phase.DrawingTarget -> {
                val drawer = state.player(phase.drawerId)?.name ?: "Someone"
                if (phase.drawerId == myId) {
                    Text("You were picked to draw the target. Stop whenever you like: 1, 2 or 3 cards.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(enabled = phase.digits.size < 3, onClick = { client.send(Intent.DrawTargetCard) }) { Text("Draw a target card") }
                        Button(enabled = phase.digits.isNotEmpty(), onClick = { client.send(Intent.StopTarget) }) { Text("Stop here") }
                    }
                } else Text("$drawer is drawing the target...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is Phase.Turns -> TurnsSection(client, state, phase, now, myId, iAmOut, catching) { catching = it }
            is Phase.RoundEnd, is Phase.Finished -> Unit
            Phase.Lobby -> Unit
        }

        SectionLabel("Players")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.players.forEach { p ->
                val rp = state.hands[p.id]
                val turn = (phase as? Phase.Turns)?.currentId == p.id
                val window = state.catchWindows.firstOrNull { it.playerId == p.id && it.closesAt >= now }
                Surface(
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (turn) 2.dp else 1.dp, if (turn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.name + if (p.id == myId) " (you)" else "", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when { !p.connected -> "offline"; rp?.out == true -> "OUT"; turn -> "turn"; else -> "" } + "   ${p.score} pts",
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rp?.hand?.forEach { c -> val b = c.toBCard(); DeckCard(b.glyph, b.kind, size = DeckCardSize.Small, dimmed = rp.out) }
                        }
                        if (turn && p.id != myId) WatchedRow(state.liveRow, p.name)
                        if (window != null && p.id != myId && !iAmOut && phase is Phase.Turns) {
                            val secs = ((window.closesAt - now) / 1000 + 1).coerceAtLeast(0)
                            Button(
                                onClick = { catching = p.id },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.lanPlayColors.danger, contentColor = MaterialTheme.lanPlayColors.onDanger),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text("DHAPPA! Catch ${p.name} (${secs}s)") }
                        }
                    }
                }
            }
        }

        if (phase is Phase.RoundEnd || phase is Phase.Finished) {
            val finished = phase as? Phase.Finished
            SectionLabel(if (finished != null) "Series over" else "Round over")
            if (finished != null) Text("${state.player(finished.winnerId)?.name} wins the series!", style = MaterialTheme.typography.headlineMedium)
            if (isAdmin) {
                if (finished == null) Button(onClick = { client.send(Intent.NextRound) }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Next round", style = MaterialTheme.typography.titleMedium) }
                OutlinedButton(onClick = { client.send(Intent.BackToLobby) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Back to lobby") }
            } else Text("Waiting for the admin...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("Leave the game?") },
        text = { Text(if (isAdmin) "You are the host. Leaving ends the game for everyone." else "You can rejoin using the room ID until the next round starts.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; onLeave() }) { Text("Leave") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
    )
}

@Composable
private fun TurnsSection(
    client: GameClient, state: GameState, phase: Phase.Turns, now: Long, myId: String, iAmOut: Boolean,
    catching: String?, setCatching: (String?) -> Unit,
) {
    val current = state.player(phase.currentId)?.name ?: "Someone"
    val secs = ((phase.turnEndsAt - now) / 1000 + 1).coerceAtLeast(0)
    val myTurn = phase.currentId == myId && !iAmOut
    Text(if (myTurn) "Your turn  ·  ${secs}s" else "$current's turn  ·  ${secs}s", style = MaterialTheme.typography.titleLarge,
        color = if (myTurn) MaterialTheme.lanPlayColors.accentText else MaterialTheme.colorScheme.onSurface)

    if (catching != null) {
        // Catching: build the target from the OTHER player's cards.
        val victim = state.hands[catching]
        val name = state.player(catching)?.name ?: "player"
        val cards = victim?.hand.orEmpty()
        val builder = remember(catching, cards.map { it.id }) { BuilderState(cards.map { it.toBCard() }) }
        SectionLabel("Catch $name: build the target from THEIR cards")
        ExpressionBuilder(
            state = builder, engineHand = { builder.allCards().toEngineHand() }, target = state.targetValue ?: 0,
            onSubmit = { expr -> client.send(Intent.Catch(catching, expr)); setCatching(null) },
            submitLabel = "Catch!", penaltyMs = 0,
        )
        TextButton(onClick = { setCatching(null) }) { Text("Cancel") }
        return
    }

    val myCards = state.hands[myId]?.hand.orEmpty()
    val builder = remember(state.round) { BuilderState(myCards.map { it.toBCard() }) }
    LaunchedEffect(myCards.map { it.id }) {
        val have = builder.allCards().map { it.id }.toSet()
        myCards.filter { it.id !in have }.forEach { builder.addToHand(it.toBCard()) }
    }
    // While it's my turn, stream my row to everyone so they can watch me work. A short delay
    // (collectLatest + delay) avoids sending an update for every pixel of a drag.
    if (myTurn) LaunchedEffect(builder) {
        androidx.compose.runtime.snapshotFlow { builder.slots.map { RowSlot(it.card.glyph, it.exp?.glyph) } }
            .distinctUntilChanged()
            .collectLatest { row -> delay(120); client.send(Intent.Preview(row)) }
    }
    if (iAmOut) { Text("You are out of this round. Watch for DHAPPA chances!", color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    if (!myTurn) { Text("Plan your move. All cards are visible to everyone.", color = MaterialTheme.colorScheme.onSurfaceVariant); return }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(enabled = !phase.hasDrawn, onClick = { client.send(Intent.DrawCard(number = true)); Feedback.draw() }) { Text("Draw number") }
        OutlinedButton(enabled = !phase.hasDrawn, onClick = { client.send(Intent.DrawCard(number = false)); Feedback.draw() }) { Text("Draw operator") }
    }
    ExpressionBuilder(
        state = builder, engineHand = { builder.allCards().toEngineHand() }, target = state.targetValue ?: 0,
        onSubmit = { expr -> client.send(Intent.Submit(expr)) },
        penaltyMs = state.settings.penaltySeconds * 1000L, liveResult = state.settings.showLiveResult,
    )
    OutlinedButton(enabled = phase.hasDrawn, onClick = { client.send(Intent.Pass) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(if (phase.hasDrawn) "Pass" else "Draw a card to pass")
    }
}
