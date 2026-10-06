package com.pratul.expensesmanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.ExpensesApp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.security.LockDelay
import com.pratul.expensesmanager.security.LockMethod
import com.pratul.expensesmanager.ui.lock.PinSetupDialog
import com.pratul.expensesmanager.ui.lock.confirmWithPhoneLock
import com.pratul.expensesmanager.ui.lock.rememberOwnerCheck
import kotlinx.coroutines.launch

private enum class PinPurpose { ENABLE, SWITCH, CHANGE }

/**
 * App lock settings: on/off (off by default), unlock with the phone's lock or an app PIN, and how
 * long the app may stay in the background before locking. Turning it off or changing how it
 * unlocks needs the owner's confirmation first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityCard() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lock = ExpensesApp.lock(context)
    val config by lock.config.collectAsState()
    val ownerCheck = rememberOwnerCheck(lock)
    val scope = rememberCoroutineScope()
    // Non-null while choosing a PIN; the value says what to do once it's set.
    var pinFor by remember { mutableStateOf<PinPurpose?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    fun turnOn() {
        if (lock.isDeviceSecure() && activity != null) {
            activity.confirmWithPhoneLock("Turn on app lock", "Confirm it's you") { ok ->
                if (ok) {
                    lock.enable(LockMethod.SYSTEM)
                    toast("App lock is on 🔒")
                }
            }
        } else {
            // No screen lock on the phone: an app PIN is the only option.
            pinFor = PinPurpose.ENABLE
        }
    }

    fun turnOff() = ownerCheck.confirm("Turn off app lock") {
        lock.disable()
        toast("App lock is off")
    }

    fun useMethod(method: LockMethod) {
        if (method == config.method) return
        when (method) {
            LockMethod.PIN -> ownerCheck.confirm("Change how the app unlocks") { pinFor = PinPurpose.SWITCH }
            LockMethod.SYSTEM -> if (!lock.isDeviceSecure()) {
                toast("Set a screen lock in your phone's settings first")
            } else {
                activity?.confirmWithPhoneLock("Unlock with phone lock", "Confirm it's you") { ok -> if (ok) lock.setMethod(LockMethod.SYSTEM) }
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { if (config.enabled) turnOff() else turnOn() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("App lock", fontWeight = FontWeight.SemiBold)
                Text(
                    if (config.enabled) "Asks for ${if (config.method == LockMethod.PIN) "your app PIN" else "fingerprint, face or phone PIN"} when opened"
                    else "Off · anyone with your phone can open the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = config.enabled, onCheckedChange = { on -> if (on) turnOn() else turnOff() })
        }
        if (config.enabled) {
            HorizontalDivider(Modifier.padding(start = 56.dp))
            Column(Modifier.padding(start = 56.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
                Text("Unlock with", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockMethod.entries.forEach { method ->
                        FilterChip(
                            selected = config.method == method,
                            onClick = { useMethod(method) },
                            label = { Text(if (method == LockMethod.SYSTEM) "Fingerprint / phone lock" else "App PIN") },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("Lock after leaving the app", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockDelay.entries.forEach { delay ->
                        FilterChip(selected = config.delay == delay, onClick = { lock.setDelay(delay) }, label = { Text(delay.label) })
                    }
                }
            }
            if (config.method == LockMethod.PIN) {
                HorizontalDivider(Modifier.padding(start = 56.dp))
                MenuRow(
                    icon = R.drawable.ic_pin,
                    title = "Change app PIN",
                    subtitle = "4 to 8 digits",
                    onClick = { ownerCheck.confirm("Change app PIN") { pinFor = PinPurpose.CHANGE } }
                )
            }
        }
    }

    pinFor?.let { purpose ->
        PinSetupDialog(
            onDismiss = { pinFor = null },
            onPinChosen = { pin ->
                pinFor = null
                scope.launch {
                    lock.setPin(pin)
                    when (purpose) {
                        PinPurpose.ENABLE -> {
                            lock.enable(LockMethod.PIN)
                            toast("App lock is on 🔒")
                        }
                        PinPurpose.SWITCH -> {
                            lock.setMethod(LockMethod.PIN)
                            toast("The app now unlocks with your PIN")
                        }
                        PinPurpose.CHANGE -> toast("PIN changed")
                    }
                }
            },
        )
    }
}
