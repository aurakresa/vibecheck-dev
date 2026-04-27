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

private val DarkColorScheme = darkColorScheme(
    primary = PixelCyan,
    secondary = PixelMagenta,
    background = PixelDarkBackground,
    surface = PixelDarkSurface,
    onPrimary = PixelDarkBackground, // Teks di atas warna Cyan jadi gelap
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PixelPurple,
    secondary = PixelTeal,
    background = PixelLightBackground,
    surface = PixelLightSurface,
    onPrimary = Color.White,
    onBackground = PixelDarkBackground,
    onSurface = PixelDarkBackground
)

@Composable
fun VibeCheckdevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Ubah default dynamicColor menjadi false
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Kita biarkan logika ini berjaga-jaga jika nanti kamu ingin menyalakannya lagi
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme // Untuk aplikasi kamera, saya sarankan paksa DarkMode terus
    }

    // Mengubah warna status bar (jam & baterai di atas layar) agar sesuai tema
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Y2KTypography,
        content = content
    )
}