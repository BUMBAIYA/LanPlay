package com.bumbaiya.lanplay.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.bumbaiya.lanplay.ui.components.IconLabel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.game.BCard
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
import com.bumbaiya.lanplay.ui.game.toEngineHand
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

private class TourStep(val title: String, val text: String, val needsAction: Boolean)

private val Steps = listOf(
    TourStep("Hit the target!", "Make the target number before your friends do. Let's try it!", false),
    TourStep("Pick the target", "Draw 1 to 3 number cards. Side by side, they make the target.", true),
    TourStep("Flip a card", "Tap the card to flip it from + to ×.", true),
    TourStep("Make 12", "Drag cards into your row, then tap Submit.", true),
    TourStep("Power up", "Hold a number over another for a power. Every answer still needs an operator, so add + 0. Make 64!", true),
    TourStep("Σ and Π", "Π multiplies all the numbers after it. Place Π, then 2 3 4. Make 24!", true),
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

    ScreenColumn(gap = 18.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.offset(x = (-10).dp).size(44.dp).clickable(onClick = { if (step > 0) { forward = false; step-- } else onBack() }), contentAlignment = Alignment.Center) { DIcon(DI.Back) }
            Text("Step ${step + 1} of ${Steps.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Text("Skip", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Steps.indices.forEach { i ->
                Box(Modifier.weight(1f).height(6.dp).background(if (i <= step) MaterialTheme.lanPlayColors.accentText else MaterialTheme.lanPlayColors.cardBorder, RoundedCornerShape(3.dp)))
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
            },
            label = "tour-step",
        ) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val st = Steps[s]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(st.title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Text(st.text, fontSize = 16.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (s) {
                    1 -> DrawTargetDemo { done = true }
                    2 -> TiltPractice { done = true }
                    3 -> Practice(listOf(num(1, "3"), num(2, "4"), plus(3)), 12, "Tap 3, then +, then 4. Tap the + in your row to flip it to ×.") { done = true }
                    4 -> Practice(listOf(num(1, "4"), num(2, "3"), num(3, "0"), plus(4)), 64, "Drag the 3 onto the 4, then tap + and 0.") { done = true }
                    5 -> Practice(
                        listOf(num(1, "2"), num(2, "3"), num(3, "4"), BCard(4, "Π", CardKind.Operator, op = Op.Pi)), 24,
                        "Tap Π, then 2, 3 and 4.",
                    ) { done = true }
                    0 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("Numbers")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("3", "7", "9").forEach { DeckCard(it, CardKind.Number, size = DeckCardSize.Large) } }
                        SectionLabel("Operators")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("+", "−", "Σ", "Π").forEach { DeckCard(it, CardKind.Operator, size = DeckCardSize.Large) } }
                    }
                    6 -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeckCard("8", CardKind.Number, size = DeckCardSize.Large); DeckCard("2", CardKind.Number, size = DeckCardSize.Large)
                        DeckCard("5", CardKind.Number, size = DeckCardSize.Large); DeckCard("+", CardKind.Operator, size = DeckCardSize.Large)
                    }
                    7 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        "DHAPPA".forEachIndexed { i, c -> DeckCard(c.toString(), if (i % 2 == 0) CardKind.Number else CardKind.Operator, size = DeckCardSize.Large) }
                    }
                    else -> {}
                }
                if (st.needsAction && done) Text("Nice!", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.lanPlayColors.accentText)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(
                if (last) "Let's play!" else "Next", { if (last) onBack() else { forward = true; step++ } },
                icon = if (last) DI.Play else DI.Right, enabled = !Steps[step].needsAction || done,
            )
            if (last) BigButton("Read the full rules", onFullRules, icon = DI.Book, primary = false, height = 48.dp, fontSize = 16)
        }
    }
}

private fun num(id: Int, g: String) = BCard(id, g, CardKind.Number)
private fun plus(id: Int) = BCard(id, "+", CardKind.Operator, alt = "×", op = Op.Plus)

@Composable
private fun DrawTargetDemo(onDone: () -> Unit) {
    val deck = listOf("6", "9", "2")
    var drawn by remember { mutableIntStateOf(0) }
    var stopped by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        deck.forEachIndexed { i, g ->
            AnimatedVisibility(i < drawn, enter = scaleIn() + fadeIn()) { DeckCard(g, CardKind.Number, size = DeckCardSize.Large) }
        }
    }
    if (!stopped) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Draw a card", { drawn++ }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, enabled = drawn < 3, height = 52.dp, fontSize = 16)
            BigButton("Stop here", { stopped = true; onDone() }, Modifier.weight(1f), icon = DI.Check, enabled = drawn > 0, height = 52.dp, fontSize = 16)
        }
    } else {
        Text("The target is ${deck.take(drawn).joinToString("")}.", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TiltPractice(onDone: () -> Unit) {
    var card by remember { mutableStateOf(plus(0)) }
    LaunchedEffect(card.glyph) { if (card.glyph == "×") onDone() }
    DeckCard(card.glyph, CardKind.Operator, altGlyph = card.alt, size = DeckCardSize.Large, onClick = { card = card.tilted() })
}

@Composable
private fun Practice(cards: List<BCard>, target: Long, hint: String, onDone: () -> Unit) {
    val state = remember { BuilderState(cards) }
    var showHint by remember { mutableStateOf(false) }
    Text("TARGET  $target", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.lanPlayColors.accentText)
    ExpressionBuilder(state, engineHand = { state.allCards().toEngineHand() }, target = target, onHit = onDone, penaltyMs = 1000)
    TextButton(onClick = { showHint = !showHint }) { Text(if (showHint) "Hide hint" else "Show hint", color = MaterialTheme.lanPlayColors.accentText) }
    if (showHint) Text(hint, style = MaterialTheme.typography.bodyMedium)
}
