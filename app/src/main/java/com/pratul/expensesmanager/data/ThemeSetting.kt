package com.pratul.expensesmanager.data

import android.content.Context

enum class ThemeMode(val label: String) { System("System default"), Light("Light"), Dark("Dark") }

object ThemeSetting {
    private const val PREFS = "settings"
    private const val KEY = "theme"

    fun load(context: Context): ThemeMode {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.System
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }
}
