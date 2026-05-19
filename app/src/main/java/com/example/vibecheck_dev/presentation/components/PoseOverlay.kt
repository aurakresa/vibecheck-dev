package com.example.vibecheck_dev.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.domain.util.GhostPose
import com.example.vibecheck_dev.domain.util.PoseBone
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun PoseOverlay(
    ghostPose: GhostPose?, // Lapis 1: Bayangan (Kalau null berarti belum muncul)
    realTimeBones: List<PoseBone> // Lapis 2: Gerakan asli user (Real-time)
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // ====================================================
            // LAPIS 1: GAMBAR "BAYANGAN CONTEKAN" (GHOST OUTLINE)
            // ====================================================
            if (ghostPose != null) {
                val ghostColor = Color.White.copy(alpha = 0.5f) // Putih transparan
                val ghostStroke = 6.dp.toPx()
                // Bikin garisnya putus-putus (Dashed) biar keliatan kaya cetakan
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)

                fun drawGhostBone(p1: Pair<Float, Float>, p2: Pair<Float, Float>) {
                    drawLine(
                        color = ghostColor,
                        start = Offset(p1.first * canvasW, p1.second * canvasH),
                        end = Offset(p2.first * canvasW, p2.second * canvasH),
                        strokeWidth = ghostStroke,
                        pathEffect = dashEffect
                    )
                }

                // Gambar Badan Ghost
                drawGhostBone(ghostPose.shoulderL, ghostPose.shoulderR)
                drawGhostBone(ghostPose.shoulderL, ghostPose.hipL)
                drawGhostBone(ghostPose.shoulderR, ghostPose.hipR)
                drawGhostBone(ghostPose.hipL, ghostPose.hipR)

                // Gambar Tangan Ghost
                drawGhostBone(ghostPose.shoulderL, ghostPose.elbowL)
                drawGhostBone(ghostPose.elbowL, ghostPose.wristL)
                drawGhostBone(ghostPose.shoulderR, ghostPose.elbowR)
                drawGhostBone(ghostPose.elbowR, ghostPose.wristR)

                // Gambar Kepala Ghost
                drawCircle(
                    color = ghostColor, radius = 50.dp.toPx(),
                    center = Offset(ghostPose.head.first * canvasW, ghostPose.head.second * canvasH),
                    style = Stroke(width = ghostStroke, pathEffect = dashEffect)
                )
            }

            // ====================================================
            // LAPIS 2: GAMBAR REAL-TIME BADAN USER (HIJAU NEON)
            // ====================================================
            val boneColor = Color.Green.copy(alpha = 0.9f)
            val boneStroke = 4.dp.toPx()

            realTimeBones.forEach { bone ->
                val startX = bone.startX * canvasW
                val startY = bone.startY * canvasH
                val endX = bone.endX * canvasW
                val endY = bone.endY * canvasH

                drawLine(color = boneColor, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = boneStroke)
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(startX, startY))
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(endX, endY))
            }
        }

        // ====================================================
        // TEKS INSTRUKSI DI LAYAR
        // ====================================================
        if (ghostPose != null) {
            Text(
                text = "SUGGESTED POSE:\n> ${ghostPose.name} <\n(Match your body to the white lines!)",
                style = Y2KTypography.bodyMedium,
                color = Color.Cyan,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .y2kBlinkEffect(800)
            )
        }
    }
}