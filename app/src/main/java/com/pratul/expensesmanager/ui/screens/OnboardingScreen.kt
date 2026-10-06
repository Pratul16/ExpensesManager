package com.pratul.expensesmanager.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.data.Profile
import com.pratul.expensesmanager.data.ProfileStore
import com.pratul.expensesmanager.ui.components.AppBackground
import com.pratul.expensesmanager.ui.components.heroBrush
import com.pratul.expensesmanager.ui.components.rememberCircleCropPicker
import com.pratul.expensesmanager.util.formatPickerDate
import kotlinx.coroutines.launch

/** Whether the first-time profile setup has been shown (or wasn't needed). */
object FirstRun {
    private const val PREFS = "settings"
    private const val KEY = "onboarded"

    /** Done once shown, or for people who already had a profile before this screen existed. */
    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false) ||
                ProfileStore.load(context).name.isNotBlank()

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

private const val STEPS = 4 // welcome, name, birthday, photo

/**
 * First-time profile setup, one question per screen. Everything is optional: "Skip" keeps whatever
 * was filled in so far and goes straight to the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf<Long?>(null) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var pickingDate by remember { mutableStateOf(false) }

    fun finish() {
        val hasPhoto = photo != null
        if (name.isNotBlank() || dob != null || hasPhoto) {
            ProfileStore.save(context, Profile(name.trim(), dob, if (hasPhoto) System.currentTimeMillis() else 0))
        }
        onDone()
    }

    val pickPhoto = rememberCircleCropPicker { cropped ->
        scope.launch {
            if (ProfileStore.savePhotoBitmap(context, cropped)) photo = cropped
            else Toast.makeText(context, "Couldn't save that picture", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = step > 0) { step-- }

    AppBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Progress and Skip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(STEPS) { i ->
                        val filled by animateFloatAsState(if (i <= step) 1f else 0f, tween(300), label = "bar")
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(filled)
                                    .height(6.dp)
                                    .background(heroBrush())
                            )
                        }
                    }
                }
                TextButton(onClick = ::finish) { Text("Skip") }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "steps"
            ) { s ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (s) {
                        0 -> {
                            BigEmoji("👋")
                            StepTitle("Welcome to\nExpenses Manager")
                            StepText("Let's set up your profile. It takes about 20 seconds, and you can change it any time.")
                        }
                        1 -> {
                            BigEmoji("✍️")
                            StepTitle("What should we\ncall you?")
                            Spacer(Modifier.height(24.dp))
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Your name", style = TextStyle(fontSize = 24.sp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        2 -> {
                            BigEmoji("🎂")
                            StepTitle("When's your\nbirthday?")
                            StepText("Just for your profile. It stays on this phone.")
                            Spacer(Modifier.height(24.dp))
                            OutlinedButton(onClick = { pickingDate = true }, shape = RoundedCornerShape(16.dp)) {
                                Text(
                                    dob?.let { formatPickerDate(it) } ?: "Pick a date",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        else -> {
                            StepTitle(if (photo == null) "Add a profile\nphoto" else "Looking good! ✨")
                            Spacer(Modifier.height(28.dp))
                            Box(
                                Modifier
                                    .size(170.dp)
                                    .clip(CircleShape)
                                    .background(heroBrush())
                                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clickable(onClick = pickPhoto),
                                contentAlignment = Alignment.Center
                            ) {
                                val p = photo
                                if (p != null) {
                                    Image(p.asImageBitmap(), "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(painterResource(R.drawable.ic_avatar_placeholder), null, tint = Color.White, modifier = Modifier.size(110.dp))
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            TextButton(onClick = pickPhoto) { Text(if (photo == null) "📷 Choose a photo" else "Change photo") }
                        }
                    }
                }
            }

            // Back / Next
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (step < STEPS - 1) step++ else finish() },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        when (step) {
                            0 -> "Let's go"
                            STEPS - 1 -> if (photo == null && name.isBlank()) "Finish" else "Start tracking 🚀"
                            else -> "Next"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    if (pickingDate) {
        val today = System.currentTimeMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dob,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dob = pickerState.selectedDateMillis ?: dob
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun BigEmoji(emoji: String) {
    Text(emoji, style = TextStyle(fontSize = 72.sp))
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
}

@Composable
private fun StepText(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
