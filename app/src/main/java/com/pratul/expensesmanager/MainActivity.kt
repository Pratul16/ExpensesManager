package com.pratul.expensesmanager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pratul.expensesmanager.data.ThemeMode
import com.pratul.expensesmanager.data.ThemeSetting
import com.pratul.expensesmanager.ui.ExpenseApp
import com.pratul.expensesmanager.ui.components.AppBackground
import com.pratul.expensesmanager.ui.lock.AppLockGate
import com.pratul.expensesmanager.ui.screens.FirstRun
import com.pratul.expensesmanager.ui.screens.IntroScreen
import com.pratul.expensesmanager.ui.screens.OnboardingScreen
import com.pratul.expensesmanager.ui.theme.ExpensesManagerTheme

private enum class Stage { Intro, Onboarding, App }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(ThemeSetting.load(this)) }
            var appStyle by remember { mutableStateOf(ThemeSetting.loadStyle(this)) }
            val dark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // The logo animation plays once per launch (not again after rotating the phone)
            var stage by rememberSaveable { mutableStateOf(Stage.Intro) }

            // Status bar and navigation bar icons follow the app's mode, not just the phone's.
            // The intro has a dark gradient, so it always uses light icons.
            val darkBars = dark || stage == Stage.Intro
            DisposableEffect(darkBars) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkBars },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkBars }
                )
                onDispose {}
            }

            ExpensesManagerTheme(darkTheme = dark, style = appStyle) {
                Crossfade(targetState = stage, animationSpec = tween(450), label = "stage") { current ->
                    when (current) {
                        Stage.Intro -> IntroScreen(onFinished = {
                            if (stage == Stage.Intro) stage = if (FirstRun.isDone(this)) Stage.App else Stage.Onboarding
                        })
                        Stage.Onboarding -> OnboardingScreen(onDone = {
                            FirstRun.markDone(this)
                            stage = Stage.App
                        })
                        Stage.App -> AppLockGate(ExpensesApp.lock(this)) {
                            AppBackground(Modifier.fillMaxSize()) {
                                ExpenseApp(
                                    themeMode = themeMode,
                                    onThemeModeChange = {
                                        themeMode = it
                                        ThemeSetting.save(this, it)
                                    },
                                    appStyle = appStyle,
                                    onAppStyleChange = {
                                        appStyle = it
                                        ThemeSetting.saveStyle(this, it)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        // The same scrims enableEdgeToEdge() uses by default (seen only with 3-button navigation)
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
