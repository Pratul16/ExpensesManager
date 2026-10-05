package com.pratul.expensesmanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Asks for a name. Rejects blanks and names already in [existing] (ignoring case),
 * except [initial] itself so a rename can just change capitalisation.
 */
@Composable
fun NameDialog(
    title: String,
    confirmLabel: String,
    existing: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initial: String = ""
) {
    var name by remember { mutableStateOf(initial) }
    val trimmed = name.trim()
    val duplicate = existing.any { it.equals(trimmed, ignoreCase = true) && !it.equals(initial, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) ({ Text("Already in the list") }) else null,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed.replaceFirstChar { it.uppercase() }) },
                enabled = trimmed.isNotEmpty() && !duplicate && trimmed != initial
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** A yes/no question. `destructive` colours the confirm button red. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else ButtonDefaults.textButtonColors()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Two warnings in a row before something is deleted. Cancelling either one stops it. */
@Composable
fun DoubleConfirmDialog(
    firstTitle: String,
    firstMessage: String,
    secondTitle: String,
    secondMessage: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    if (step == 1) {
        ConfirmDialog(firstTitle, firstMessage, "Continue", onDismiss, { step = 2 }, destructive = true)
    } else {
        ConfirmDialog(secondTitle, secondMessage, confirmLabel, onDismiss, onConfirm, destructive = true)
    }
}
