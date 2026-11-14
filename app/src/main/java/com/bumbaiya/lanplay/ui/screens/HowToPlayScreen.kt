package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.components.*
import com.bumbaiya.lanplay.ui.theme.*

private class Rule(val title: String, val text: String, val color: Color)

private val Rules = listOf(
    Rule("The target", "A random player draws 1 to 3 number cards. Side by side they make the target, like 67.", Teal),
    Rule("Your cards", "Everyone starts with a number and an operator card. All cards are face up.", Rose),
    Rule("Your turn", "Draw a card, then Submit an answer or Pass. 30 s. A wrong answer locks Submit for 2 s.", Amber),
    Rule("Numbers", "Cards side by side join up: 3 and 4 make 34 or 43. Each card is used once.", Teal),
    Rule("Operators", "Use at least one. + − take two numbers. Π × all after it, Σ adds all after it, √ must be exact, * is wild.", Rose),
    Rule("Power", "Hold a number over another to raise it: 4 with 3 is 4³ = 64. Still needs an operator.", Amber),
    Rule("Hit the target", "First correct answer wins the round. First to 11 points wins. The host can change this.", Teal),
    Rule("DHAPPA!", "After a pass, build the target from THEIR cards. Right: +3 and they are out. Wrong: you are out.", Terra),
)

/** The rules of Combine, eight short cards. */
@Composable
fun HowToPlayScreen(onBack: () -> Unit, onTour: () -> Unit = onBack) {
    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Full rules", onBack, trailing = { Chip("Combine", fill = Amber) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0..1) Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..3) {
                        val i = row * 4 + col
                        Panel(Modifier.weight(1f).fillMaxHeight(), radius = 14.dp, padding = 10.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Box(Modifier.size(22.dp).background(Rules[i].color, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Ink)
                                    }
                                    Text(Rules[i].title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                                }
                                Text(Rules[i].text, fontFamily = Inter, fontSize = 10.sp, lineHeight = 13.sp, color = Ink)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkButton("Play the tour", onTour, fill = Teal)
                ChunkButton("Got it", onBack, icon = DI.Check)
            }
        }
    }
}
