package com.pratul.expensesmanager.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.auth.GoogleAuth
import com.pratul.expensesmanager.auth.SignInResult
import com.pratul.expensesmanager.data.Profile
import com.pratul.expensesmanager.data.ProfileStore
import com.pratul.expensesmanager.data.ThemeMode
import com.pratul.expensesmanager.ui.AppState
import com.pratul.expensesmanager.ui.components.GradientCard
import com.pratul.expensesmanager.ui.theme.isAppDark
import com.pratul.expensesmanager.util.ageFrom
import com.pratul.expensesmanager.util.formatPickerDate
import kotlinx.coroutines.launch

private enum class AccountPage { Home, Categories, Bin }

@Composable
fun AccountScreen(state: AppState, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    var page by rememberSaveable { mutableStateOf(AccountPage.Home) }
    BackHandler(enabled = page != AccountPage.Home) { page = AccountPage.Home }

    when (page) {
        AccountPage.Home -> AccountHome(state, themeMode, onThemeModeChange, onOpen = { page = it })
        AccountPage.Categories -> ManageCategoriesScreen(state, onBack = { page = AccountPage.Home })
        AccountPage.Bin -> RecycleBinScreen(state, onBack = { page = AccountPage.Home })
    }
}

@Composable
private fun AccountHome(
    state: AppState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpen: (AccountPage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(ProfileStore.load(context)) }
    var editing by remember { mutableStateOf(false) }
    // Bumped after "Delete all my data" so cards that remember things start fresh
    var wipeCount by remember { mutableIntStateOf(0) }

    fun saveProfile(p: Profile) {
        profile = p
        ProfileStore.save(context, p)
    }

    val photo by produceState<Bitmap?>(null, profile.photoVersion) {
        value = ProfileStore.loadPhoto(context)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                if (ProfileStore.savePhoto(context, uri)) {
                    saveProfile(profile.copy(photoVersion = System.currentTimeMillis()))
                } else {
                    Toast.makeText(context, "Couldn't open that picture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun choosePhoto() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hey${profile.name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""} 👋",
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall
            )
            ThemeMenu(themeMode, onThemeModeChange)
        }

        // ---------- Profile ----------
        GradientCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(photo, profile.name, 112.dp, onClick = ::choosePhoto)
                Spacer(Modifier.height(16.dp))
                Text(
                    profile.name.ifBlank { "Add your name" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = if (profile.name.isBlank()) 0.75f else 1f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.dobUtcMillis?.let { "🎂 ${formatPickerDate(it)} · ${ageFrom(it)} years" }
                        ?: "🎂 Birthday not set",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { editing = true },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Edit profile") }
            }
        }

        // ---------- Data ----------
        Card(Modifier.fillMaxWidth()) {
            val itemCount = state.catalog.sumOf { it.items.size }
            MenuRow(
                icon = R.drawable.ic_category,
                title = "Categories & items",
                subtitle = "${state.catalog.size} categories · $itemCount items · rename or delete",
                onClick = { onOpen(AccountPage.Categories) }
            )
            HorizontalDivider(Modifier.padding(start = 56.dp))
            MenuRow(
                icon = R.drawable.ic_delete,
                title = "Recycle bin",
                subtitle = if (state.bin.isEmpty()) "Empty" else "${state.bin.size} deleted · tap to restore",
                onClick = { onOpen(AccountPage.Bin) }
            )
        }

        // ---------- Backup and export ----------
        DataCard(state, onProfileReplaced = { profile = ProfileStore.load(context) })

        // ---------- Google (optional) ----------
        key(wipeCount) {
            GoogleAccountCard(
                onSignedIn = { name -> if (profile.name.isBlank()) saveProfile(profile.copy(name = name)) }
            )
        }

        // ---------- Danger zone ----------
        DeleteDataCard(state, onWiped = {
            profile = ProfileStore.load(context)
            wipeCount++
        })
    }

    if (editing) {
        EditProfileDialog(
            profile = profile,
            hasPhoto = photo != null,
            onChangePhoto = ::choosePhoto,
            onRemovePhoto = {
                ProfileStore.deletePhoto(context)
                saveProfile(profile.copy(photoVersion = System.currentTimeMillis()))
            },
            onDismiss = { editing = false },
            onSave = { name, dob ->
                saveProfile(profile.copy(name = name, dobUtcMillis = dob))
                editing = false
            }
        )
    }
}

/** Sun or moon button that opens System default / Light / Dark. */
@Composable
private fun ThemeMenu(mode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painterResource(if (isAppDark()) R.drawable.ic_dark_mode else R.drawable.ic_light_mode),
                contentDescription = "Light or dark mode"
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in ThemeMode.entries) {
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = when (option) {
                        ThemeMode.Light -> ({ Icon(painterResource(R.drawable.ic_light_mode), null) })
                        ThemeMode.Dark -> ({ Icon(painterResource(R.drawable.ic_dark_mode), null) })
                        ThemeMode.System -> ({ Icon(painterResource(R.drawable.ic_brightness_auto), null) })
                    },
                    trailingIcon = if (option == mode) ({ Text("✓") }) else null,
                    onClick = {
                        onChange(option)
                        open = false
                    }
                )
            }
        }
    }
}

/** Round photo, or the name's first letter when there's no photo, with a small camera badge. */
@Composable
private fun Avatar(photo: Bitmap?, name: String, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clickable(onClick = onClick)) {
        val circle = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = circle
            )
        } else {
            Box(circle.background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text(
                    name.trim().take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_camera),
                contentDescription = "Change picture",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MenuRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    profile: Profile,
    hasPhoto: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, dobUtcMillis: Long?) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var dob by remember { mutableStateOf(profile.dobUtcMillis) }
    var pickingDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date of birth: " + (dob?.let { formatPickerDate(it) } ?: "not set"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onChangePhoto, modifier = Modifier.weight(1f)) {
                        Text(if (hasPhoto) "Change photo" else "Add photo")
                    }
                    if (hasPhoto) {
                        OutlinedButton(onClick = onRemovePhoto, modifier = Modifier.weight(1f)) { Text("Remove photo") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim(), dob) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingDate) {
        val now = System.currentTimeMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dob,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= now
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
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Google sign-in, unchanged for now apart from its layout. */
@Composable
private fun GoogleAccountCard(onSignedIn: (name: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(GoogleAuth.savedAccount(context)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Google account (optional)", fontWeight = FontWeight.SemiBold)
            Text(
                "The app works fully without signing in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val signedIn = account
            if (signedIn != null) {
                Text(signedIn.email, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        GoogleAuth.signOut(context)
                        account = null
                    }
                }) { Text("Sign out") }
            } else if (busy) {
                CircularProgressIndicator(Modifier.size(32.dp))
            } else {
                Button(
                    onClick = {
                        error = null
                        busy = true
                        scope.launch {
                            when (val result = GoogleAuth.signIn(context)) {
                                is SignInResult.Success -> {
                                    account = result.account
                                    onSignedIn(result.account.name)
                                }
                                is SignInResult.Failed -> error = result.message
                                SignInResult.Cancelled -> Unit
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sign in with Google") }
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start)
            }
        }
    }
}
