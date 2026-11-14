package com.bumbaiya.lanplay.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.Avatar
import com.bumbaiya.lanplay.ui.components.CapsLabel
import com.bumbaiya.lanplay.ui.components.Chunk
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.Frame
import com.bumbaiya.lanplay.ui.components.GearArt
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.IconDisc
import com.bumbaiya.lanplay.ui.components.InkField
import com.bumbaiya.lanplay.ui.components.LogoMark
import com.bumbaiya.lanplay.ui.components.NameMax
import com.bumbaiya.lanplay.ui.components.Panel
import com.bumbaiya.lanplay.ui.components.Segmented
import com.bumbaiya.lanplay.ui.components.SparkleArt
import com.bumbaiya.lanplay.ui.components.ToggleSwitch
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.AmberSoft
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Inter
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Sand
import com.bumbaiya.lanplay.ui.theme.Sand2
import com.bumbaiya.lanplay.ui.theme.Teal

@Composable
fun SettingsScreen(onBack: () -> Unit, onCredits: () -> Unit) {
    val settings = Session.settings
    val name by settings.name.collectAsState()
    val sound by settings.sound.collectAsState()
    val haptics by settings.haptics.collectAsState()
    val portrait by settings.portrait.collectAsState()
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    Frame {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Settings", onBack, subtitle = "This device only", lead = { Box(Modifier.size(44.dp)) { GearArt(Modifier.fillMaxSize()) } }, trailing = {
                Chunk(Modifier.height(44.dp), onClick = onCredits, fill = Amber, radius = 14.dp, shadow = 4.dp, contentAlignment = Alignment.Center) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DIcon(DI.Heart, tint = Rose, size = 16.dp)
                        Text("Credits", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                    }
                }
            })
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Panel(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CapsLabel("Profile", color = Ink)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Avatar(name.ifBlank { "?" }, Teal, 46.dp)
                                InkField(name, { settings.setName(it) }, Modifier.weight(1f), maxLength = NameMax, height = 46.dp, placeholder = "Your name")
                            }
                            Text("Used for every room you host or join.", fontFamily = Inter, fontSize = 11.sp, color = Muted)
                        }
                    }
                    Panel(Modifier.fillMaxWidth().weight(1f), padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CapsLabel("Appearance")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Theme", Modifier.weight(1f), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
                                Row(Modifier.background(Sand2, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Chunk(Modifier.height(36.dp), fill = Amber, radius = 11.dp, shadow = 3.dp, contentAlignment = Alignment.Center) { Text("Light", Modifier.padding(horizontal = 14.dp), fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink) }
                                    Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Dark", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted)
                                        Box(Modifier.background(AmberSoft, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("Coming soon", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Ink) }
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isTablet) "Screen" else "Screen (tablets only)", Modifier.weight(1f), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isTablet) Ink else Muted)
                                Segmented(listOf("Landscape", "Portrait"), if (portrait) 1 else 0, { settings.setPortrait(it == 1) }, enabled = booleanArrayOf(true, isTablet))
                            }
                        }
                    }
                }
                Panel(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CapsLabel("Sound and feedback")
                        ToggleRowCard(DI.Speaker, "Sound effects", "Turn changes and card draws", sound, settings::setSound)
                        ToggleRowCard(DI.Torch, "Vibration", "A buzz when it becomes your turn", haptics, settings::setHaptics)
                        Spacer(Modifier.weight(1f))
                        Text("Each player controls their own. You can also mute from the speaker button on the game table.", fontFamily = Inter, fontSize = 10.sp, lineHeight = 14.sp, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRowCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Sand, RoundedCornerShape(14.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconDisc(icon, Color(0xFFF4B5A0), Color(0xFFF4B5A0), 34.dp)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
            Text(sub, fontFamily = Inter, fontSize = 10.sp, color = Muted, maxLines = 1)
        }
        ToggleSwitch(on, onChange)
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Frame {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Credits", onBack)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Chunk(Modifier.width(230.dp).fillMaxHeight(), fill = Amber, radius = 18.dp, shadow = 5.dp) {
                    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SparkleArt(Modifier.fillMaxSize())
                            LogoMark(Modifier.size(104.dp), bg = Color.White)
                        }
                        Text("LanPlay", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ink)
                        Text("Games for the room, over your own Wi-Fi.", fontFamily = Inter, fontSize = 10.sp, color = Ink, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Panel(Modifier.weight(1f).fillMaxHeight(), padding = 18.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        CreditRow("Made by", "Amit Chauhan")
                        CreditRow("GitHub", "github.com/BUMBAIYA", onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BUMBAIYA")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
                        CreditRow("Fonts", "Baloo 2 and Inter")
                        CreditRow("Built with", "Kotlin, Jetpack Compose, Ktor")
                        CreditRow("Version", "1.0", last = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditRow(k: String, v: String, last: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).then(if (onClick != null) Modifier.androidClick(onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(k, Modifier.width(80.dp), fontFamily = Inter, fontSize = 11.sp, color = Muted)
        Text(v, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
    }
}

private fun Modifier.androidClick(onClick: () -> Unit) = this.then(Modifier.clickable(onClick = onClick))
