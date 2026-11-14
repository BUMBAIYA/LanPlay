package com.bumbaiya.lanplay.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.*
import com.bumbaiya.lanplay.ui.game.BCard
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
import com.bumbaiya.lanplay.ui.game.toEngineHand
import com.bumbaiya.lanplay.ui.theme.*

private class TourStep(val title: String, val text: String, val needsAction: Boolean)

private val Steps = listOf(
    TourStep("Hit the target!", "Make the target number before your friends do. Teal cards are numbers, orange cards are operators.", false),
    TourStep("Pick the target", "Draw 1 to 3 number cards. Side by side, they make the target.", true),
    TourStep("Flip a card", "Tap the card to flip it from + to ×.", true),
    TourStep("Make 12", "Tap cards to add them to your row, then Submit.", true),
    TourStep("Power up", "Hold a number over another for a power. Every answer still needs an operator, so add + 0. Make 64!", true),
    TourStep("Σ and Π", "Π multiplies the numbers after it. Add Π, then 2, 3 and 4. Make 24!", true),
    TourStep("The ★ wild card", "A ★ card can be any operator. Tap it to cycle through them. Tap it until it shows ×.", true),
    TourStep("DHAPPA!", "Someone passed? Catch them! Build the target from their cards. Right: +3 points. Wrong: you're out!", false),
    TourStep("You're ready!", "Grab your friends and play!", false),
)

/** A guided, hands-on tour: each step explains one idea and, where it matters, asks you to try it. */
@Composable
fun TourScreen(onBack: () -> Unit, onFullRules: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var done by remember(step) { mutableStateOf(false) }
    var forward by remember { mutableStateOf(true) }
    val last = step == Steps.lastIndex

    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SquareButton(DI.Back, { if (step > 0) { forward = false; step-- } else onBack() })
                Text("Step ${step + 1} of ${Steps.size}", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Muted, maxLines = 1, softWrap = false)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Steps.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(8.dp).background(if (i <= step) Teal else PanelBg, CircleShape).border(1.5.dp, Ink, CircleShape))
                    }
                }
                ChunkButton("Skip", onBack, fill = Sand, height = 32.dp, fontSize = 12.sp, padding = 14.dp)
            }
            AnimatedContent(
                targetState = step, modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
                },
                label = "tour-step",
            ) { s ->
                val st = Steps[s]
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.width(260.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                        Text(st.title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink)
                        Text(st.text, fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp, color = Muted)
                    }
                    Panel(Modifier.weight(1f).fillMaxHeight(), padding = 12.dp, contentAlignment = Alignment.Center) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            when (s) {
                                0 -> {
                                    CapsLabel("Numbers")
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("3", "7", "9").forEach { DeckCard(it, CardKind.Number, size = DeckCardSize.Large) } }
                                    Spacer(Modifier.height(10.dp))
                                    CapsLabel("Operators")
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("+", "−", "Σ", "Π").forEach { DeckCard(it, CardKind.Operator, size = DeckCardSize.Large) } }
                                }
                                1 -> DrawTargetDemo { done = true }
                                2 -> TiltPractice { done = true }
                                3 -> Practice(listOf(num(1, "3"), num(2, "4"), plus(3)), 12, "Tap the + in your row to flip it to ×.") { done = true }
                                4 -> Practice(listOf(num(1, "4"), num(2, "3"), num(3, "0"), plus(4)), 64, "Drag the 3 onto the 4, then tap + and 0.") { done = true }
                                5 -> Practice(listOf(num(1, "2"), num(2, "3"), num(3, "4"), BCard(4, "Π", CardKind.Operator, op = Op.Pi)), 24, "Tap Π, then 2, 3 and 4.") { done = true }
                                6 -> WildPractice { done = true }
                                7 -> {
                                    CapsLabel("Dev's cards")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DeckCard("8", CardKind.Number, size = DeckCardSize.Large); DeckCard("2", CardKind.Number, size = DeckCardSize.Large)
                                        DeckCard("5", CardKind.Number, size = DeckCardSize.Large); DeckCard("+", CardKind.Operator, size = DeckCardSize.Large)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    ChunkButton("DHAPPA!", {}, fill = Rose)
                                }
                                else -> {
                                    // The game's own name, spelled out in cards.
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        "COMBINE".forEachIndexed { i, c -> DeckCard(c.toString(), if (i % 2 == 0) CardKind.Number else CardKind.Operator, size = DeckCardSize.Large) }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("Grab your friends and play!", fontFamily = Inter, fontSize = 11.sp, color = Muted)
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (Steps[step].needsAction && done) Chip("Nice!", fill = Teal, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (last) ChunkButton("Read the full rules", onFullRules, fill = Sand, icon = DI.Book)
                ChunkButton(if (last) "Let's play!" else "Next", { if (last) onBack() else { forward = true; step++ } }, fill = Teal, icon = if (last) DI.Play else DI.Right, enabled = !Steps[step].needsAction || done)
            }
        }
    }
}

private fun num(id: Int, g: String) = BCard(id, g, CardKind.Number)
private fun plus(id: Int) = BCard(id, "+", CardKind.Operator, alt = "×", op = Op.Plus)

@Composable
private fun DrawTargetDemo(onDone: () -> Unit) {
    val deck = listOf("6", "7", "2")
    var drawn by remember { mutableIntStateOf(0) }
    var stopped by remember { mutableStateOf(false) }
    CapsLabel("Target")
    Box(Modifier.height(60.dp), contentAlignment = Alignment.Center) {
        if (drawn == 0) Text("Draw a card", fontFamily = Inter, fontSize = 12.sp, color = Muted)
        else key(drawn) { FlipIn { TargetBox(deck.take(drawn).joinToString(""), scale = 1.3f) } }
    }
    Spacer(Modifier.height(8.dp))
    if (!stopped) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChunkButton("Draw a card", { drawn++ }, fill = Teal, icon = DI.DrawCard, enabled = drawn < 3)
        ChunkButton("Stop here", { stopped = true; onDone() }, icon = DI.Check, enabled = drawn > 0)
    } else Text("The target is ${deck.take(drawn).joinToString("")}.", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
}

@Composable
private fun TiltPractice(onDone: () -> Unit) {
    var card by remember { mutableStateOf(plus(0)) }
    LaunchedEffect(card.glyph) { if (card.glyph == "×") onDone() }
    DeckCard(card.glyph, CardKind.Operator, altGlyph = card.alt, size = DeckCardSize.Large, scale = 1.5f, onClick = { card = card.tilted() })
}

@Composable
private fun WildPractice(onDone: () -> Unit) {
    var card by remember { mutableStateOf(BCard(0, "*", CardKind.Operator, op = Op.Wild, wild = true)) }
    LaunchedEffect(card.glyph) { if (card.glyph == "×") onDone() }
    DeckCard(card.glyph, CardKind.Operator, isWild = true, size = DeckCardSize.Large, scale = 1.5f, onClick = { card = card.tilted() })
}

@Composable
private fun ColumnScope.Practice(cards: List<BCard>, target: Long, hint: String, onDone: () -> Unit) {
    val state = remember { BuilderState(cards) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CapsLabel("Target"); TargetBox(target.toString())
        Spacer(Modifier.weight(1f))
        Text(hint, fontFamily = Inter, fontSize = 10.sp, color = Muted, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.widthIn(max = 200.dp))
    }
    ExpressionBuilder(
        state, engineHand = { state.allCards().toEngineHand() }, target = target, onHit = onDone, penaltyMs = 1000, modifier = Modifier.weight(1f), actionsHeight = 50.dp, handLabel = "Your row",
        actions = { submit, _, locked ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ChunkButton(if (locked) "Wait…" else "Submit", submit, icon = DI.Check, enabled = !locked, height = 38.dp, fontSize = 13.sp) }
        },
    )
}
