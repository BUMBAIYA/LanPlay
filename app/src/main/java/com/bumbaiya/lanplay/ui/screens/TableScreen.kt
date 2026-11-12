package com.bumbaiya.lanplay.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.bumbaiya.lanplay.ui.components.LockLandscape
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
    LockLandscape()
    // The table is always dark and colourful, whatever theme the rest of the app uses.
    com.bumbaiya.lanplay.ui.theme.LanPlayTheme(com.bumbaiya.lanplay.ui.theme.ThemeMode.Dark) { TableContent(client, state, onLeave) }
}

@Composable
private fun TableContent(client: GameClient, state: GameState, onLeave: () -> Unit) {
    val me by client.myId.collectAsState()
    val myId = me ?: return
    val phase = state.phase
    // The clock only changes once per second as far as the screen is concerned. Rounding down means the
    // value is identical for 4 of every 5 checks, and Compose skips redrawing when a state doesn't change.
    val now by produceState((client.serverNow() / 1000) * 1000) { while (true) { value = (client.serverNow() / 1000) * 1000; delay(200) } }
    var confirmLeave by remember { mutableStateOf(false) }
    var catching by remember { mutableStateOf<String?>(null) }
    var shownId by remember { mutableStateOf<String?>(null) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val isAdmin = myId == state.adminId
    val iAmOut = state.hands[myId]?.out == true
    val sound by Session.settings.sound.collectAsState()

    // Beep and buzz when the turn becomes mine.
    val currentId = (phase as? Phase.Turns)?.currentId
    LaunchedEffect(currentId) {
        if (currentId == myId) { Feedback.turn(); Feedback.buzz(appContext) }
    }
    LaunchedEffect(phase is Phase.Turns) { if (phase !is Phase.Turns) catching = null }
    // Scores at the start of the round, so the result screen can show "+3".
    val startScores = remember(state.round) { state.players.associate { it.id to it.score } }

    // A catch makes the whole table shake and slams a DHAPPA stamp onto the screen.
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    var stamp by remember { mutableStateOf(false) }
    LaunchedEffect(state.seq) {
        if (state.notice.contains("DHAPPA")) {
            stamp = true
            for (x in listOf(18f, -16f, 12f, -9f, 5f, 0f)) shake.animateTo(x, tween(55))
            delay(1100); stamp = false
        }
    }

    val victim = catching?.let { state.player(it) }
    Arena(Modifier.graphicsLayer { translationX = shake.value }) {
        when {
            phase is Phase.RoundEnd || phase is Phase.Finished ->
                ScoresView(client, state, myId, isAdmin, startScores, onLeave = { confirmLeave = true })
            victim != null && phase is Phase.Turns -> CatchView(client, state, victim.id, victim.name, now) { catching = null }
            else -> Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val others = state.players.filter { it.id != myId }
                OpponentRail(state, currentId, now, myId, iAmOut, compact = others.size >= 4, onCatch = { catching = it }, onShow = { shownId = it }, modifier = Modifier.width(if (others.size >= 4) 150.dp else 230.dp))
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeaderRow(state, phase, now, myId, startScores, sound, onLeave = { confirmLeave = true }, onSound = { Session.settings.setSound(!sound) })
                    when (phase) {
                        is Phase.DrawingTarget -> DrawingTargetControls(client, state, phase, myId)
                        is Phase.Turns -> {
                            if (phase.currentId != myId) WatchedRow(state.liveRow, state.player(phase.currentId)?.name ?: "Player")
                            if (iAmOut) Text("You are out of this round. Watch for chances to catch!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else MyTurn(client, state, phase, myId)
                        }
                        else -> Unit
                    }
                }
            }
        }
        if (stamp) DhappaStamp()
    }

    shownId?.let { id ->
        val p = state.player(id)
        if (p != null) CardsDialog(p.name, state.hands[id]?.hand.orEmpty().map { it.toBCard() }, out = state.hands[id]?.out == true) { shownId = null }
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

/** The colourful dark play area every table screen sits on. */
@Composable
private fun Arena(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF3B2A8C), Color(0xFF1A1450), Color(0xFF0D0A2C)), center = Offset(900f, 500f), radius = 1600f))
            .drawBehind {
                drawCircle(Color(0xFFFF5C7A).copy(alpha = 0.14f), size.minDimension * 0.55f, Offset(0f, size.height * 0.85f))
                drawCircle(Color(0xFF4DA3FF).copy(alpha = 0.14f), size.minDimension * 0.5f, Offset(size.width, 0f))
                drawCircle(Color(0xFFB4F24D).copy(alpha = 0.10f), size.minDimension * 0.35f, Offset(size.width * 0.7f, size.height))
            },
    ) { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color.White) { content() } }
}

private val Animals = listOf("\uD83E\uDD8A", "\uD83D\uDC3C", "\uD83D\uDC38", "\uD83E\uDD81", "\uD83D\uDC19", "\uD83D\uDC2F", "\uD83D\uDC35", "\uD83E\uDD84")
private val AvatarColors = listOf(Color(0xFFFF7A7A), Color(0xFF4DE3B0), Color(0xFF7A8BFF), Color(0xFFFFB04A), Color(0xFFC58BFF), Color(0xFF5FD3FF), Color(0xFFFF8BD1), Color(0xFFA6EE5C))

/** A round avatar: an animal on a bright background, stable per seat. */
@Composable
private fun Avatar(index: Int, size: Dp, ring: Boolean = false) {
    val i = index.coerceAtLeast(0) % Animals.size
    Box(
        Modifier.size(size).background(AvatarColors[i], androidx.compose.foundation.shape.CircleShape)
            .border(if (ring) 3.dp else 2.dp, if (ring) Color(0xFFB6F24A) else Color.White, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(Animals[i], fontSize = (size.value * 0.5f).sp) }
}

/** The big red DHAPPA stamp that slams onto the screen when someone is caught. */
@Composable
private fun DhappaStamp() {
    val scale = remember { androidx.compose.animation.core.Animatable(3.2f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 500f)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "DHAPPA!", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 72.sp, color = Color(0xFFFF5C7A),
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF7A1230), Offset(0f, 10f), 0f)),
            modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = -8f; alpha = (4f - scale.value).coerceIn(0f, 1f) },
        )
    }
}

/** Everything one player is holding, full size. */
@Composable
private fun CardsDialog(name: String, cards: List<com.bumbaiya.lanplay.ui.game.BCard>, out: Boolean, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1450), border = BorderStroke(3.dp, Color.White), modifier = Modifier.clickable(onClick = onClose)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$name's cards" + if (out) " (out)" else "", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cards.forEach { c -> DeckCard(c.glyph, c.kind, altGlyph = c.alt, size = DeckCardSize.Large, dimmed = out) }
                }
                Text("Tap anywhere to close", fontSize = 12.sp, color = Color(0xFF9AA3B2))
            }
        }
    }
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

// ---------- header: scores, target and timer in one row ----------

@Composable
private fun HeaderRow(state: GameState, phase: Phase, now: Long, myId: String, startScores: Map<String, Int>, sound: Boolean, onLeave: () -> Unit, onSound: () -> Unit) {
    val shown = (phase as? Phase.DrawingTarget)?.digits ?: state.target
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clickable(onClick = onLeave), contentAlignment = Alignment.Center) { DIcon(DI.Back) }
        Column(Modifier.weight(1f)) {
            Text("Round ${state.round} · to ${state.settings.winScore}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.players.forEach { p ->
                    val score by animateIntAsState(p.score, tween(700), label = "score")
                    Text(
                        "${if (p.id == myId) "You" else p.name} $score", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (p.id == myId) Color(0xFFB6F24A) else Color.White, maxLines = 1,
                    )
                }
            }
        }
        // target
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TARGET", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFFB6F24A))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
                if (shown.isEmpty()) Text("?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                shown.forEachIndexed { i, d -> androidx.compose.runtime.key(i) { FlipIn { DeckCard(d.toString(), CardKind.Number, size = DeckCardSize.Target) } } }
            }
        }
        if (phase is Phase.Turns) {
            val myTurn = phase.currentId == myId
            val total = state.settings.turnSeconds.coerceAtLeast(1)
            val secs = ((phase.turnEndsAt - now) / 1000 + 1).toInt().coerceIn(0, total)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimerRing(secs, total, if (myTurn) Color(0xFFB6F24A) else Color(0xFF9AA3B2), 52.dp)
                Text(
                    if (myTurn) "YOUR TURN" else (state.player(phase.currentId)?.name ?: "") + "'s turn", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = if (myTurn) Color(0xFFB6F24A) else Color(0xFF9AA3B2), modifier = if (myTurn) Modifier.pulse() else Modifier, maxLines = 1,
                )
            }
        }
        Box(Modifier.size(40.dp).clickable(onClick = onSound), contentAlignment = Alignment.Center) { DIcon(if (sound) DI.Speaker else DI.SpeakerOff) }
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
                    shown.forEachIndexed { i, d -> androidx.compose.runtime.key(i) { FlipIn { DeckCard(d.toString(), CardKind.Number, size = DeckCardSize.Target) } } }
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

/** Flips its content over from edge-on, like a card being turned face-up. */
@Composable
private fun FlipIn(content: @Composable () -> Unit) {
    val t = remember { androidx.compose.animation.core.Animatable(90f) }
    LaunchedEffect(Unit) { t.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 260f)) }
    Box(Modifier.graphicsLayer { rotationY = t.value; cameraDistance = 14f * density }) { content() }
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

/**
 * The other players down the left edge. With up to 3 opponents each shows its cards inline; with 4 or more
 * they shrink to avatar bubbles with a card count. Tap anyone to see all their cards at full size.
 */
@Composable
private fun OpponentRail(
    state: GameState, currentId: String?, now: Long, myId: String, iAmOut: Boolean, compact: Boolean,
    onCatch: (String) -> Unit, onShow: (String) -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.players.forEachIndexed { index, p ->
            if (p.id == myId) return@forEachIndexed
            val rp = state.hands[p.id]
            val turn = currentId == p.id
            val window = state.catchWindows.firstOrNull { it.playerId == p.id && it.closesAt >= now }
            val out = rp?.out == true
            val cards = rp?.hand.orEmpty()
            Surface(
                onClick = { onShow(p.id) }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(if (turn) 3.dp else 1.5.dp, if (turn) Color(0xFFB6F24A) else Color.White.copy(alpha = 0.25f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            Avatar(index, if (compact) 40.dp else 36.dp, ring = turn)
                            Text(
                                "${cards.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10131C),
                                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape).padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = if (out || !p.connected) Color(0xFF9AA3B2) else Color.White)
                            Text(
                                when { !p.connected -> "Offline"; out -> "Out"; turn -> "Playing"; else -> "${p.score} pts" },
                                fontSize = 12.sp, color = if (turn) Color(0xFFB6F24A) else Color(0xFF9AA3B2), modifier = if (turn) Modifier.pulse() else Modifier,
                            )
                        }
                        if (window != null && !iAmOut) Surface(onClick = { onCatch(p.id) }, shape = RoundedCornerShape(10.dp), color = Color(0xFFFF5C7A), border = BorderStroke(2.dp, Color.White)) {
                            Text("DHAPPA?", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                        }
                    }
                    if (!compact) {
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            cards.take(5).forEach { c -> val b = c.toBCard(); DeckCard(b.glyph, b.kind, size = DeckCardSize.Small, dimmed = out) }
                            if (cards.size > 5) Text("+${cards.size - 5}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB6F24A), modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 4.dp))
                        }
                    }
                }
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DrawButton("Draw number", MaterialTheme.lanPlayColors.numberLabel, !phase.hasDrawn && myTurn, Modifier.weight(1f)) { client.send(Intent.DrawCard(number = true)); Feedback.draw() }
                    DrawButton("Draw operator", MaterialTheme.lanPlayColors.operatorLabel, !phase.hasDrawn && myTurn, Modifier.weight(1f)) { client.send(Intent.DrawCard(number = false)); Feedback.draw() }
                    BigButton("Pass", { client.send(Intent.Pass) }, Modifier.weight(0.7f), icon = DI.Pass, primary = false, enabled = myTurn && phase.hasDrawn, height = 48.dp, fontSize = 15)
                    BigButton(if (locked && myTurn) "Wait..." else "Submit", submit, Modifier.weight(1.2f), icon = DI.Check, enabled = myTurn && !locked, height = 48.dp, fontSize = 16)
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
