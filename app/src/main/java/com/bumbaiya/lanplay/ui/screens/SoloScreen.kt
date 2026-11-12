package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.bumbaiya.lanplay.ui.components.IconLabel
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
    com.bumbaiya.lanplay.ui.components.LockLandscape()
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
        Header("Solo", onBack)
        if (phase != SoloPhase.Setup) Text("Score $score   ·   ${secondsLeft}s left", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.lanPlayColors.accentText)
        Panel(radius = 16.dp) {
            SectionLabel("Target")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (targetDigits.isEmpty()) Text("No target yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                targetDigits.forEach { DeckCard(it.toString(), CardKind.Number, size = DeckCardSize.Target) }
            }
        }

        when (phase) {
            SoloPhase.Setup -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Draw card", { targetDigits.add(pool.randomDigit()) }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, enabled = targetDigits.size < 3, height = 48.dp, fontSize = 16)
                    BigButton("Random", {
                        targetDigits.clear()
                        repeat((1..3).random()) { targetDigits.add(pool.randomDigit()) }
                    }, Modifier.weight(1f), icon = DI.Spinner, primary = false, height = 48.dp, fontSize = 16)
                }
                BigButton("Start ${ROUND_SECONDS}s round", { phase = SoloPhase.Playing }, icon = DI.Play, enabled = targetDigits.isNotEmpty())
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
                    BigButton("Draw number", { state.addToHand(pool.drawNumber()) }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, height = 44.dp, fontSize = 14, content = MaterialTheme.lanPlayColors.numberLabel)
                    BigButton("Draw operator", { state.addToHand(pool.drawOperator()) }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, height = 44.dp, fontSize = 14, content = MaterialTheme.lanPlayColors.operatorLabel)
                }
            }
            SoloPhase.Over -> {
                Text("Time's up! You hit $score target${if (score == 1) "" else "s"}.", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                BigButton("Play again", { score = 0; targetDigits.clear(); round++; phase = SoloPhase.Setup }, icon = DI.Play)
            }
        }
    }
}
