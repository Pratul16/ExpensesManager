package com.pratul.expensesmanager.ui.lock

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.security.AppLock
import com.pratul.expensesmanager.security.LockMethod
import com.pratul.expensesmanager.ui.components.heroBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Asks the phone to confirm it's the owner: fingerprint, face, or the phone's PIN / pattern /
 * password. [onResult] gets true on success.
 */
fun Activity.confirmWithPhoneLock(title: String, subtitle: String? = null, onResult: (Boolean) -> Unit) {
    val builder = BiometricPrompt.Builder(this).setTitle(title)
    subtitle?.let(builder::setSubtitle)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    } else {
        @Suppress("DEPRECATION")
        builder.setDeviceCredentialAllowed(true)
    }
    runCatching {
        builder.build().authenticate(
            CancellationSignal(),
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            },
        )
    }.onFailure { onResult(false) }
}

/** Full-screen brand gradient with white content, used by the lock and PIN screens. */
@Composable
fun LockBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(heroBrush())) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

/** Shows [content], covered by the lock screen while the app is locked. */
@Composable
fun AppLockGate(lock: AppLock, content: @Composable () -> Unit) {
    val locked by lock.locked.collectAsState()
    Box(Modifier.fillMaxSize()) {
        content()
        if (locked) LockScreen(lock)
    }
}

@Composable
private fun LockScreen(lock: AppLock) {
    val config by lock.config.collectAsState()
    val activity = LocalActivity.current
    val usePhone = config.method == LockMethod.SYSTEM || !config.hasPin
    // Never trap the owner: with "phone lock" chosen but no lock on the phone, there's nothing to check.
    LaunchedEffect(usePhone) { if (usePhone && !lock.isDeviceSecure()) lock.unlock() }

    fun askPhone() {
        activity?.confirmWithPhoneLock("Unlock Expenses Manager") { ok -> if (ok) lock.unlock() }
    }
    LaunchedEffect(Unit) { if (usePhone && lock.isDeviceSecure()) askPhone() }
    // Back leaves the app instead of slipping past the lock.
    BackHandler { activity?.moveTaskToBack(true) }

    LockBackground(
        Modifier
            .fillMaxSize()
            // Swallow every touch so nothing underneath can be used.
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppLogo(Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Locked", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            if (usePhone) {
                Text(
                    "Use your fingerprint, face or phone PIN to continue.",
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = ::askPhone,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF5B21B6))
                ) {
                    Icon(painterResource(R.drawable.ic_lock), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock")
                }
            } else {
                PinEntry(
                    title = "Enter your app PIN",
                    verify = { lock.verifyPin(it) },
                    waitMs = lock::pinWaitMs,
                    onSuccess = lock::unlock,
                )
                if (lock.isDeviceSecure()) {
                    TextButton(onClick = ::askPhone) { Text("Forgot PIN? Use phone lock", color = Color.White) }
                }
            }
        }
    }
}

/** The app icon's two layers in a rounded square. */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
    }
}

/**
 * Dots + keypad. [verify] checks a typed PIN; a wrong one shakes the dots. [waitMs] reports any
 * enforced wait after too many wrong tries.
 */
@Composable
fun PinEntry(
    title: String,
    verify: suspend (String) -> Boolean,
    onSuccess: () -> Unit,
    waitMs: () -> Long = { 0L },
    subtitle: String? = null,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var wait by remember { mutableIntStateOf((waitMs() / 1000).toInt()) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(wait) {
        if (wait > 0) {
            delay(1000)
            wait = (waitMs() / 1000).toInt()
        }
    }

    fun submit() {
        if (pin.length < MIN_PIN || busy || wait > 0) return
        busy = true
        scope.launch {
            if (verify(pin)) {
                onSuccess()
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                pin = ""
                wait = (waitMs() / 1000).toInt()
                error = if (wait > 0) null else "Wrong PIN"
                for (x in listOf(24f, -20f, 14f, -8f, 0f)) shake.animateTo(x, spring(stiffness = 4000f))
            }
            busy = false
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center) }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .height(18.dp)
                .offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(maxOf(MIN_PIN, pin.length)) { i -> PinDot(filled = i < pin.length) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                wait > 0 -> "Too many tries. Try again in ${wait}s"
                error != null -> error!!
                else -> " "
            },
            color = Color(0xFFFFE4E6),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        Keypad(
            enabled = wait == 0 && !busy,
            canSubmit = pin.length >= MIN_PIN,
            onDigit = {
                if (pin.length < MAX_PIN) {
                    pin += it
                    error = null
                }
            },
            onDelete = { pin = pin.dropLast(1) },
            onSubmit = ::submit,
        )
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    val scale by animateFloatAsState(if (filled) 1f else 0.7f, spring(dampingRatio = 0.45f), label = "dot")
    Box(
        Modifier
            .size(14.dp)
            .scale(scale)
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(Color.White)
                else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            ),
    )
}

@Composable
private fun Keypad(enabled: Boolean, canSubmit: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf("123", "456", "789", "<0>").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { key ->
                    KeyButton(enabled = enabled && (key != '>' || canSubmit), onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        when (key) {
                            '<' -> onDelete()
                            '>' -> onSubmit()
                            else -> onDigit(key)
                        }
                    }) {
                        when (key) {
                            '<' -> Icon(painterResource(R.drawable.ic_backspace), contentDescription = "Delete")
                            '>' -> Icon(painterResource(R.drawable.ic_check), contentDescription = "Done")
                            else -> Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(enabled: Boolean, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.18f else 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Full-screen "choose a PIN, then confirm it" flow. */
@Composable
fun PinSetupDialog(onDismiss: () -> Unit, onPinChosen: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        LockBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(painterResource(R.drawable.ic_lock), contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(20.dp))
                // Re-key so the keypad starts empty for the second step.
                androidx.compose.runtime.key(first) {
                    PinEntry(
                        title = if (first == null) "Choose an app PIN" else "Confirm your PIN",
                        subtitle = when {
                            mismatch -> "PINs didn't match. Try again."
                            first == null -> "4 to 8 digits"
                            else -> "Enter it once more"
                        },
                        verify = { pin ->
                            val chosen = first
                            when {
                                chosen == null -> {
                                    first = pin
                                    mismatch = false
                                    true
                                }
                                chosen == pin -> {
                                    onPinChosen(pin)
                                    true
                                }
                                else -> {
                                    first = null
                                    mismatch = true
                                    true
                                }
                            }
                        },
                        onSuccess = {},
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
            }
        }
    }
}

/** Full-screen "enter your app PIN" check used before sensitive settings. */
@Composable
fun PinVerifyDialog(lock: AppLock, reason: String, onDismiss: () -> Unit, onVerified: () -> Unit) {
    val activity = LocalActivity.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        LockBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PinEntry(title = "Enter your app PIN", subtitle = reason, verify = { lock.verifyPin(it) }, waitMs = lock::pinWaitMs, onSuccess = onVerified)
                if (lock.isDeviceSecure()) {
                    TextButton(onClick = { activity?.confirmWithPhoneLock(reason) { if (it) onVerified() } }) {
                        Text("Forgot PIN? Use phone lock", color = Color.White)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
            }
        }
    }
}

/**
 * Confirms the owner before a sensitive action (turning the lock off, changing how it unlocks):
 * with the app PIN when that's the lock method, otherwise with the phone lock. On a phone with
 * no lock at all and no app PIN, there is nothing to check and the action runs directly.
 */
class OwnerCheck internal constructor(private val run: (String, () -> Unit) -> Unit) {
    fun confirm(reason: String, onConfirmed: () -> Unit) = run(reason, onConfirmed)
}

@Composable
fun rememberOwnerCheck(lock: AppLock): OwnerCheck {
    val activity = LocalActivity.current
    val config by lock.config.collectAsState()
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    pending?.let { (reason, then) ->
        PinVerifyDialog(lock, reason, onDismiss = { pending = null }, onVerified = {
            pending = null
            then()
        })
    }
    return OwnerCheck { reason, then ->
        when {
            config.enabled && config.method == LockMethod.PIN && config.hasPin -> pending = reason to then
            lock.isDeviceSecure() && activity != null -> activity.confirmWithPhoneLock(reason) { if (it) then() }
            else -> then()
        }
    }
}

private const val MIN_PIN = 4
private const val MAX_PIN = 8
