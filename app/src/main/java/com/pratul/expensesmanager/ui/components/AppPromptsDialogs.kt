package com.pratul.expensesmanager.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.review.ReviewManagerFactory
import com.pratul.expensesmanager.billing.TipJar
import com.pratul.expensesmanager.billing.TipJarState
import com.pratul.expensesmanager.billing.TipOption
import com.pratul.expensesmanager.util.CurrencySetting

enum class PromptType { Rating, TipCoffee }

object AppPromptsManager {
    private const val PREFS = "app_prompts"
    private const val K_COUNTER = "launch_counter"

    fun shouldShowPrompt(context: Context): PromptType? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(K_COUNTER, 0) + 1
        prefs.edit().putInt(K_COUNTER, count).apply()

        // Prompt 1 in every 20 usages
        if (count > 0 && count % 20 == 0) {
            return if ((count / 20) % 2 == 1) PromptType.Rating else PromptType.TipCoffee
        }
        return null
    }
}

fun launchPlayStoreRating(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        } else {
            // Direct Play Store fallback
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}"))
            try {
                activity.startActivity(intent)
            } catch (_: Exception) {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
            }
        }
    }
}

@Composable
fun PlayStoreRatingDialog(
    activity: Activity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Enjoying Expenses Manager? ⭐", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "If you think our app is helping you, kindly rate us on the Play Store. Your feedback helps others find our app!",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    launchPlayStoreRating(activity)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Rate Us on Play Store")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
fun BuyCoffeePromptDialog(
    activity: Activity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tipJar = remember { TipJar(context) }

    LaunchedEffect(Unit) {
        tipJar.connect()
    }

    AlertDialog(
        onDismissRequest = {
            tipJar.close()
            onDismiss()
        },
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Keep Expenses Manager Free ☕", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    "To keep this app 100% free and ad-free, kindly help us with a coffee if you find it helpful!",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))

                when (val state = tipJar.state) {
                    is TipJarState.Ready -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.options.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            tipJar.buy(activity, option)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.emoji, fontSize = 22.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(option.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        option.price,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Fallback offline coffee options UI
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoffeeFallbackOption("☕", "Coffee", "${CurrencySetting.currentSymbol}50") {
                                onDismiss()
                                openPlayStoreUrl(activity)
                            }
                            CoffeeFallbackOption("🍰", "Coffee & Cake", "${CurrencySetting.currentSymbol}100") {
                                onDismiss()
                                openPlayStoreUrl(activity)
                            }
                            CoffeeFallbackOption("🎉", "Big Thanks", "${CurrencySetting.currentSymbol}200") {
                                onDismiss()
                                openPlayStoreUrl(activity)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    tipJar.close()
                    onDismiss()
                }
            ) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
private fun CoffeeFallbackOption(emoji: String, title: String, price: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
        Text(price, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private fun openPlayStoreUrl(activity: Activity) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}"))
    try {
        activity.startActivity(intent)
    } catch (e: Exception) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
    }
}
