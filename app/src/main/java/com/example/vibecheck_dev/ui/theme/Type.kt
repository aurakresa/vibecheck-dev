package com.example.vibecheck_dev.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.vibecheck_dev.R
import androidx.compose.ui.text.font.Font

// Set of Material typography styles to start with
val PixelFont = FontFamily(
    Font(R.font.press_start)
) // Nama file ttf lu

val Y2KTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = PixelFont,
        fontSize = 22.sp,
        color = Color.Green // Warna neon khas retro
    ),
    bodyMedium = TextStyle(
        fontFamily = PixelFont,
        fontSize = 12.sp,
        color = Color.White
    )
)