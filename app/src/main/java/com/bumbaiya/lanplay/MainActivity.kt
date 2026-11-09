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
import com.bumbaiya.lanplay.ui.screens.LanPlayApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LanPlayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(innerPadding)) { LanPlayApp() }
                }
            }
        }
    }
}
