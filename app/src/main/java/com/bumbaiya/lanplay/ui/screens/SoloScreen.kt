package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.Hand
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.CardPool
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
import kotlinx.coroutines.delay

private enum class SoloPhase { Setup, Playing, Over }
private const val ROUND_SECONDS = 60

/**
 * Solo mode: pick a target (draw up to 3 cards yourself or roll a random one), then race the
 * clock to build it. Each hit scores a point and starts a new target. Timeout ends the run.
 */
@Composable
fun SoloScreen(onBack: () -> Unit) {
    val pool = remember { CardPool() }
    var phase by remember { mutableStateOf(SoloPhase.Setup) }
    var score by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(ROUND_SECONDS) }
    val targetDigits = remember { mutableStateListOf<Int>() }
    val state = remember(round) { BuilderState(listOf(pool.drawNumber(), pool.drawOperator())) }

    LaunchedEffect(phase, round) {
        if (phase == SoloPhase.Playing) {
            secondsLeft = ROUND_SECONDS
            while (secondsLeft > 0) { delay(1000); secondsLeft-- }
            phase = SoloPhase.Over
        }
    }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.lanPlayColors.accentText)) { Text("< Back") }
            if (phase != SoloPhase.Setup) {
                Text("Score $score   ${secondsLeft}s", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Text("Solo", style = MaterialTheme.typography.headlineMedium)
        Text("TARGET", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (targetDigits.isEmpty()) Text("No target yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            targetDigits.forEach { DeckCard(it.toString(), CardKind.Number, size = DeckCardSize.Large) }
        }

        when (phase) {
            SoloPhase.Setup -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        enabled = targetDigits.size < 3,
                        onClick = { targetDigits.add(pool.randomDigit()) },
                    ) { Text("Draw target card") }
                    OutlinedButton(onClick = {
                        targetDigits.clear()
                        repeat((1..3).random()) { targetDigits.add(pool.randomDigit()) }
                    }) { Text("Random target") }
                }
                Button(
                    enabled = targetDigits.isNotEmpty(),
                    onClick = { phase = SoloPhase.Playing },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text("Start ${ROUND_SECONDS}s round", style = MaterialTheme.typography.titleMedium) }
            }
            SoloPhase.Playing -> {
                val target = targetDigits.joinToString("").toLong()
                ExpressionBuilder(
                    state = state,
                    engineHand = {
                        val cards = state.allCards()
                        Hand(
                            digits = cards.filter { it.kind == CardKind.Number }.map { it.glyph.toInt() },
                            operators = cards.mapNotNull { it.op },
                        )
                    },
                    target = target,
                    onHit = {
                        score++
                        targetDigits.clear()
                        round++
                        phase = SoloPhase.Setup
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { state.addToHand(pool.drawNumber()) }) { Text("Draw number") }
                    OutlinedButton(onClick = { state.addToHand(pool.drawOperator()) }) { Text("Draw operator") }
                }
            }
            SoloPhase.Over -> {
                Text("Time's up! You hit $score target${if (score == 1) "" else "s"}.", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { score = 0; targetDigits.clear(); round++; phase = SoloPhase.Setup },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text("Play again", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
