package com.example.photobooth.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object ExportManager {

    suspend fun saveToGallery(context: Context, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                // Load original file to copy
                val inputStream: InputStream? = resolver.openInputStream(imageUri)
                if (inputStream == null) return@withContext false

                val displayName = "photostrip_${System.currentTimeMillis()}.jpg"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoBooth")
                    }

                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        val outputStream: OutputStream? = resolver.openOutputStream(uri)
                        outputStream?.use { out ->
                            inputStream.copyTo(out)
                        }
                    } else {
                        return@withContext false
                    }
                } else {
                    // Pre Android 10
                    @Suppress("DEPRECATION")
                    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoBooth")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val file = File(directory, displayName)
                    val outputStream = FileOutputStream(file)
                    outputStream.use { out ->
                        inputStream.copyTo(out)
                    }
                    // Notify media scanner
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(file)
                    context.sendBroadcast(mediaScanIntent)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun shareImage(context: Context, imageUri: Uri) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg"
            // Grant read URI permission
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Photo Strip to..."))
    }
}
