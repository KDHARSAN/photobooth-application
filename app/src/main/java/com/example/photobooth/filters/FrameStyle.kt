package com.example.photobooth.filters

import androidx.compose.ui.graphics.Color

enum class FrameStyle(
    val displayName: String,
    val color: Color,
    val textColor: Color
) {
    CLASSIC_WHITE("Classic White", Color.White, Color.Black),
    MIDNIGHT_BLACK("Midnight Black", Color.Black, Color.White),
    PASTEL_PINK("Pastel Pink", Color(0xFFFFD1DC), Color.DarkGray),
    RAW_CARDBOARD("Raw Cardboard", Color(0xFFD2B48C), Color(0xFF5D4037))
}
