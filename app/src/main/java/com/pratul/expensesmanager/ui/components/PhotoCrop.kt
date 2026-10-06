package com.pratul.expensesmanager.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratul.expensesmanager.expectExternalReturn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Largest side the picked photo is loaded at for cropping; plenty for a 512 px profile picture. */
private const val LOAD_MAX_SIDE = 2048
/** Size of the saved, cropped profile picture. */
private const val OUTPUT_SIZE = 512

/**
 * Lets the user pick a photo and crop it to a circle. Returns a launcher function; [onCropped]
 * receives a square bitmap whose inscribed circle is what the user framed.
 */
@Composable
fun rememberCircleCropPicker(onCropped: (Bitmap) -> Unit): () -> Unit {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> picked = uri }

    picked?.let { uri ->
        CircleCropDialog(
            uri = uri,
            onDismiss = { picked = null },
            onCropped = {
                picked = null
                onCropped(it)
            }
        )
    }
    return {
        context.expectExternalReturn()
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

/** Full-screen cropper: pinch to zoom and drag to position the photo inside the circle. */
@Composable
fun CircleCropDialog(uri: Uri, onDismiss: () -> Unit, onCropped: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uri) { value = loadForCrop(context, uri) { onDismiss() } }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Move and pinch to fit", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onSizeChanged { viewport = it }
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val img = bitmap ?: return@detectTransformGestures
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = clampOffset(offset + pan, img, scale, size.width.toFloat())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val img = bitmap
                if (img == null) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Image(
                        img.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                    )
                    // Dim everything outside the circle, and outline it
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        val hole = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
                            addOval(androidx.compose.ui.geometry.Rect(center, r))
                        }
                        drawPath(hole, Color.Black.copy(alpha = 0.6f))
                        drawCircle(Color.White, r, center, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                Button(
                    onClick = { bitmap?.let { onCropped(crop(it, scale, offset, viewport.width.toFloat())) } },
                    enabled = bitmap != null && viewport.width > 0
                ) { Text("Use photo") }
            }
        }
    }
}

private suspend fun loadForCrop(context: Context, uri: Uri, onFail: () -> Unit): Bitmap? = withContext(Dispatchers.IO) {
    try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val s = LOAD_MAX_SIDE.toFloat() / max(info.size.width, info.size.height)
            if (s < 1f) decoder.setTargetSize((info.size.width * s).toInt().coerceAtLeast(1), (info.size.height * s).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Couldn't open that picture", Toast.LENGTH_SHORT).show()
            onFail()
        }
        null
    }
}

/**
 * The photo is shown "cropped to fill" a square of side [view] px, then zoomed by [userScale] and
 * moved by [offset]. Keeps the photo covering the whole square so the circle never shows a gap.
 */
private fun clampOffset(offset: Offset, img: Bitmap, userScale: Float, view: Float): Offset {
    val s = max(view / img.width, view / img.height) * userScale
    val maxX = (img.width * s - view) / 2f
    val maxY = (img.height * s - view) / 2f
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

/** Cuts out exactly the square the user sees and scales it to [OUTPUT_SIZE]. */
private fun crop(img: Bitmap, userScale: Float, offset: Offset, view: Float): Bitmap {
    val s = max(view / img.width, view / img.height) * userScale
    // Where the visible square's top-left lands in the photo's own pixels
    val left = (img.width / 2f) - (view / 2f + offset.x) / s
    val top = (img.height / 2f) - (view / 2f + offset.y) / s
    val side = view / s
    val x = left.coerceIn(0f, img.width - side).toInt()
    val y = top.coerceIn(0f, img.height - side).toInt()
    val sidePx = side.toInt().coerceAtMost(minOf(img.width - x, img.height - y)).coerceAtLeast(1)
    val square = Bitmap.createBitmap(img, x, y, sidePx, sidePx)
    return Bitmap.createScaledBitmap(square, OUTPUT_SIZE, OUTPUT_SIZE, true)
}
