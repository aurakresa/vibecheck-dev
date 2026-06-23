package com.example.vibecheck_dev.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- PALET WARNA Y2K ---
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonGreen = Color(0xFF00FF00)

private val MatrixGreen = Color(0xFF00FF41)
private val MatrixDarkGreen = Color(0xFF008F11)

private val UbuntuOrange = Color(0xFFE95420)
private val UbuntuBg = Color(0xFF300A24)
private val UbuntuSurface = Color(0xFF430C33)

// --- WARNA CYBER CHROME (BARU - HIGH CONTRAST) ---
private val ChromePrimary = Color(0xFFFF1493) // Hot Pink Tua
private val ChromeSecondary = Color(0xFF0000CD) // Biru Elektrik Tua
private val ChromeBg = Color(0xFFEBEBEB) // Silver Terang
private val ChromeSurface = Color(0xFFD6D6D6) // Silver Agak Gelap (buat sidebar/menu)
private val ChromeText = Color(0xFF1A1A1A) // Hampir Hitam

// --- SKEMA WARNA MATERIAL TEMA ---
private val Y2KNeonScheme = darkColorScheme(
    primary = NeonMagenta,
    secondary = NeonCyan,
    tertiary = NeonGreen,
    background = Color.Black,
    surface = Color(0xFF050505),
    onBackground = Color.White,
    onSurface = Color.White
)

private val MatrixScheme = darkColorScheme(
    primary = MatrixGreen,
    secondary = MatrixDarkGreen,
    tertiary = MatrixGreen,
    background = Color(0xFF050505),
    surface = Color.Black,
    onBackground = MatrixGreen,
    onSurface = MatrixGreen
)

private val UbuntuScheme = darkColorScheme(
    primary = UbuntuOrange,
    secondary = Color.White,
    tertiary = UbuntuOrange,
    background = UbuntuBg,
    surface = UbuntuSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

private val CyberChromeScheme = lightColorScheme(
    primary = ChromePrimary,
    secondary = ChromeSecondary,
    tertiary = ChromePrimary,
    background = ChromeBg,
    surface = ChromeSurface,
    onBackground = ChromeText,
    onSurface = ChromeText,
    error = Color(0xFFD32F2F)
)

@Composable
fun VibeCheckdevTheme(
    themeName: String = "Y2K BRIGHT NEON",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "MATRIX TERMINAL" -> MatrixScheme
        "UBUNTU DARK" -> UbuntuScheme
        "CYBER CHROME (LIGHT)" -> CyberChromeScheme
        else -> Y2KNeonScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Kalau temanya Light (Cyber Chrome), icon jam & baterai di HP dihitamin biar kelihatan
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = themeName == "CYBER CHROME (LIGHT)"
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Y2KTypography,
        content = content
    )
}