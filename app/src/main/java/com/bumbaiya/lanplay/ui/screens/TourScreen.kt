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

    ScreenColumn {
        BackLink(onBack)
        LinearProgressIndicator(progress = { (step + 1f) / Steps.size }, modifier = Modifier.fillMaxWidth())
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
            },
            label = "tour-step",
        ) { s ->
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val st = Steps[s]
                Text(st.title, style = MaterialTheme.typography.headlineMedium)
                Text(st.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (s) {
                    1 -> DrawTargetDemo { done = true }
                    2 -> TiltPractice { done = true }
                    3 -> Practice(
                        listOf(num(1, "3"), num(2, "4"), plus(3)), 12,
                        "Flip + to ×. Then 3 × 4.",
                    ) { done = true }
                    4 -> Practice(listOf(num(1, "4"), num(2, "3"), num(3, "0"), plus(4)), 64, "Drag the 3 onto the 4, then add + 0.") { done = true }
                    5 -> Practice(
                        listOf(num(1, "2"), num(2, "3"), num(3, "4"), BCard(4, "Π", CardKind.Operator, op = Op.Pi)), 24,
                        "Π 2 3 4 = 2 × 3 × 4",
                    ) { done = true }
                    else -> {}
                }
                if (st.needsAction && done) Text("Nice!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.lanPlayColors.accentText)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 0) OutlinedButton(onClick = { forward = false; step-- }, modifier = Modifier.heightIn(min = 52.dp)) { IconLabel(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Button(
                enabled = !Steps[step].needsAction || done,
                onClick = { if (last) onBack() else { forward = true; step++ } },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text(if (last) "Let's play!" else "Next", style = MaterialTheme.typography.titleMedium) }
        }
        if (Steps[step].needsAction && !done) TextButton(onClick = { forward = true; step++ }) { IconLabel(Icons.AutoMirrored.Filled.ArrowForward, "Skip this step", color = MaterialTheme.lanPlayColors.accentText) }
        if (last) OutlinedButton(onClick = onFullRules, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { IconLabel(Icons.Filled.Info, "Read the full rules") }
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(enabled = drawn < 3, onClick = { drawn++ }, modifier = Modifier.heightIn(min = 52.dp)) { IconLabel(Icons.Filled.Add, "Draw a card") }
            OutlinedButton(enabled = drawn > 0, onClick = { stopped = true; onDone() }, modifier = Modifier.heightIn(min = 52.dp)) { IconLabel(Icons.Filled.Check, "Stop here") }
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
