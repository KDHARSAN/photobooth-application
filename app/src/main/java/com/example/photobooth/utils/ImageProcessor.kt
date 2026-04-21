package com.example.photobooth.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import com.example.photobooth.filters.RetroFilter

object ImageProcessor {

    fun createPhotoStrip(
        context: Context,
        uris: List<Uri>,
        outputDirectory: File,
        frameStyle: com.example.photobooth.filters.FrameStyle = com.example.photobooth.filters.FrameStyle.CLASSIC_WHITE,
        includeDate: Boolean = true,
        filter: RetroFilter = RetroFilter.NORMAL
    ): Uri? {
        val resultBitmap = createPhotoStripBitmap(context, uris, frameStyle, includeDate, filter) ?: return null
        
        // Save the result
        val ts = System.currentTimeMillis()
        val resultFile = File(outputDirectory, "strip_$ts.jpg")
        return try {
            val outStream = FileOutputStream(resultFile)
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
            resultBitmap.recycle() 
            Uri.fromFile(resultFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createPhotoStripBitmap(
        context: Context,
        uris: List<Uri>,
        frameStyle: com.example.photobooth.filters.FrameStyle,
        includeDate: Boolean,
        filter: RetroFilter
    ): Bitmap? {
        if (uris.size != 3) return null

        val bitmaps = uris.map { uri ->
            val raw = getBitmapFromUri(context, uri) ?: return null
            if (filter == RetroFilter.NORMAL) {
                raw
            } else {
                val gpuImage = jp.co.cyberagent.android.gpuimage.GPUImage(context)
                gpuImage.setImage(raw)
                gpuImage.setFilter(filter.getGPUImageFilter())
                val filtered = gpuImage.bitmapWithFilterApplied
                raw.recycle()
                filtered
            }
        }

        // Calculate dimensions
        val padding = 40 
        val imageWidth = bitmaps[0].width
        val imageHeight = bitmaps[0].height

        val stripWidth = imageWidth + (padding * 2)
        val stripHeight = (imageHeight * 3) + (padding * 4) + (if (includeDate) 80 else 0)

        val resultBitmap = Bitmap.createBitmap(stripWidth, stripHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw background
        canvas.drawColor(android.graphics.Color.argb(
            255,
            (frameStyle.color.red * 255).toInt(),
            (frameStyle.color.green * 255).toInt(),
            (frameStyle.color.blue * 255).toInt()
        ))

        // Draw each image
        var currentY = padding.toFloat()
        for (bitmap in bitmaps) {
            canvas.drawBitmap(bitmap, padding.toFloat(), currentY, null)
            currentY += bitmap.height + padding
            bitmap.recycle()
        }

        // Draw Date Stamp
        if (includeDate) {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FF8C00") 
                textSize = 48f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            }
            val sdf = java.text.SimpleDateFormat("yyyy MM dd", java.util.Locale.US)
            val dateStr = sdf.format(java.util.Date())
            canvas.drawText(dateStr, (stripWidth - padding - 300).toFloat(), (stripHeight - padding).toFloat(), paint)
        }
        
        return resultBitmap
    }

    @Suppress("DEPRECATION")
    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                    // CRITICAL: Force software allocation to allow drawing onto a software Canvas
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    // Downscale slightly to manage memory limits (S20 FE has large camera output)
                    decoder.setTargetSampleSize(2) 
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
