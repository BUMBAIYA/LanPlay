package com.bumbaiya.lanplay.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.platform.Session
import com.bumbaiya.lanplay.ui.components.Avatar
import com.bumbaiya.lanplay.ui.components.CapsLabel
import com.bumbaiya.lanplay.ui.components.Chunk
import com.bumbaiya.lanplay.ui.components.CombineArt
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DIcon
import com.bumbaiya.lanplay.ui.components.Frame
import com.bumbaiya.lanplay.ui.components.Header
import com.bumbaiya.lanplay.ui.components.HostArt
import com.bumbaiya.lanplay.ui.components.JoinArt
import com.bumbaiya.lanplay.ui.components.LogoMark
import com.bumbaiya.lanplay.ui.components.SoloArt
import com.bumbaiya.lanplay.ui.components.SoonArt
import com.bumbaiya.lanplay.ui.components.SquareButton
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Inter
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.PanelBg
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Sand2
import com.bumbaiya.lanplay.ui.theme.Teal
import com.bumbaiya.lanplay.ui.theme.TealDark

private val SoloTile = Color(0xFFD9825A)

/** The home screen: pick a game, like the events in a game lobby. */
@Composable
fun GamesScreen(onOpen: () -> Unit, onSettings: () -> Unit) {
    val name by Session.settings.name.collectAsState()
    Frame {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DIcon(DI.Logo, tint = TealDark, size = 30.dp)
                Text("LanPlay", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                if (name.isNotBlank()) Chunk(Modifier.height(44.dp), onClick = onSettings, fill = Sand2, radius = 22.dp, border = 2.dp, shadow = 3.dp, contentAlignment = Alignment.Center) {
                    Row(Modifier.padding(start = 6.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Avatar(name, Teal, 28.dp)
                        Text(name, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink, maxLines = 1)
                    }
                }
                SquareButton(DI.Sliders, onSettings, size = 44.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CHOOSE A GAME", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp, color = TealDark)
                Text("One hotspot, no internet, no accounts.", fontFamily = Inter, fontSize = 11.sp, color = Muted)
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Chunk(Modifier.width(232.dp).fillMaxHeight(), onClick = onOpen, fill = Amber, radius = 18.dp, shadow = 5.dp) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().height(26.dp).background(Color(0x26241C18)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("2 to 6 players", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Ink)
                        }
                        CombineArt(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp))
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                            Text("Combine", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ink)
                            Text("Build the target from your cards. Catch anyone who passes.", fontFamily = Inter, fontSize = 11.sp, lineHeight = 15.sp, color = Ink.copy(alpha = 0.85f))
                        }
                    }
                }
                Chunk(Modifier.width(190.dp).fillMaxHeight(), fill = PanelBg, radius = 18.dp, shadow = 5.dp) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().height(26.dp).background(Color(0x0F241C18)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("COMING SOON", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Muted)
                        }
                        SoonArt(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp))
                        Text("More games for the room. Any game, cards or not.", Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, color = Muted)
                    }
                }
            }
        }
    }
}

/** Combine's own menu: host, join or play solo. */
@Composable
fun GameMenuScreen(onBack: () -> Unit, onSolo: () -> Unit, onHost: () -> Unit, onJoin: () -> Unit, onHowTo: () -> Unit, onTour: () -> Unit) {
    Frame(cards = true) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.width(224.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Header("", onBack, subtitle = null, trailing = {}, modifier = Modifier.height(44.dp))
                    Chunk(Modifier.fillMaxWidth().weight(1f), fill = Amber, radius = 18.dp, shadow = 5.dp) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxWidth()) { CombineArt(Modifier.fillMaxSize(), listOf("6", "×", "7", "+")) }
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                                Text("Combine", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Ink)
                                Text("Combine number and operator cards to hit the target. Spot a player who missed it and catch them.", Modifier.padding(top = 4.dp), fontFamily = Inter, fontSize = 11.sp, lineHeight = 15.sp, color = Ink.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("HOW DO YOU WANT TO PLAY?", fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.6.sp, color = TealDark)
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeTile("Host a game", "Create a room, friends join you", Teal, onHost, Modifier.weight(1f)) { HostArt(Modifier.fillMaxSize()) }
                        ModeTile("Join a game", "Enter the room ID from the host", Rose, onJoin, Modifier.weight(1f)) { JoinArt(Modifier.fillMaxSize()) }
                        ModeTile("Play solo", "Your own target, beat the timer", SoloTile, onSolo, Modifier.weight(1f)) { SoloArt(Modifier.fillMaxSize()) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SideButton("How to play", DI.Tour, onTour, Modifier.weight(1f))
                        SideButton("Full rules", DI.Book, onHowTo, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTile(title: String, sub: String, fill: Color, onClick: () -> Unit, modifier: Modifier, art: @Composable () -> Unit) {
    Chunk(modifier.fillMaxHeight(), onClick = onClick, fill = fill, radius = 18.dp, shadow = 5.dp) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { art() }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                Text(title, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink, maxLines = 1)
                Text(sub, fontFamily = Inter, fontSize = 11.sp, lineHeight = 14.sp, color = Ink.copy(alpha = 0.85f), maxLines = 2)
            }
        }
    }
}

@Composable
private fun SideButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Chunk(modifier.height(44.dp), onClick = onClick, fill = Sand2, radius = 14.dp, border = 2.dp, shadow = 3.dp, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIcon(icon, tint = Ink, size = 16.dp)
            Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Ink)
        }
    }
}
