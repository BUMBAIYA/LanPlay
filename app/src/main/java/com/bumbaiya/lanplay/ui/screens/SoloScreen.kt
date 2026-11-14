package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.bumbaiya.lanplay.ui.components.*
import com.bumbaiya.lanplay.ui.theme.*
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.CardPool
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder
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
    var lastTarget by remember { mutableStateOf("") }
    var best by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(ROUND_SECONDS) }
    val targetDigits = remember { mutableStateListOf<Int>() }
    val state = remember(round) { BuilderState(listOf(pool.drawNumber(), pool.drawOperator())) }

    LaunchedEffect(phase, round) {
        if (phase == SoloPhase.Playing) {
            secondsLeft = ROUND_SECONDS
            while (secondsLeft > 0) { delay(1000.milliseconds); secondsLeft-- }
            best = maxOf(best, score); phase = SoloPhase.Over
        }
    }

    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Solo", onBack, trailing = {
                if (phase != SoloPhase.Setup) { Chip("Score $score"); Chip("${secondsLeft} s", fill = Rose) }
                CapsLabel("Target")
                if (targetDigits.isEmpty()) Text("none yet", fontFamily = Inter, fontSize = 12.sp, color = Muted) else TargetBox(targetDigits.joinToString(""))
            })
            when (phase) {
                SoloPhase.Setup -> Column(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Pick your target", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                        Text("Draw 1 to 3 cards yourself, or roll a random one. Then race the clock.", fontFamily = Inter, fontSize = 12.sp, color = Muted)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                        ChunkButton("Draw card", { targetDigits.add(pool.randomDigit()) }, fill = Teal, icon = DI.DrawCard, enabled = targetDigits.size < 3)
                        ChunkButton("Random", { targetDigits.clear(); repeat((1..3).random()) { targetDigits.add(pool.randomDigit()) } }, fill = Sand, icon = DI.Spinner)
                        ChunkButton("Start ${ROUND_SECONDS} s", { phase = SoloPhase.Playing }, icon = DI.Play, enabled = targetDigits.isNotEmpty())
                    }
                }
                SoloPhase.Playing -> {
                    val target = targetDigits.joinToString("").toLong()
                    ExpressionBuilder(
                        state = state, modifier = Modifier.weight(1f),
                        engineHand = {
                            val cards = state.allCards()
                            Hand(digits = cards.filter { it.kind == CardKind.Number }.map { it.glyph.toInt() }, operators = cards.mapNotNull { it.op })
                        },
                        target = target, actionsHeight = 54.dp,
                        onHit = { score++; lastTarget = targetDigits.joinToString(""); targetDigits.clear(); round++; phase = SoloPhase.Setup },
                        actions = { submit, _, locked ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                                ChunkButton("Draw number", { state.addToHand(pool.drawNumber()) }, fill = Teal, icon = DI.DrawCard, fontSize = 13.sp, padding = 14.dp)
                                ChunkButton("Draw operator", { state.addToHand(pool.drawOperator()) }, fill = Rose, icon = DI.DrawCard, fontSize = 13.sp, padding = 14.dp)
                                ChunkButton(if (locked) "Wait…" else "Submit", submit, icon = DI.Check, enabled = !locked, fontSize = 13.sp, padding = 14.dp)
                            }
                        },
                    )
                }
                SoloPhase.Over -> Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Chunk(Modifier.width(290.dp).fillMaxHeight(), fill = Amber, radius = 18.dp, shadow = 5.dp) {
                        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            WinnerArt(Modifier.weight(1f).fillMaxWidth())
                            Text("You won!", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Ink)
                            Text("Time's up. Nice run.", fontFamily = Inter, fontSize = 12.sp, color = Ink)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Panel(Modifier.weight(1f).fillMaxWidth(), padding = 16.dp, contentAlignment = Alignment.CenterStart) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CapsLabel("Your run")
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TargetBox("$score", scale = 1.3f)
                                    Text("target${if (score == 1) "" else "s"} hit", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                                }
                                RunRow("Time", "$ROUND_SECONDS s")
                                RunRow("Last target", lastTarget.ifEmpty { "none" })
                                RunRow("Best run", "$best target${if (best == 1) "" else "s"}")
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                            ChunkButton("Back to menu", onBack, fill = Sand)
                            ChunkButton("Play again", { score = 0; lastTarget = ""; targetDigits.clear(); round++; phase = SoloPhase.Setup }, icon = DI.Play)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunRow(k: String, v: String) = Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(k, Modifier.weight(1f), fontFamily = Inter, fontSize = 12.sp, color = Muted)
    Text(v, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
}
