package com.bumbaiya.lanplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.bumbaiya.lanplay.ui.theme.LanPlayTheme
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.Hand
import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.game.BCard
import com.bumbaiya.lanplay.ui.game.BuilderState
import com.bumbaiya.lanplay.ui.game.ExpressionBuilder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LanPlayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(innerPadding).padding(20.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    ) {
                        androidx.compose.material3.Text("Builder demo", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                        androidx.compose.material3.Text("TARGET 69", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                        val cards = listOf(
                            BCard(1, "6", CardKind.Number), BCard(2, "9", CardKind.Number),
                            BCard(3, "3", CardKind.Number), BCard(4, "8", CardKind.Number),
                            BCard(5, "+", CardKind.Operator, alt = "×", op = Op.Plus),
                            BCard(6, "−", CardKind.Operator, alt = "÷", op = Op.Minus),
                            BCard(7, "Π", CardKind.Operator, op = Op.Pi),
                            BCard(8, "Σ", CardKind.Operator, op = Op.Sigma),
                            BCard(9, "√", CardKind.Operator, op = Op.Sqrt),
                        )
                        val state = androidx.compose.runtime.remember { BuilderState(cards) }
                        val engineHand = Hand(
                            digits = cards.filter { it.kind == CardKind.Number }.map { it.glyph.toInt() },
                            operators = cards.mapNotNull { it.op },
                        )
                        ExpressionBuilder(state, engineHand, target = 69)
                    }
                }
            }
        }
    }
}
