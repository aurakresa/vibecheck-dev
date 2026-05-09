package com.example.vibecheck_dev.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 3. Efek Teks Ngetik Sendiri (Terminal/Command Prompt)
@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    typingSpeed: Long = 50L // Kecepatan ngetik dalam milidetik
) {
    var visibleText by remember { mutableStateOf("") }

    // Efek ini bakal jalan ulang kalau 'text'-nya berubah (misal pas pindah halaman onboarding)
    LaunchedEffect(text) {
        visibleText = ""
        for (char in text) {
            delay(typingSpeed)
            visibleText += char
        }
    }

    Text(
        text = visibleText,
        style = style,
        color = color,
        modifier = modifier
    )
}

// 4. Efek Glitch (Goyang Kanan-Kiri Cepat ala Monitor Rusak)
@Composable
fun Modifier.y2kGlitchEffect(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "GlitchTransition")

    // Bikin nilai X berubah-ubah cepet dari -2 ke 2
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing), // 50ms = super cepet!
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlitchOffset"
    )

    // Menerapkan perubahan posisi X
    return this.offset(x = offsetX.dp)
}

// 1. Animasi Berkedip (Blinking) ala Indikator REC / Kursor DOS
@Composable
fun Modifier.y2kBlinkEffect(durationMillis: Int = 500): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = StepEasing), // Pakai StepEasing biar kedipnya kaku (Y2K)
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimation"
    )
    return this.alpha(alpha)
}

// 2. Animasi Tombol Membal (Scale/Press) ala Tombol Fisik Kamera
@Composable
fun Modifier.y2kPressEffect(
    interactionSource: MutableInteractionSource
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f, // Menyusut 10% saat ditekan
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "PressScaleAnimation"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// Easing kaku khusus pixel art (nggak ada transisi halus)
val StepEasing = Easing { fraction ->
    if (fraction < 0.5f) 1f else 0f
}