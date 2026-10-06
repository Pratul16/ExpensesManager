package com.pratul.expensesmanager.data

import android.content.Context

enum class ThemeMode(val label: String) { System("System default"), Light("Light"), Dark("Dark") }

/** The app's overall look. Each one works in both light and dark mode. */
enum class AppStyle(val label: String, val description: String) {
    Vibrant("Vibrant", "Violet and pink, the original"),
    Simple("Simple", "Plain, high contrast and easy to read"),
    Glass("Glass", "Frosted see-through cards"),
    Ocean("Ocean", "Calm blues and teals"),
    Sunset("Sunset", "Warm orange and coral")
}

object ThemeSetting {
    private const val PREFS = "settings"
    private const val KEY = "theme"
    private const val STYLE_KEY = "style"

    fun load(context: Context): ThemeMode {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.System
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }

    fun loadStyle(context: Context): AppStyle {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(STYLE_KEY, null)
        return AppStyle.entries.firstOrNull { it.name == saved } ?: AppStyle.Vibrant
    }

    fun saveStyle(context: Context, style: AppStyle) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(STYLE_KEY, style.name).apply()
    }
}
