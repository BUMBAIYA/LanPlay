package com.bumbaiya.lanplay.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize

/**
 * A temporary screen that shows the theme: fonts, colors, a button and sample cards.
 * We will replace it with the real Games screen.
 */
@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("LanPlay", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Games for the room you're in. One hotspot, no internet, no accounts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("TARGET", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckCard("6", CardKind.Number, size = DeckCardSize.Large)
                DeckCard("9", CardKind.Number, size = DeckCardSize.Large)
            }
            Text("YOUR CARDS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckCard("8", CardKind.Number, dimmed = true)
                DeckCard("9", CardKind.Number, dimmed = true)
                DeckCard("3", CardKind.Number)
                DeckCard("Π", CardKind.Operator, dimmed = true)
                var plusTilted by remember { mutableStateOf(false) }
                DeckCard(
                    glyph = if (plusTilted) "×" else "+",
                    altGlyph = if (plusTilted) "+" else "×",
                    kind = CardKind.Operator,
                    onClick = { plusTilted = !plusTilted },
                )
                DeckCard("√", CardKind.Operator)
            }
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Host a game", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeShowcaseDarkPreview() {
    LanPlayTheme(themeMode = ThemeMode.Dark) { ThemeShowcase() }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ThemeShowcaseLightPreview() {
    LanPlayTheme(themeMode = ThemeMode.Light) { ThemeShowcase() }
}
