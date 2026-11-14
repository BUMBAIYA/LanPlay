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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.screens.LanPlayApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Immersive, landscape-only: hide the system bars (swipe to reveal) so the app fills the screen.
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        debugJoin(intent)
        setContent {
            val theme by Session.settings.theme.collectAsState()
            val portrait by Session.settings.portrait.collectAsState()
            // Phones are landscape only; tablets have room for portrait, so they can switch in Settings.
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600
            androidx.compose.runtime.LaunchedEffect(portrait) {
                requestedOrientation = if (isTablet && portrait) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            LanPlayTheme(themeMode = theme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    androidx.compose.runtime.CompositionLocalProvider(com.bumbaiya.lanplay.ui.components.LocalSafePadding provides innerPadding) { LanPlayApp() }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        debugJoin(intent)
    }

    /**
     * Test helper for emulators, which cannot discover each other: connect straight to a host.
     * adb shell am start -n com.bumbaiya.lanplay/.MainActivity --es lanplay_join 10.0.2.2:8765 --es lanplay_name Riya
     * Only works in debuggable builds, so it is not in the released app.
     */
    private fun debugJoin(intent: android.content.Intent?) {
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        if (intent?.getBooleanExtra("lanplay_fixed", false) == true) { Session.init(this); Session.debugFixedCards = true }
        val target = intent?.getStringExtra("lanplay_join") ?: return
        val host = target.substringBefore(':')
        val port = target.substringAfter(':', "8765").toIntOrNull() ?: 8765
        Session.init(this)
        Session.join(com.bumbaiya.lanplay.net.JoinLink("DEBUG", host, port), intent.getStringExtra("lanplay_name") ?: "Riya")
    }
}
