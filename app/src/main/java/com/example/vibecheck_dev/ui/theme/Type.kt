package com.example.vibecheck_dev.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.vibecheck_dev.R

// Font Custom Pixel
val PixelFont = FontFamily(
    Font(R.font.press_start)
)

val Y2KTypography = Typography(
    // Gunakan PixelFont untuk Judul Besar (Contoh: "MODE REMOTE")
    headlineLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp
    ),
    // Gunakan PixelFont untuk Sub-Judul atau Angka ISO/Timer
    titleLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 1.sp
    ),
    // Gunakan Font Bawaan (Sans-serif) yang bersih untuk tombol agar mudah dibaca
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    ),
    // Gunakan Font Bawaan (Sans-serif) untuk deskripsi panjang
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)