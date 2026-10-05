package com.pratul.expensesmanager.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratul.expensesmanager.data.BillStore
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.formatMoney
import kotlinx.coroutines.launch

/** Attaches (file name) or removes (null) the bill of an expense. Provided by the app's root. */
val LocalSetBill = compositionLocalOf<(expenseId: Long, bill: String?) -> Unit> { { _, _ -> } }

/**
 * Optional bill for an expense: buttons to take a photo, pick a photo or pick a PDF; once attached,
 * a preview with View and Remove. New files are copied into the app before [onChange] is called.
 * [confirmRemove] asks before removing (for bills already saved with an expense).
 */
@Composable
fun BillPicker(bill: String?, onChange: (String?) -> Unit, confirmRemove: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf(false) }
    var askingRemove by remember { mutableStateOf(false) }
    // Survives the trip to the camera app and back
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun importFrom(uri: Uri) {
        busy = true
        scope.launch {
            try {
                onChange(BillStore.import(context, uri))
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't attach that bill", Toast.LENGTH_LONG).show()
            }
            busy = false
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) importFrom(Uri.parse(uri))
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importFrom(uri)
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFrom(uri)
    }

    Column {
        Text("🧾 Bill (optional)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        when {
            busy -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Attaching…", style = MaterialTheme.typography.bodySmall)
            }
            bill == null -> Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        val uri = BillStore.cameraTargetUri(context)
                        cameraUri = uri.toString()
                        try {
                            takePhoto.launch(uri)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    label = { Text("📷 Camera") }
                )
                AssistChip(
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    label = { Text("🖼️ Photo") }
                )
                AssistChip(onClick = { pickPdf.launch(arrayOf("application/pdf")) }, label = { Text("📄 PDF") })
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                BillThumbnail(bill, Modifier.size(56.dp).clickable { viewing = true })
                Spacer(Modifier.width(12.dp))
                Text(
                    if (BillStore.isPdf(bill)) "PDF bill attached" else "Photo bill attached",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewing = true }) { Text("View") }
                TextButton(onClick = { if (confirmRemove) askingRemove = true else onChange(null) }) { Text("✕") }
            }
        }
    }

    if (viewing && bill != null) BillViewer(bill, onClose = { viewing = false })
    if (askingRemove) {
        ConfirmDialog(
            title = "Remove this bill?",
            message = "The bill file will be deleted from the app. The expense itself stays.",
            confirmLabel = "Remove",
            destructive = true,
            onDismiss = { askingRemove = false },
            onConfirm = {
                askingRemove = false
                onChange(null)
            }
        )
    }
}

/** A small square preview: the photo itself, or a 📄 tile for PDFs. */
@Composable
fun BillThumbnail(bill: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val thumb by produceState<Bitmap?>(null, bill) { value = BillStore.thumbnail(context, bill) }
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val image = thumb
        if (image != null) {
            Image(image.asImageBitmap(), contentDescription = "Bill", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(if (BillStore.isPdf(bill)) "📄" else "🧾", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Shows a bill: photos full-screen with pinch-to-zoom (double-tap resets), PDFs in the phone's
 * PDF viewer.
 */
@Composable
fun BillViewer(bill: String, onClose: () -> Unit) {
    val context = LocalContext.current
    if (!BillStore.exists(context, bill)) {
        LaunchedEffect(bill) {
            Toast.makeText(context, "This bill's file is missing", Toast.LENGTH_SHORT).show()
            onClose()
        }
        return
    }
    if (BillStore.isPdf(bill)) {
        LaunchedEffect(bill) {
            if (!BillStore.openExternally(context, bill)) {
                Toast.makeText(context, "Install a PDF viewer to open this bill", Toast.LENGTH_LONG).show()
            }
            onClose()
        }
        return
    }

    val image by produceState<Bitmap?>(null, bill) { value = BillStore.fullImage(context, bill) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero })
                },
            contentAlignment = Alignment.Center
        ) {
            image?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = "Bill",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                )
            } ?: CircularProgressIndicator()
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(8.dp)) {
                Text("✕ Close", color = Color.White)
            }
        }
    }
}

/** Everything about one expense, with its bill (view, attach, or remove). */
@Composable
fun ExpenseDetailsDialog(expense: Expense, onDismiss: () -> Unit) {
    val setBill = LocalSetBill.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { CategoryBadge(expense.category, 56.dp) },
        title = { Text(expense.item.ifBlank { expense.category }) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatMoney(expense.amount), style = MaterialTheme.typography.headlineMedium)
                Text(
                    listOfNotNull(expense.category.takeIf { expense.item.isNotBlank() }, formatDate(expense.dateMillis))
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.note.isNotBlank()) Text("📝 ${expense.note}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                BillPicker(bill = expense.bill.ifBlank { null }, onChange = { setBill(expense.id, it) }, confirmRemove = true)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
