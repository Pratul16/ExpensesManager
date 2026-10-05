package com.pratul.expensesmanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.auth.GoogleAuth
import com.pratul.expensesmanager.data.ProfileStore
import com.pratul.expensesmanager.ui.AppState
import com.pratul.expensesmanager.ui.components.ConfirmDialog
import com.pratul.expensesmanager.util.DeviceAuth
import com.pratul.expensesmanager.util.formatMoneyWhole
import kotlinx.coroutines.launch

private enum class Step { Warning, TypeDelete, Error }

/**
 * "Delete all my data": explains what goes, then asks for the phone's screen lock. Phones with no
 * screen lock have to type DELETE instead. [onWiped] lets the Profile page reset itself.
 */
@Composable
fun DeleteDataCard(state: AppState, onWiped: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<Step?>(null) }
    var errorText by remember { mutableStateOf("") }

    fun wipe() {
        state.wipeAll()
        ProfileStore.clear(context)
        scope.launch { GoogleAuth.signOut(context) }
        onWiped()
        step = null
        Toast.makeText(context, "All your data has been deleted", Toast.LENGTH_LONG).show()
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { step = Step.Warning }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Delete all my data", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                Text(
                    "Needs your phone's screen lock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    when (step) {
        null -> Unit
        Step.Warning -> ConfirmDialog(
            title = "Delete all your data?",
            message = "This permanently deletes:\n" +
                    "• all ${state.expenses.size} expenses (${formatMoneyWhole(state.expenses.sumOf { it.amount })})\n" +
                    "• your categories and items (they go back to the defaults)\n" +
                    "• everything in the Recycle bin\n" +
                    "• your name, birthday and photo\n\n" +
                    "It can't be undone and nothing goes to the Recycle bin. " +
                    "Export a backup first if you might want any of it later.",
            confirmLabel = "Continue",
            destructive = true,
            onDismiss = { step = null },
            onConfirm = {
                if (DeviceAuth.hasScreenLock(context)) {
                    step = null
                    DeviceAuth.confirm(
                        context,
                        title = "Confirm it's you",
                        subtitle = "Enter your screen lock to delete all your data",
                        onSuccess = ::wipe,
                        onCancelOrError = { message ->
                            if (message != null) {
                                errorText = message
                                step = Step.Error
                            }
                        }
                    )
                } else {
                    step = Step.TypeDelete
                }
            }
        )
        Step.TypeDelete -> TypeDeleteDialog(onDismiss = { step = null }, onConfirm = ::wipe)
        Step.Error -> ConfirmDialog(
            title = "Nothing was deleted",
            message = errorText,
            confirmLabel = "OK",
            onDismiss = { step = null },
            onConfirm = { step = null }
        )
    }
}

/** Fallback for phones without a screen lock: type DELETE to confirm. */
@Composable
private fun TypeDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type DELETE to confirm") },
        text = {
            Column {
                Text("Your phone has no screen lock, so type DELETE to confirm instead.")
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    placeholder = { Text("DELETE") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = typed.trim() == "DELETE",
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete everything") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
