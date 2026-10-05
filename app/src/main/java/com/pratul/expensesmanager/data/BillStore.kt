package com.pratul.expensesmanager.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Bill photos and PDFs attached to expenses. Each is copied into the app's private "bills" folder
 * and referred to by file name, so it survives even if the original is deleted.
 */
object BillStore {
    private const val DIR = "bills"
    private const val MAX_IMAGE_SIDE = 2000
    private const val MAX_PDF_BYTES = 15L * 1024 * 1024

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun exists(context: Context, name: String) = name.isNotBlank() && file(context, name).exists()

    fun isPdf(name: String) = name.endsWith(".pdf", ignoreCase = true)

    /**
     * Copies a picked or captured bill into the bills folder and returns its new file name.
     * Photos are shrunk and saved as JPEG; PDFs are copied as they are.
     * Throws [IllegalArgumentException] with a readable message if it can't be used.
     */
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri).orEmpty()
        val looksPdf = type == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)
        if (looksPdf) {
            val name = UUID.randomUUID().toString() + ".pdf"
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Couldn't open that file.")
            var copied = 0L
            input.use { inp ->
                file(context, name).outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = inp.read(buffer)
                        if (n < 0) break
                        copied += n
                        if (copied > MAX_PDF_BYTES) {
                            out.close()
                            file(context, name).delete()
                            throw IllegalArgumentException("That PDF is over 15 MB. Try a smaller one.")
                        }
                        out.write(buffer, 0, n)
                    }
                }
            }
            name
        } else {
            val bitmap = try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
                    if (scale < 1f) {
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("That file isn't a photo or PDF the app can read.")
            }
            val name = UUID.randomUUID().toString() + ".jpg"
            file(context, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            name
        }
    }

    /** A small preview of a photo bill (null for PDFs or missing files). */
    suspend fun thumbnail(context: Context, name: String, maxSide: Int = 300): Bitmap? = withContext(Dispatchers.IO) {
        if (isPdf(name) || !exists(context, name)) return@withContext null
        val path = file(context, name).path
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    suspend fun fullImage(context: Context, name: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!exists(context, name)) null else BitmapFactory.decodeFile(file(context, name).path)
    }

    fun delete(context: Context, name: String) {
        if (name.isNotBlank()) file(context, name).delete()
    }

    fun deleteAll(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }

    /** Where the camera app should save a new photo, before it's shrunk and moved into the bills folder. */
    fun cameraTargetUri(context: Context): Uri {
        val folder = File(context.cacheDir, "camera").apply { mkdirs() }
        val target = File(folder, "capture.jpg").apply { delete() }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", target)
    }

    /** Opens a PDF bill in the phone's PDF viewer. Returns false if there isn't one. */
    fun openExternally(context: Context, name: String): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file(context, name))
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, if (isPdf(name)) "application/pdf" else "image/jpeg")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
