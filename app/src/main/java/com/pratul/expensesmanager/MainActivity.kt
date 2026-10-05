package com.pratul.expensesmanager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pratul.expensesmanager.data.ThemeMode
import com.pratul.expensesmanager.data.ThemeSetting
import com.pratul.expensesmanager.ui.ExpenseApp
import com.pratul.expensesmanager.ui.theme.ExpensesManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(ThemeSetting.load(this)) }
            val dark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Status bar and navigation bar icons follow the app's mode, not just the phone's
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
                onDispose {}
            }

            ExpensesManagerTheme(darkTheme = dark) {
                ExpenseApp(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        ThemeSetting.save(this, it)
                    }
                )
            }
        }
    }

    private companion object {
        // The same scrims enableEdgeToEdge() uses by default (seen only with 3-button navigation)
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
