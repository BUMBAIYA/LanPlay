package com.bumbaiya.lanplay.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.bumbaiya.lanplay.ui.components.*
import com.bumbaiya.lanplay.ui.game.*
import com.bumbaiya.lanplay.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TableScreen(client: GameClient, state: GameState, onLeave: () -> Unit) {
    val me by client.myId.collectAsState()
    val myId = me ?: return
    val phase = state.phase
    // The clock only changes once per second as far as the screen is concerned.
    val now by produceState((client.serverNow() / 1000) * 1000) { while (true) { value = (client.serverNow() / 1000) * 1000; delay(200.milliseconds) } }
    var confirmLeave by remember { mutableStateOf(false) }
    var showStandings by remember { mutableStateOf(false) }
    var catching by remember { mutableStateOf<String?>(null) }
    var shownId by remember { mutableStateOf<String?>(null) }
    val appContext = LocalContext.current.applicationContext
    val isAdmin = myId == state.adminId
    val iAmOut = state.hands[myId]?.out == true
    val sound by Session.settings.sound.collectAsState()

    // Beep and buzz when the turn becomes mine.
    val currentId = (phase as? Phase.Turns)?.currentId
    LaunchedEffect(currentId) { if (currentId == myId) { Feedback.turn(); Feedback.buzz(appContext) } }
    LaunchedEffect(phase is Phase.Turns) { if (phase !is Phase.Turns) catching = null }
    // Scores at the start of the round, so the result screen can show "+3".
    val startScores = remember(state.round) { state.players.associate { it.id to it.score } }

    // A catch makes the whole table shake and slams a DHAPPA stamp onto the screen.
    val shake = remember { Animatable(0f) }
    var stamp by remember { mutableStateOf(false) }
    LaunchedEffect(state.seq) {
        if (state.notice.contains("DHAPPA")) {
            stamp = true
            for (x in listOf(18f, -16f, 12f, -9f, 5f, 0f)) shake.animateTo(x, tween(55))
            delay(1100.milliseconds); stamp = false
        }
    }

    val victim = catching?.let { state.player(it) }
    Box(Modifier.fillMaxSize().graphicsLayer { translationX = shake.value }) {
        Frame(cards = true) {
            when {
                phase is Phase.RoundEnd || phase is Phase.Finished -> ScoresView(client, state, myId, isAdmin, startScores, onLeave = { confirmLeave = true })
                victim != null && phase is Phase.Turns -> CatchView(client, state, victim.id, victim.name, now) { catching = null }
                else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TableHeader(state, phase, now, myId, sound, onLeave = { confirmLeave = true }, onSound = { Session.settings.setSound(!sound) }, onStandings = { showStandings = true })
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        OpponentRail(state, currentId, now, myId, iAmOut, shownId, onCatch = { catching = it }, onShow = { shownId = if (shownId == it) null else it }, modifier = Modifier.width(150.dp))
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val peek = shownId?.let { state.player(it) }
                            when {
                                peek != null -> PeekPanel(peek.name, state.players.indexOf(peek), peek.score, state.hands[peek.id]?.hand.orEmpty().map { it.toBCard() }, state.hands[peek.id]?.out == true) { shownId = null }
                                phase is Phase.DrawingTarget -> DrawingTargetControls(client, state, phase, myId)
                                phase is Phase.Turns -> if (iAmOut) OutView(state, phase, now) else MyTurn(client, state, phase, myId, now)
                            }
                        }
                    }
                }
            }
        }
        if (stamp) DhappaStamp()
    }

    if (showStandings) StandingsModal(state, myId) { showStandings = false }
    if (confirmLeave) ModalCard({ confirmLeave = false }) {
        Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Leave the game?", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
            Text(if (isAdmin) "You are the host. Leaving ends the game for everyone." else "You can rejoin using the room ID until the next round starts.", fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, color = Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                ChunkButton("Stay", { confirmLeave = false }, fill = Sand, icon = DI.Check, height = 42.dp)
                ChunkButton("Leave", { confirmLeave = false; onLeave() }, fill = Rose, icon = DI.Close, height = 42.dp)
            }
        }
    }
}

/** A stamp that lands on the screen when someone is caught. */
@Composable
private fun DhappaStamp() {
    val scale = remember { Animatable(2.2f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 600f)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "DHAPPA!", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 46.sp, color = Rose,
            modifier = Modifier
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = -6f; alpha = (3.2f - scale.value).coerceIn(0f, 1f) }
                .background(PanelBg, RoundedCornerShape(14.dp)).border(3.dp, Ink, RoundedCornerShape(14.dp))
                .padding(horizontal = 24.dp, vertical = 6.dp),
        )
    }
}

// ---------- header ----------

@Composable
private fun TableHeader(state: GameState, phase: Phase, now: Long, myId: String, sound: Boolean, onLeave: () -> Unit, onSound: () -> Unit, onStandings: () -> Unit) {
    val shown = (phase as? Phase.DrawingTarget)?.digits ?: state.target
    val leader = state.players.maxByOrNull { it.score }
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SquareButton(DI.Back, onLeave)
        Text("Round ${state.round} · to ${state.settings.winScore}", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink, maxLines = 1, softWrap = false)
        Chunk(Modifier.height(40.dp), onClick = onStandings, fill = Amber, radius = 12.dp, border = 2.dp, shadow = 3.dp, contentAlignment = Alignment.Center) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                DIcon(DI.Trophy, tint = Ink, size = 16.dp)
                Text("Standings", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                if (leader != null && leader.score > 0) Text("${if (leader.id == myId) "You lead" else "${leader.name.take(8)} leads"} · ${leader.score}", fontFamily = Inter, fontSize = 10.sp, color = Ink.copy(alpha = 0.75f), maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        CapsLabel("Target")
        if (shown.isEmpty()) Text("?", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Muted)
        else key(shown.joinToString("")) { FlipIn { TargetBox(shown.joinToString("")) } }
        if (phase is Phase.Turns) {
            val myTurn = phase.currentId == myId
            val total = state.settings.turnSeconds.coerceAtLeast(1)
            val secs = ((phase.turnEndsAt - now) / 1000 + 1).toInt().coerceIn(0, total)
            Chip(if (myTurn) "Your turn" else (state.player(phase.currentId)?.name ?: "") + "'s turn", fill = if (myTurn) Teal else Sand, modifier = if (myTurn) Modifier.pulse() else Modifier)
            TimerRing(secs, total, Rose, 40.dp)
        }
        SquareButton(if (sound) DI.Speaker else DI.SpeakerOff, onSound)
    }
}

/** A circle that drains as time runs out, with the seconds left in the middle. */
@Composable
fun TimerRing(secs: Int, total: Int, color: Color, size: Dp) {
    val progress by animateFloatAsState(secs.toFloat() / total, tween(900), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = 5.dp.toPx()
            val inset = w / 2
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(Sand2, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            drawArc(color, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text("$secs", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
    }
}

/** Flips its content over from edge-on, like a card being turned face-up. */
@Composable
fun FlipIn(content: @Composable () -> Unit) {
    val t = remember { Animatable(90f) }
    LaunchedEffect(Unit) { t.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
    Box(Modifier.graphicsLayer { rotationY = t.value; cameraDistance = 14f * density }) { content() }
}

// ---------- drawing the target ----------

@Composable
private fun ColumnScope.DrawingTargetControls(client: GameClient, state: GameState, phase: Phase.DrawingTarget, myId: String) {
    val drawer = state.player(phase.drawerId)?.name ?: "Someone"
    Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
        if (phase.drawerId == myId) {
            Text("You draw the target", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Text("Stop whenever you like: 1, 2 or 3 cards.", fontFamily = Inter, fontSize = 12.sp, color = Muted)
        } else {
            Text("$drawer is drawing the target…", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink, modifier = Modifier.pulse())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (phase.digits.isNotEmpty()) key(phase.digits.joinToString("")) { FlipIn { TargetBox(phase.digits.joinToString(""), scale = 1.5f) } }
            if (phase.digits.size < 3) Box(Modifier.size(68.dp, 70.dp).drawBehind { drawRoundRect(Muted, cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f)))) })
        }
        if (phase.drawerId == myId) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkButton("Draw a target card", { client.send(Intent.DrawTargetCard) }, fill = Teal, icon = DI.DrawCard, enabled = phase.digits.size < 3)
            ChunkButton("Stop here", { client.send(Intent.StopTarget) }, icon = DI.Check, enabled = phase.digits.isNotEmpty())
        }
    }
}

// ---------- other players ----------

@Composable
private fun OpponentRail(
    state: GameState, currentId: String?, now: Long, myId: String, iAmOut: Boolean, shownId: String?,
    onCatch: (String) -> Unit, onShow: (String) -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.players.forEachIndexed { index, p ->
            if (p.id == myId) return@forEachIndexed
            val rp = state.hands[p.id]
            val turn = currentId == p.id
            val window = state.catchWindows.firstOrNull { it.playerId == p.id && it.closesAt >= now }
            val out = rp?.out == true
            Chunk(Modifier.fillMaxWidth().weight(1f), onClick = { onShow(p.id) }, fill = if (turn || shownId == p.id) AmberSoft else PanelBg, radius = 12.dp, border = 2.dp, shadow = 3.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Avatar(p.name, PlayerColors[index % PlayerColors.size], 26.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (out || !p.connected) Muted else Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when { !p.connected -> "Offline"; out -> "Out"; else -> "${rp?.hand?.size ?: 0} cards · ${p.score} pts" },
                            fontFamily = Inter, fontSize = 9.sp, color = Muted, maxLines = 1,
                        )
                    }
                    if (window != null && !iAmOut) Chunk(Modifier.height(26.dp), onClick = { onCatch(p.id) }, fill = Rose, radius = 8.dp, border = 1.5.dp, shadow = 2.dp, contentAlignment = Alignment.Center) {
                        Text("Catch", Modifier.padding(horizontal = 8.dp), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Ink, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Everything one player is holding, full size, in place of the table. */
@Composable
private fun ColumnScope.PeekPanel(name: String, index: Int, score: Int, cards: List<BCard>, out: Boolean, onClose: () -> Unit) {
    Panel(Modifier.weight(1f).fillMaxWidth(), padding = 14.dp) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(name, PlayerColors[index.coerceAtLeast(0) % PlayerColors.size], 34.dp)
                Text("$name's cards" + if (out) " (out)" else "", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink, maxLines = 1)
                Chip("Peeking", icon = DI.Eye)
                Spacer(Modifier.weight(1f))
                Chip("$score pts", fill = Amber)
                ChunkButton("Close", onClose, fill = Sand, height = 32.dp, fontSize = 12.sp, padding = 14.dp)
            }
            CapsLabel("In hand")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val fit = fitScale(cards.size, DeckCardSize.Large.width, DeckCardSize.Large.height, 8.dp, maxWidth, maxHeight, maxScale = 1.2f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cards.forEach { c -> DeckCard(c.glyph, c.kind, altGlyph = c.alt, size = DeckCardSize.Large, scale = fit, dimmed = out) }
                }
            }
            Text("Tap any player on the left to see their cards", Modifier.fillMaxWidth(), fontFamily = Inter, fontSize = 10.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ---------- my turn / watching ----------

@Composable
private fun ColumnScope.OutView(state: GameState, phase: Phase.Turns, now: Long) {
    if (phase.currentId.isNotEmpty()) WatchedRow(state.liveRow, state.player(phase.currentId)?.name ?: "Player")
    Text("You are out of this round. Watch for chances to catch!", fontFamily = Inter, fontSize = 12.sp, color = Muted)
}

@Composable
private fun ColumnScope.MyTurn(client: GameClient, state: GameState, phase: Phase.Turns, myId: String, now: Long) {
    val myTurn = phase.currentId == myId
    val myCards = state.hands[myId]?.hand.orEmpty()

    if (!myTurn) {
        // Watching: a banner when someone has just passed, then their row, then my cards, dimmed.
        val window = state.catchWindows.firstOrNull { it.closesAt >= now }
        if (window != null) {
            val who = state.player(window.playerId)?.name ?: "Someone"
            Panel(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$who passed on ${state.targetValue ?: ""}", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                    Text("Think they missed it? Catch them.", Modifier.weight(1f), fontFamily = Inter, fontSize = 11.sp, color = Muted, maxLines = 1)
                    val secs = ((window.closesAt - now) / 1000 + 1).toInt().coerceAtLeast(0)
                    TimerRing(secs, state.settings.catchWindowSeconds.coerceAtLeast(1), Rose, 38.dp)
                }
            }
        }
        WatchedRow(state.liveRow, state.player(phase.currentId)?.name ?: "Player")
        CapsLabel("Your cards", color = Muted.copy(alpha = 0.7f))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val fit = fitScale(myCards.size.coerceAtLeast(1), DeckCardSize.Medium.width, DeckCardSize.Medium.height, 8.dp, maxWidth, maxHeight, maxScale = 1.2f)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                myCards.forEach { c -> val b = c.toBCard(); DeckCard(b.glyph, b.kind, altGlyph = b.alt, size = DeckCardSize.Medium, scale = fit, dimmed = true) }
            }
        }
        return
    }

    val builder = remember(state.round) { BuilderState(myCards.map { it.toBCard() }) }
    LaunchedEffect(myCards.map { it.id }) {
        val have = builder.allCards().map { it.id }.toSet()
        myCards.filter { it.id !in have }.forEach { builder.addToHand(it.toBCard()) }
    }
    // While it's my turn, stream my row to everyone so they can watch me work.
    LaunchedEffect(builder) {
        snapshotFlow { builder.slots.map { RowSlot(it.card.glyph, it.exp?.glyph) } }
            .distinctUntilChanged()
            .collectLatest { row -> delay(120.milliseconds); client.send(Intent.Preview(row)) }
    }
    ExpressionBuilder(
        state = builder, engineHand = { builder.allCards().toEngineHand() }, target = state.targetValue ?: 0,
        onSubmit = { expr -> client.send(Intent.Submit(expr)) },
        penaltyMs = state.settings.penaltySeconds * 1000L, liveResult = state.settings.showLiveResult,
        modifier = Modifier.weight(1f), enabled = true, actionsHeight = 54.dp,
        actions = { submit, _, locked ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (!phase.hasDrawn) Text("Draw a card to be allowed to pass.", fontFamily = Inter, fontSize = 10.sp, color = Muted, modifier = Modifier.weight(1f))
                else Spacer(Modifier.weight(1f))
                ChunkButton("Draw number", { client.send(Intent.DrawCard(number = true)); Feedback.draw() }, fill = Teal, icon = DI.DrawCard, enabled = !phase.hasDrawn, fontSize = 13.sp, padding = 14.dp)
                ChunkButton("Draw operator", { client.send(Intent.DrawCard(number = false)); Feedback.draw() }, fill = Rose, icon = DI.DrawCard, enabled = !phase.hasDrawn, fontSize = 13.sp, padding = 14.dp)
                if (phase.hasDrawn) ChunkButton("Pass", { client.send(Intent.Pass) }, fill = Sand, icon = DI.Pass, fontSize = 13.sp, padding = 14.dp)
                ChunkButton(if (locked) "Wait…" else "Submit", submit, icon = DI.Check, enabled = !locked, fontSize = 13.sp, padding = 14.dp)
            }
        },
    )
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

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Header("Catch $name", onCancel, trailing = {
            Chip("closes in $secs s", fill = Rose)
            TimerRing(secs, total, Rose, 40.dp)
        })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$name passed on $target. Build it from their cards to prove they missed it.", Modifier.weight(1f), fontFamily = Inter, fontSize = 11.sp, color = Muted, maxLines = 2)
            CapsLabel("Target"); Spacer(Modifier.width(8.dp)); TargetBox(target.toString())
        }
        ExpressionBuilder(
            state = builder, engineHand = { builder.allCards().toEngineHand() }, target = target,
            modifier = Modifier.weight(1f),
            onSubmit = { expr -> client.send(Intent.Catch(victimId, expr)); onCancel() },
            penaltyMs = 0, style = BuilderStyle.Catch, handLabel = "$name's cards", actionsHeight = 54.dp,
            actions = { submit, _, _ ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ChunkButton("DHAPPA! +${state.settings.catchPoints} points if valid", submit, fill = Rose, fontSize = 14.sp)
                }
            },
        )
    }
}

// ---------- standings and scores ----------

@Composable
private fun StandingRows(state: GameState, myId: String, startScores: Map<String, Int>, rowHeight: Dp, modifier: Modifier = Modifier, caughtIds: Set<String> = emptySet()) {
    val ordered = state.players.sortedByDescending { it.score }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ordered.forEachIndexed { i, p ->
            val mine = p.id == myId
            val gain = p.score - (startScores[p.id] ?: p.score)
            val shown by animateIntAsState(p.score, tween(900), label = "score")
            val fill by animateFloatAsState((p.score.toFloat() / state.settings.winScore).coerceIn(0f, 1f), tween(900), label = "bar")
            Chunk(Modifier.fillMaxWidth().height(rowHeight), fill = if (mine) AmberSoft else PanelBg, radius = 12.dp, border = 2.dp, shadow = 3.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${i + 1}", Modifier.width(14.dp), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
                    Avatar(if (mine) "You" else p.name, PlayerColors[state.players.indexOf(p).coerceAtLeast(0) % PlayerColors.size], 26.dp)
                    Text(if (mine) "You" else p.name, Modifier.width(78.dp), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f).height(10.dp).background(Sand2, CircleShape).border(1.5.dp, Ink, CircleShape)) {
                        Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(if (p.id in caughtIds) Rose else Teal, CircleShape))
                    }
                    Text("$shown", Modifier.width(22.dp), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    if (startScores.isNotEmpty()) { if (gain > 0) Chip("+$gain", fill = Amber, fontSize = 10.sp) else Spacer(Modifier.width(30.dp)) }
                }
            }
        }
    }
}

/** The standings, opened from the Standings button on the table. */
@Composable
private fun StandingsModal(state: GameState, myId: String, onClose: () -> Unit) {
    val leader = state.players.maxByOrNull { it.score }
    ModalCard(onClose) {
        Column(Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Standings", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink)
                Chip("Round ${state.round} · first to ${state.settings.winScore}")
                Spacer(Modifier.weight(1f))
                SquareButton(DI.Close, onClose, size = 34.dp)
            }
            if (leader != null) Row(
                Modifier.fillMaxWidth().background(Amber, RoundedCornerShape(12.dp)).border(2.dp, Ink, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DIcon(DI.Trophy, tint = Ink, size = 18.dp)
                Text(if (leader.score == 0) "Nobody has scored yet" else "${if (leader.id == myId) "You are" else leader.name + " is"} winning with ${leader.score} points", Modifier.weight(1f), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                val left = (state.settings.winScore - leader.score).coerceAtLeast(0)
                Text("$left point${if (left == 1) "" else "s"} to win", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Ink)
            }
            StandingRows(state, myId, emptyMap(), 32.dp)
        }
    }
}

@Composable
private fun ScoresView(client: GameClient, state: GameState, myId: String, isAdmin: Boolean, startScores: Map<String, Int>, onLeave: () -> Unit) {
    val finished = state.phase as? Phase.Finished
    val reason = (state.phase as? Phase.RoundEnd)?.reason ?: state.notice
    val winnerId = finished?.winnerId ?: (state.phase as? Phase.RoundEnd)?.winnerId
    val caughtIds = state.hands.filter { it.value.out }.keys
    val winner = winnerId?.let { state.player(it) }
    val title = when {
        finished != null -> if (finished.winnerId == myId) "You win the series!" else "${winner?.name} wins!"
        winnerId == null -> "Nobody got it"
        winnerId == myId -> "You won the round!"
        else -> "${winner?.name} won the round!"
    }
    val gain = winner?.let { it.score - (startScores[it.id] ?: it.score) } ?: 0
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Header(if (finished != null) "Series over" else "Round ${state.round} over", onLeave, trailing = { Chip("First to ${state.settings.winScore} wins") })
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Chunk(Modifier.width(250.dp).fillMaxHeight(), fill = Amber, radius = 18.dp, shadow = 5.dp) {
                Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    WinnerArt(Modifier.weight(1f).fillMaxWidth())
                    Text(title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                    if (winner != null) Text("${if (gain > 0) "+$gain points · " else ""}${winner.score} total", fontFamily = Inter, fontSize = 10.sp, color = Ink)
                    Panel(Modifier.fillMaxWidth().padding(top = 6.dp), radius = 12.dp, padding = 8.dp) {
                        Column { CapsLabel("What happened", color = Ink); Text(reason, fontFamily = Inter, fontSize = 10.sp, lineHeight = 13.sp, color = Ink, maxLines = 3) }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StandingRows(state, myId, startScores, 38.dp, Modifier.weight(1f), caughtIds)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (!isAdmin) Text("Waiting for the host…", Modifier.weight(1f), fontFamily = Inter, fontSize = 11.sp, color = Muted)
                    ChunkButton("Leave game", onLeave, fill = Sand)
                    if (isAdmin) {
                        if (finished != null) ChunkButton("Lobby", { client.send(Intent.BackToLobby) }, icon = DI.Back)
                        else {
                            ChunkButton("Lobby", { client.send(Intent.BackToLobby) }, fill = Sand)
                            ChunkButton("Next round", { client.send(Intent.NextRound) }, icon = DI.Check)
                        }
                    }
                }
            }
        }
    }
}
