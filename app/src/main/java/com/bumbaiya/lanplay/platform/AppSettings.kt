package com.bumbaiya.lanplay.platform

import android.content.Context
import com.bumbaiya.lanplay.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The user's own preferences, kept on the phone: name, theme and sound. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lanplay", Context.MODE_PRIVATE)

    private val _name = MutableStateFlow(prefs.getString("name", "") ?: "")
    private val _theme = MutableStateFlow(runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: "") }.getOrDefault(ThemeMode.System))
    private val _sound = MutableStateFlow(prefs.getBoolean("sound", true))
    private val _haptics = MutableStateFlow(prefs.getBoolean("haptics", true))

    val name: StateFlow<String> = _name
    val theme: StateFlow<ThemeMode> = _theme
    val sound: StateFlow<Boolean> = _sound
    val haptics: StateFlow<Boolean> = _haptics

    fun setName(v: String) { _name.value = v; prefs.edit().putString("name", v).apply() }
    fun setTheme(v: ThemeMode) { _theme.value = v; prefs.edit().putString("theme", v.name).apply() }
    fun setSound(v: Boolean) { _sound.value = v; prefs.edit().putBoolean("sound", v).apply() }
    fun setHaptics(v: Boolean) { _haptics.value = v; prefs.edit().putBoolean("haptics", v).apply() }
}
