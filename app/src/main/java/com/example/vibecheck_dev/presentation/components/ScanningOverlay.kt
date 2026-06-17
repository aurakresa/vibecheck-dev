package com.example.vibecheck_dev.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun ScanningOverlay(modifier: Modifier = Modifier) {
    // Animasi pergerakan laser dari Y=0 (atas) ke Y=1 (bawah)
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Ubah durasi jadi 1500 (1.5 detik per sapuan)
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        // --- 1. CANVAS UNTUK LASER & BINGKAI TARGET ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val currentY = scanLineY * h

            // Background agak gelap biar lasernya kontras
            drawRect(Color.Black.copy(alpha = 0.3f))

            // Laser Scanner Cyan yang tebal dan solid
            drawLine(
                color = Color.Cyan,
                start = Offset(0f, currentY),
                end = Offset(w, currentY),
                strokeWidth = 16f
            )

            // Bingkai Target Kamera (Sudut-sudut layar)
            val margin = 60f
            val cornerLength = 80f
            val thickStroke = 12f

            // Kiri Atas
            drawLine(Color.Green, Offset(margin, margin), Offset(margin + cornerLength, margin), strokeWidth = thickStroke)
            drawLine(Color.Green, Offset(margin, margin), Offset(margin, margin + cornerLength), strokeWidth = thickStroke)

            // Kanan Atas
            drawLine(Color.Green, Offset(w - margin, margin), Offset(w - margin - cornerLength, margin), strokeWidth = thickStroke)
            drawLine(Color.Green, Offset(w - margin, margin), Offset(w - margin, margin + cornerLength), strokeWidth = thickStroke)

            // Kiri Bawah
            drawLine(Color.Green, Offset(margin, h - margin), Offset(margin + cornerLength, h - margin), strokeWidth = thickStroke)
            drawLine(Color.Green, Offset(margin, h - margin), Offset(margin, h - margin - cornerLength), strokeWidth = thickStroke)

            // Kanan Bawah
            drawLine(Color.Green, Offset(w - margin, h - margin), Offset(w - margin - cornerLength, h - margin), strokeWidth = thickStroke)
            drawLine(Color.Green, Offset(w - margin, h - margin), Offset(w - margin, h - margin - cornerLength), strokeWidth = thickStroke)
        }

        // --- 2. TEKS INDIKATOR ALA KONSOL GAME RETRO ---
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Black)
                .border(3.dp, Color.Magenta)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .y2kBlinkEffect(durationMillis = 1500) // Efek kedip cepat
        ) {
            Text(
                text = "ANALYZING TARGET...",
                color = Color.Magenta,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = Y2KTypography.bodyLarge // Pastikan typography Y2K lu kepanggil
            )
        }
    }
}