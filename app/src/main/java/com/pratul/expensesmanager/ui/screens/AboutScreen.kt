package com.pratul.expensesmanager.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.billing.TipJar
import com.pratul.expensesmanager.billing.TipJarState
import com.pratul.expensesmanager.ui.components.GradientCard
import com.pratul.expensesmanager.util.findActivity

/** App version and the developer's details, from res/values/strings.xml. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = appVersion(context)
    val developer = stringResource(R.string.developer_name)
    val email = stringResource(R.string.developer_email)
    val tagline = stringResource(R.string.developer_tagline)
    val privacyUrl = stringResource(R.string.privacy_policy_url)

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("About & developer", onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GradientCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // The launcher icon's two layers, so this page matches what's on the home screen
                    Box(Modifier.size(88.dp).clip(RoundedCornerShape(24.dp))) {
                        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Text("Version $version", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Track · Compare · Wrapped",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (developer.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Developer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(developer, style = MaterialTheme.typography.titleLarge)
                        if (tagline.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            TipCard()

            Card(Modifier.fillMaxWidth()) {
                if (email.isNotBlank()) {
                    MenuRow(
                        icon = R.drawable.ic_mail,
                        title = "Contact the developer",
                        subtitle = email,
                        onClick = { sendEmail(context, email, "Expenses Manager", "") }
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                    MenuRow(
                        icon = R.drawable.ic_feedback,
                        title = "Send feedback or report a problem",
                        subtitle = "Opens an email with your app and Android version",
                        onClick = {
                            sendEmail(
                                context, email, "Expenses Manager feedback (v$version)",
                                "\n\n—\nApp version: $version\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                        "Phone: ${Build.MANUFACTURER} ${Build.MODEL}"
                            )
                        }
                    )
                }
                if (privacyUrl.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                    MenuRow(
                        icon = R.drawable.ic_info,
                        title = "Privacy policy",
                        subtitle = "Your data stays on your phone",
                        onClick = { openLink(context, privacyUrl) }
                    )
                }
            }

            Text(
                "No ads · No tracking · Your data stays on this phone",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

/** The app's tip jar, provided by the app's root. Null in previews. */
val LocalTipJar = compositionLocalOf<TipJar?> { null }

/** "Buy me a coffee": tip sizes with Google Play prices, or why tips aren't available here. */
@Composable
private fun TipCard() {
    val tipJar = LocalTipJar.current ?: return
    // Reconnect each time the page opens, in case Google Play was unavailable earlier
    LaunchedEffect(tipJar) { tipJar.connect() }
    val context = LocalContext.current

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Buy me a coffee ☕", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Enjoying the app? A small tip keeps it ad-free and growing. Totally optional, and every feature stays free.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            when (val state = tipJar.state) {
                TipJarState.Loading -> Text("Loading prices…", style = MaterialTheme.typography.bodySmall)
                is TipJarState.Unavailable -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is TipJarState.Ready -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in state.options) {
                        FilledTonalButton(
                            onClick = { context.findActivity()?.let { tipJar.buy(it, option) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(option.emoji, style = MaterialTheme.typography.titleLarge)
                                Text(option.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
                                Text(option.price, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (tipJar.hasPendingTip) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "⏳ A tip is waiting for payment approval. It completes automatically once approved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (tipJar.completedTips > 0) {
                Spacer(Modifier.height(8.dp))
                Text("💜 Thank you for your support!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun appVersion(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

private fun sendEmail(context: Context, to: String, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found. Write to $to", Toast.LENGTH_LONG).show()
    }
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
    }
}
