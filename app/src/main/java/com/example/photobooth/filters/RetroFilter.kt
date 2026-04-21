package com.example.photobooth.filters

import jp.co.cyberagent.android.gpuimage.filter.*

enum class RetroFilter(val displayName: String) {
    NORMAL("Normal"),
    SEPIA("Sepia Vintage"),
    MONOCHROME("B&W Classic"),
    VINTAGE_WARM("1990s Warm"),
    CONTRAST("High Contrast"),
    FADED("Faded Polaroid");

    fun getGPUImageFilter(): GPUImageFilter {
        return when (this) {
            NORMAL -> GPUImageFilter()
            SEPIA -> GPUImageSepiaToneFilter()
            MONOCHROME -> GPUImageGrayscaleFilter()
            VINTAGE_WARM -> GPUImageColorMatrixFilter(
                1.0f,
                floatArrayOf(
                    1.2f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.8f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f
                )
            ) // Boosts reds, lowers blue for a warm fuzzy look
            CONTRAST -> GPUImageContrastFilter(1.5f)
            FADED -> GPUImageColorMatrixFilter(
                0.8f, // Lower intensity
                floatArrayOf(
                    0.9f, 0.1f, 0.0f, 0.0f,
                    0.1f, 0.9f, 0.0f, 0.0f,
                    0.0f, 0.1f, 0.9f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f
                )
            )
        }
    }
}
