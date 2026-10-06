package com.pratul.expensesmanager.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The user's profile. [dobUtcMillis] is UTC midnight of the birth date (what the date picker gives).
 * [photoVersion] changes whenever the photo does, so the screen knows to reload it.
 */
data class Profile(val name: String, val dobUtcMillis: Long?, val photoVersion: Long)

object ProfileStore {
    private const val PREFS = "profile"
    private const val PHOTO_FILE = "profile_photo.jpg"
    private const val PHOTO_MAX_SIZE = 512

    fun load(context: Context): Profile {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Profile(
            name = prefs.getString("name", "") ?: "",
            dobUtcMillis = prefs.getLong("dob", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE },
            photoVersion = prefs.getLong("photoVersion", 0)
        )
    }

    fun save(context: Context, profile: Profile) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", profile.name)
            .putLong("photoVersion", profile.photoVersion)
        if (profile.dobUtcMillis != null) editor.putLong("dob", profile.dobUtcMillis) else editor.remove("dob")
        editor.apply()
    }

    private fun photoFile(context: Context) = File(context.filesDir, PHOTO_FILE)

    /** Copies the picked image into the app, shrunk and turned the right way up. Returns false if it can't be read. */
    suspend fun savePhoto(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = PHOTO_MAX_SIZE.toFloat() / maxOf(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            photoFile(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Saves an already-cropped photo (from the circle cropper). */
    suspend fun savePhotoBitmap(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            photoFile(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loadPhoto(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val file = photoFile(context)
        if (file.exists()) BitmapFactory.decodeFile(file.path) else null
    }

    fun deletePhoto(context: Context) {
        photoFile(context).delete()
    }

    /** Removes the name, birthday and photo. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        deletePhoto(context)
    }

    /** The saved photo's JPEG bytes, for backups. */
    fun photoBytes(context: Context): ByteArray? =
        photoFile(context).takeIf { it.exists() }?.readBytes()

    /** Puts back a photo from a backup. */
    fun restorePhotoBytes(context: Context, bytes: ByteArray) {
        photoFile(context).writeBytes(bytes)
    }
}
