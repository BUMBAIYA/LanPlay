package com.bumbaiya.lanplay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.ui.components.BackLink
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.components.ScreenColumn
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.game.BCard

/** The rules of Combine Maths, with cards you can tap to see how they behave. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    ScreenColumn {
        BackLink(onBack)
        Text("How to play", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Combine Maths is a race of mental maths. Use your cards to make the target number before anyone else does.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Step("1", "The target") {
            Body("A random player draws 1, 2 or 3 number cards and stops whenever they like. The cards side by side are the target, in the order drawn.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckCard("6", CardKind.Number, size = DeckCardSize.Large)
                DeckCard("9", CardKind.Number, size = DeckCardSize.Large)
            }
            Body("These two make the target 69.")
        }

        Step("2", "Your cards") {
            Body("Everyone starts with one number card and one operator card. Blue cards are numbers, red cards are operators. All cards are face up, so everyone can see everyone's hand.")
        }

        Step("3", "On your turn") {
            Body("Draw one card, a number or an operator. Then either build an answer and press Submit, or press Pass. You have 30 seconds (the host can change this). Running out of time counts as a pass.")
            Body("A wrong answer locks Submit for 2 seconds. Cards are random and endless, so there is no deck to run out.")
        }

        Step("4", "Numbers") {
            Body("Number cards placed side by side join up: a 3 and a 4 can make 34 or 43. You do not have to use every card, and each card can be used once.")
        }

        Step("5", "Operators (tap the cards!)") {
            Body("You must use at least one operator card.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TiltDemo("+", "×")
                TiltDemo("−", "÷")
            }
            Body("Two-in-one cards: the big symbol at the top right is the one that counts. Tap a card to tilt it and swap. + and − take exactly two numbers, like 3 × 4.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeckCard("Π", CardKind.Operator)
                DeckCard("Σ", CardKind.Operator)
                DeckCard("√", CardKind.Operator)
                WildDemo()
            }
            Bullet("Π multiplies all the number cards after it, two or more: Π 2 3 4 = 24")
            Bullet("Σ adds all the number cards after it, two or more: Σ 1 2 3 = 6")
            Bullet("√ is the square root: √ 9 = 3 (it must come out exact)")
            Bullet("* is wild: tap it to choose any operator you like")
            Bullet("No bracket cards: just imagine brackets anywhere. 3 + 4 × 2 counts as 14 if that is your target.")
        }

        Step("6", "Power") {
            Body("Drag a number card and hold it over another number card. It sits on top as a small raised number, the power.")
            Box(Modifier.padding(top = 10.dp, end = 12.dp)) {
                DeckCard("4", CardKind.Number)
                DeckCard("3", CardKind.Number, size = DeckCardSize.Small, modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-10).dp))
            }
            Body("That is 4³ = 64. Tap the small card to take the power off again. Both cards must be single numbers, and your answer still needs one operator card, for example 4³ + 0.")
        }

        Step("7", "Hit the target") {
            Body("The first player whose answer equals the target wins the round and scores points. The first to 11 points wins the series. The host can change all of these numbers in the lobby.")
        }

        Step("8", "DHAPPA! Catching") {
            Body("When a player passes or runs out of time, you get a short window to catch them. If you can build the target from THEIR cards, press DHAPPA and build it.")
            Bullet("Only after their turn is over, and only for a few seconds.")
            Bullet("A correct catch earns you 3 points and puts them out of the round.")
            Bullet("A wrong catch puts YOU out of the round, so be sure!")
        }

        Step("9", "Playing alone") {
            Body("Solo mode lets you draw your own target or roll a random one, then race a 60 second clock. Each target you hit scores a point.")
        }

        Step("10", "Tips") {
            Bullet("Watch other players build: you can see their row appear live.")
            Bullet("By default the app does not show the result while you build. The host can turn that on in the lobby.")
            Bullet("Cards are random, so a pass now can be a catch for someone else. Count their cards too.")
        }
    }
}

@Composable
private fun Step(number: String, title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Step $number")
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

@Composable
private fun Bullet(text: String) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("•", style = MaterialTheme.typography.bodyMedium)
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/** A real two-in-one card you can tap. */
@Composable
private fun TiltDemo(a: String, b: String) {
    var tilted by remember { mutableStateOf(false) }
    DeckCard(if (tilted) b else a, CardKind.Operator, altGlyph = if (tilted) a else b, onClick = { tilted = !tilted })
}

/** A real wild card you can tap to cycle through. */
@Composable
private fun WildDemo() {
    var card by remember { mutableStateOf(BCard(0, "*", CardKind.Operator, wild = true)) }
    DeckCard(card.glyph, CardKind.Operator, onClick = { card = card.tilted() })
}
