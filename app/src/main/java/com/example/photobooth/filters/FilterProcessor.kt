package com.example.photobooth.filters

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import jp.co.cyberagent.android.gpuimage.GPUImage
import java.io.File
import java.io.FileOutputStream

object FilterProcessor {

    fun applyFilter(
        context: Context,
        inputBitmap: Bitmap,
        filter: RetroFilter,
        outputDirectory: File
    ): Uri? {
        if (filter == RetroFilter.NORMAL) {
            // Save as is if normal
            return saveBitmap(inputBitmap, outputDirectory)
        }

        val gpuImage = GPUImage(context)
        gpuImage.setImage(inputBitmap)
        gpuImage.setFilter(filter.getGPUImageFilter())
        
        val filteredBitmap = gpuImage.bitmapWithFilterApplied
        return saveBitmap(filteredBitmap, outputDirectory)
    }

    private fun saveBitmap(bitmap: Bitmap, outputDirectory: File): Uri? {
        val ts = System.currentTimeMillis()
        val resultFile = File(outputDirectory, "filtered_$ts.jpg")
        return try {
             val outStream = FileOutputStream(resultFile)
             bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
             outStream.flush()
             outStream.close()
             Uri.fromFile(resultFile)
        } catch (e: Exception) {
             e.printStackTrace()
             null
        }
    }
}
