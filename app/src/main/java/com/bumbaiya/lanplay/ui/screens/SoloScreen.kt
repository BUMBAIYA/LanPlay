package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.game.combinemaths.Hand
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.CardPool
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
            while (secondsLeft > 0) { delay(1000.milliseconds); secondsLeft-- }
            phase = SoloPhase.Over
        }
    }

    com.bumbaiya.lanplay.ui.components.Stage {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) { com.bumbaiya.lanplay.ui.components.DIcon(DI.Back) }
                Text("Solo", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (phase != SoloPhase.Setup) Text("Score $score  ·  ${secondsLeft}s", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFB6F24A))
                Spacer(Modifier.weight(1f))
                Text("TARGET", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color(0xFFB6F24A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (targetDigits.isEmpty()) Text("none yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    targetDigits.forEach { DeckCard(it.toString(), CardKind.Number, size = DeckCardSize.Target) }
                }
            }
            when (phase) {
                SoloPhase.Setup -> Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BigButton("Draw card", { targetDigits.add(pool.randomDigit()) }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, enabled = targetDigits.size < 3, height = 52.dp, fontSize = 16)
                    BigButton("Random", {
                        targetDigits.clear()
                        repeat((1..3).random()) { targetDigits.add(pool.randomDigit()) }
                    }, Modifier.weight(1f), icon = DI.Spinner, primary = false, height = 52.dp, fontSize = 16)
                    BigButton("Start ${ROUND_SECONDS}s", { phase = SoloPhase.Playing }, Modifier.weight(1.2f), icon = DI.Play, enabled = targetDigits.isNotEmpty(), height = 52.dp, fontSize = 16)
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
                        target = target, actionsHeight = 46.dp,
                        onHit = {
                            score++
                            targetDigits.clear()
                            round++
                            phase = SoloPhase.Setup
                        },
                        actions = { submit, _, locked ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BigButton("Draw number", { state.addToHand(pool.drawNumber()) }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, height = 44.dp, fontSize = 14, content = MaterialTheme.lanPlayColors.numberLabel)
                                BigButton("Draw operator", { state.addToHand(pool.drawOperator()) }, Modifier.weight(1f), icon = DI.DrawCard, primary = false, height = 44.dp, fontSize = 14, content = MaterialTheme.lanPlayColors.operatorLabel)
                                BigButton(if (locked) "Wait..." else "Submit", submit, Modifier.weight(1f), icon = DI.Check, enabled = !locked, height = 44.dp, fontSize = 15)
                            }
                        },
                    )
                }
                SoloPhase.Over -> Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Time's up! You hit $score target${if (score == 1) "" else "s"}.", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Spacer(Modifier.height(12.dp))
                    BigButton("Play again", { score = 0; targetDigits.clear(); round++; phase = SoloPhase.Setup }, Modifier.width(260.dp), icon = DI.Play, height = 52.dp)
                }
            }
        }
    }
}
