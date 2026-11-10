package com.bumbaiya.lanplay.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.game.combinemaths.Phase
import com.bumbaiya.lanplay.game.combinemaths.RowSlot
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.platform.Feedback
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.Confetti
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.pulse
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.BuilderStyle
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
import com.bumbaiya.lanplay.ui.game.WatchedRow
import com.bumbaiya.lanplay.ui.game.toBCard
import com.bumbaiya.lanplay.ui.game.toEngineHand
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged


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
    val iAmOut = state.hands[myId]?.out == true
    val sound by Session.settings.sound.collectAsState()

    // Beep and buzz when the turn becomes mine.
    val currentId = (phase as? Phase.Turns)?.currentId
    LaunchedEffect(currentId) {
        if (currentId == myId) { Feedback.turn(); if (Session.settings.haptics.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    LaunchedEffect(phase is Phase.Turns) { if (phase !is Phase.Turns) catching = null }
    // Scores at the start of the round, so the result screen can show "+3".
    val startScores = remember(state.round) { state.players.associate { it.id to it.score } }

    val victim = catching?.let { state.player(it) }
    when {
        phase is Phase.RoundEnd || phase is Phase.Finished ->
            ScoresView(client, state, myId, isAdmin, startScores, onLeave = { confirmLeave = true })
        victim != null && phase is Phase.Turns -> CatchView(client, state, victim.id, victim.name, now) { catching = null }
        else -> ScreenColumn(gap = 14.dp) {
            TopBar(state, myId, startScores, sound, onLeave = { confirmLeave = true }, onSound = { Session.settings.setSound(!sound) })
            TargetPanel(state, phase, now, myId)
            when (phase) {
                is Phase.DrawingTarget -> DrawingTargetControls(client, state, phase, myId)
                is Phase.Turns -> {
                    Opponents(state, phase, now, myId, iAmOut) { catching = it }
                    if (iAmOut) Text("You are out of this round. Watch for chances to catch!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else MyTurn(client, state, phase, myId)
                }
                else -> Unit
            }
        }
    }

    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Leave the game?") },
        text = { Text(if (isAdmin) "You are the host. Leaving ends the game for everyone." else "You can rejoin using the room ID until the next round starts.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; onLeave() }) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { com.bumbaiya.lanplay.ui.components.DIcon(com.bumbaiya.lanplay.ui.components.DI.Close, tint = MaterialTheme.lanPlayColors.danger, size = 18.dp); Text("Leave", color = MaterialTheme.lanPlayColors.danger, fontWeight = FontWeight.SemiBold) } } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { com.bumbaiya.lanplay.ui.components.DIcon(com.bumbaiya.lanplay.ui.components.DI.Check, tint = MaterialTheme.colorScheme.onSurface, size = 18.dp); Text("Stay", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) } } },
    )
}

// ---------- top bar ----------

@Composable
private fun TopBar(state: GameState, myId: String, startScores: Map<String, Int>, sound: Boolean, onLeave: () -> Unit, onSound: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.offset(x = (-10).dp).size(44.dp).clickable(onClick = onLeave), contentAlignment = Alignment.Center) { DIcon(DI.Back) }
        Column(Modifier.weight(1f)) {
            Text("Round ${state.round} · to ${state.settings.winScore}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.players.forEach { p ->
                    val shown by animateIntAsState(p.score, tween(700), label = "score")
                    Text(
                        "${if (p.id == myId) "You" else p.name} $shown", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = if (p.id == myId) MaterialTheme.lanPlayColors.accentText else MaterialTheme.colorScheme.onBackground, maxLines = 1,
                    )
                }
            }
        }
        Box(Modifier.size(44.dp).clickable(onClick = onSound), contentAlignment = Alignment.Center) { DIcon(if (sound) DI.Speaker else DI.SpeakerOff) }
    }
}

// ---------- target + timer ----------

@Composable
private fun TargetPanel(state: GameState, phase: Phase, now: Long, myId: String) {
    val shown = (phase as? Phase.DrawingTarget)?.digits ?: state.target
    Panel(radius = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Target")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (shown.isEmpty()) Text("Not drawn yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    shown.forEach { DeckCard(it.toString(), CardKind.Number, size = DeckCardSize.Target) }
                }
            }
            if (phase is Phase.Turns) {
                val myTurn = phase.currentId == myId
                val total = state.settings.turnSeconds.coerceAtLeast(1)
                val secs = ((phase.turnEndsAt - now) / 1000 + 1).toInt().coerceIn(0, total)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimerRing(secs, total, MaterialTheme.lanPlayColors.accentText.takeIf { myTurn } ?: MaterialTheme.colorScheme.onSurfaceVariant, 56.dp)
                    Text(
                        if (myTurn) "Your turn" else (state.player(phase.currentId)?.name ?: "") + "'s turn", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.lanPlayColors.accentText.takeIf { myTurn } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = if (myTurn) Modifier.pulse() else Modifier, maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A circle that drains as time runs out, with the seconds left in the middle. */
@Composable
private fun TimerRing(secs: Int, total: Int, color: Color, size: Dp) {
    val progress by animateFloatAsState(secs.toFloat() / total, tween(900), label = "ring")
    val track = MaterialTheme.lanPlayColors.cardBorder
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = 5.dp.toPx()
            val inset = w / 2
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            drawArc(color, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text("$secs", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

// ---------- drawing the target ----------

@Composable
private fun DrawingTargetControls(client: GameClient, state: GameState, phase: Phase.DrawingTarget, myId: String) {
    val drawer = state.player(phase.drawerId)?.name ?: "Someone"
    if (phase.drawerId == myId) {
        Text("You draw the target. Stop whenever you like: 1, 2 or 3 cards.", fontSize = 15.sp)
        BigButton("Draw a target card", { client.send(Intent.DrawTargetCard) }, icon = DI.DrawCard, primary = false, enabled = phase.digits.size < 3, height = 52.dp)
        BigButton("Stop here", { client.send(Intent.StopTarget) }, icon = DI.Check, enabled = phase.digits.isNotEmpty(), height = 52.dp)
    } else Text("$drawer is drawing the target...", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.pulse())
}

// ---------- other players ----------

@Composable
private fun Opponents(state: GameState, phase: Phase.Turns, now: Long, myId: String, iAmOut: Boolean, onCatch: (String) -> Unit) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.players.filter { it.id != myId }.forEach { p ->
            val rp = state.hands[p.id]
            val turn = phase.currentId == p.id
            val window = state.catchWindows.firstOrNull { it.playerId == p.id && it.closesAt >= now }
            val out = rp?.out == true
            Column {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (turn) 2.dp else 1.dp, if (turn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(46.dp), maxLines = 1, color = if (out || !p.connected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        val cards = rp?.hand.orEmpty()
                        val open = expandedId == p.id
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            (if (open) cards else cards.take(5)).forEach { c -> val b = c.toBCard(); DeckCard(b.glyph, b.kind, size = DeckCardSize.Small, dimmed = out) }
                            if (cards.size > 5) Text(
                                if (open) "Show less" else "+${cards.size - 5} more", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.lanPlayColors.accentText,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expandedId = if (open) null else p.id }.padding(horizontal = 6.dp, vertical = 8.dp),
                            )
                        }
                        when {
                            window != null && !iAmOut -> Surface(
                                onClick = { onCatch(p.id) }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.lanPlayColors.dangerContainer,
                                modifier = Modifier.size(52.dp, 44.dp),
                            ) { Box(contentAlignment = Alignment.Center) { Text("Catch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.lanPlayColors.onDangerContainer) } }
                            !p.connected -> Text("Offline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            out -> Text("Out", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            turn -> Text("Playing", fontSize = 12.sp, color = MaterialTheme.lanPlayColors.accentText, modifier = Modifier.pulse())
                            else -> Text("Waiting", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (turn) Box(Modifier.padding(start = 10.dp, top = 6.dp)) { WatchedRow(state.liveRow, p.name) }
            }
        }
    }
}

// ---------- my turn ----------

@Composable
private fun MyTurn(client: GameClient, state: GameState, phase: Phase.Turns, myId: String) {
    val myTurn = phase.currentId == myId
    val myCards = state.hands[myId]?.hand.orEmpty()
    val builder = remember(state.round) { BuilderState(myCards.map { it.toBCard() }) }
    LaunchedEffect(myCards.map { it.id }) {
        val have = builder.allCards().map { it.id }.toSet()
        myCards.filter { it.id !in have }.forEach { builder.addToHand(it.toBCard()) }
    }
    // While it's my turn, stream my row to everyone so they can watch me work. A short delay
    // (collectLatest + delay) avoids sending an update for every pixel of a drag.
    if (myTurn) LaunchedEffect(builder) {
        snapshotFlow { builder.slots.map { RowSlot(it.card.glyph, it.exp?.glyph) } }
            .distinctUntilChanged()
            .collectLatest { row -> delay(120); client.send(Intent.Preview(row)) }
    }
    ExpressionBuilder(
        state = builder, engineHand = { builder.allCards().toEngineHand() }, target = state.targetValue ?: 0,
        onSubmit = { expr -> client.send(Intent.Submit(expr)) },
        penaltyMs = state.settings.penaltySeconds * 1000L, liveResult = state.settings.showLiveResult,
        enabled = myTurn,
        actions = { submit, _, locked ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrawButton("Draw number", MaterialTheme.lanPlayColors.numberLabel, !phase.hasDrawn && myTurn, Modifier.weight(1f)) { client.send(Intent.DrawCard(number = true)); Feedback.draw() }
                    DrawButton("Draw operator", MaterialTheme.lanPlayColors.operatorLabel, !phase.hasDrawn && myTurn, Modifier.weight(1f)) { client.send(Intent.DrawCard(number = false)); Feedback.draw() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Pass", { client.send(Intent.Pass) }, Modifier.weight(1f), icon = DI.Pass, primary = false, enabled = myTurn && phase.hasDrawn, height = 52.dp)
                    BigButton(if (locked && myTurn) "Wait..." else "Submit answer", submit, Modifier.weight(2f), icon = DI.Check, enabled = myTurn && !locked, height = 52.dp)
                }
                if (myTurn && !phase.hasDrawn) Text("Draw a card to be allowed to pass.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun DrawButton(text: String, color: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(14.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val c = if (enabled) color else MaterialTheme.lanPlayColors.onDisabledContainer
            DIcon(DI.DrawCard, tint = c, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c, maxLines = 1)
        }
    }
}

// ---------- catching ----------

@Composable
private fun CatchView(client: GameClient, state: GameState, victimId: String, name: String, now: Long, onCancel: () -> Unit) {
    val window = state.catchWindows.firstOrNull { it.playerId == victimId }
    val secs = if (window == null) 0 else ((window.closesAt - now) / 1000 + 1).toInt().coerceAtLeast(0)
    val total = state.settings.catchWindowSeconds.coerceAtLeast(1)
    val target = state.targetValue ?: 0
    val cards = state.hands[victimId]?.hand.orEmpty()
    val builder = remember(victimId, cards.map { it.id }) { BuilderState(cards.map { it.toBCard() }) }
    // The window can close while I'm building: go back to the table.
    LaunchedEffect(window == null) { if (window == null) onCancel() }

    ScreenColumn(gap = 16.dp) {
        Header("Catch $name", onBack = onCancel)
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.lanPlayColors.dangerContainer, RoundedCornerShape(16.dp)).border(1.dp, MaterialTheme.lanPlayColors.danger.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("$name passed on $target", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Catch window closes in $secs s", fontSize = 14.sp, color = MaterialTheme.lanPlayColors.onDangerContainer)
            }
            TimerRing(secs, total, MaterialTheme.lanPlayColors.danger, 56.dp)
        }
        ExpressionBuilder(
            state = builder, engineHand = { builder.allCards().toEngineHand() }, target = target,
            onSubmit = { expr -> client.send(Intent.Catch(victimId, expr)); onCancel() },
            penaltyMs = 0, style = BuilderStyle.Catch,
            cardsTitle = "$name's cards",
            cardsHint = "You can only catch after $name's turn is over. Build the target from these cards to prove they missed it.",
            proofHint = "Arrange $name's cards to reach $target. The result isn't calculated for you; the host checks it when you call DHAPPA.",
            actions = { submit, _, _ ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("DHAPPA!", submit, icon = DI.Torch, container = MaterialTheme.lanPlayColors.danger, content = MaterialTheme.lanPlayColors.onDanger, height = 56.dp, fontSize = 20)
                    Text("A valid catch puts $name out of the round and gives you 3 points.", fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                }
            },
        )
    }
}

// ---------- scores ----------

@Composable
private fun ScoresView(client: GameClient, state: GameState, myId: String, isAdmin: Boolean, startScores: Map<String, Int>, onLeave: () -> Unit) {
    val finished = state.phase as? Phase.Finished
    val reason = (state.phase as? Phase.RoundEnd)?.reason ?: state.notice
    val winnerId = finished?.winnerId ?: (state.phase as? Phase.RoundEnd)?.winnerId
    val caughtIds = state.hands.filter { it.value.out }.keys
    val title = when {
        finished != null -> if (finished.winnerId == myId) "You win the series!" else "${state.player(finished.winnerId)?.name} wins!"
        winnerId == null -> "Nobody got it"
        winnerId == myId -> "You got it!"
        else -> "${state.player(winnerId)?.name} got it"
    }
    val leaderScore = state.players.maxOfOrNull { it.score } ?: 0
    val ordered = state.players.sortedByDescending { it.score }

    ScreenColumn(gap = 16.dp) {
        if (winnerId == myId) Box(Modifier.height(0.dp)) { Confetti(Modifier.fillMaxWidth().requiredHeight(160.dp)) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel(if (finished != null) "Series complete" else "Round ${state.round} complete")
            Text(title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Text(reason, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionLabel("Series · first to ${state.settings.winScore}")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ordered.forEachIndexed { i, p ->
                val gain = p.score - (startScores[p.id] ?: p.score)
                val caught = p.id in caughtIds
                val leader = p.score == leaderScore && leaderScore > 0
                val shown by animateIntAsState(p.score, tween(900), label = "score")
                val fill by animateFloatAsState((p.score.toFloat() / state.settings.winScore).coerceIn(0f, 1f), tween(900), label = "bar")
                Surface(
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (leader) 2.dp else 1.dp, if (leader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. ${if (p.id == myId) "You" else p.name}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            if (caught) Text("  · out", fontSize = 13.sp, color = MaterialTheme.lanPlayColors.onDangerContainer)
                            Spacer(Modifier.weight(1f))
                            if (gain > 0) Text("+$gain  ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.lanPlayColors.accentText)
                            Text("$shown", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        val barColor = when { caught -> MaterialTheme.lanPlayColors.danger; leader -> MaterialTheme.colorScheme.primary; else -> Color(0xFF4DA3FF) }
                        Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.lanPlayColors.cardBorder, RoundedCornerShape(4.dp))) {
                            Box(Modifier.fillMaxWidth(fill).height(8.dp).background(barColor, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
        Panel {
            Text("How points work", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Finish order scores 5, 3, 2, 1, 0. A correct catch always earns 3. First to ${state.settings.winScore} wins.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isAdmin) {
            if (finished == null) BigButton("Next round", { client.send(Intent.NextRound) }, icon = DI.Right)
            BigButton("Back to lobby", { client.send(Intent.BackToLobby) }, icon = DI.Back, primary = finished != null, height = 48.dp, fontSize = 16)
        } else Text("Waiting for the host...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.pulse())
        BigButton("Leave room", onLeave, icon = DI.Leave, primary = false, height = 48.dp, fontSize = 16)
    }
}
