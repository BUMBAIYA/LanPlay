package com.bumbaiya.lanplay.platform

import android.content.Context
import com.bumbaiya.lanplay.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit

/** The user's own preferences, kept on the phone: name, theme and sound. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lanplay", Context.MODE_PRIVATE)

    private val _name = MutableStateFlow(prefs.getString("name", "") ?: "")
    private val _theme = MutableStateFlow(runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: "") }.getOrDefault(ThemeMode.System))
    private val _sound = MutableStateFlow(prefs.getBoolean("sound", true))
    private val _haptics = MutableStateFlow(prefs.getBoolean("haptics", true))
    private val _portrait = MutableStateFlow(prefs.getBoolean("portrait", false))

    val name: StateFlow<String> = _name
    val theme: StateFlow<ThemeMode> = _theme
    val sound: StateFlow<Boolean> = _sound
    val haptics: StateFlow<Boolean> = _haptics
    /** Tablets only: play in portrait instead of landscape. Phones are always landscape. */
    val portrait: StateFlow<Boolean> = _portrait

    fun setName(v: String) { _name.value = v; prefs.edit { putString("name", v) } }
    fun setTheme(v: ThemeMode) { _theme.value = v; prefs.edit { putString("theme", v.name) } }
    fun setSound(v: Boolean) { _sound.value = v; prefs.edit { putBoolean("sound", v) } }
    fun setPortrait(v: Boolean) { _portrait.value = v; prefs.edit { putBoolean("portrait", v) } }
    fun setHaptics(v: Boolean) { _haptics.value = v; prefs.edit { putBoolean("haptics", v) } }
}
