package com.example.vibecheck_dev.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.vibecheck_dev.presentation.camera.Y2KPoseType

@Composable
fun PoseOverlay(
    poseType: Y2KPoseType,
    isMatched: Boolean,
    anchorX: Float, // Ini masuk sebagai persentase (misal 0.5 untuk tengah)
    anchorY: Float,
    bodyScale: Float,
    modifier: Modifier = Modifier
) {
    if (anchorX == 0f || anchorY == 0f || bodyScale == 0f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // --- TRANSLATE KE RESOLUSI LAYAR HP ASLI ---
        val realAnchorX = anchorX * w
        val realAnchorY = anchorY * h
        val realScale = bodyScale * w

        val wireColor = if (isMatched) Color.Magenta else Color.Green
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 15f), 0f)
        val strokeStyle = Stroke(
            width = realScale * 0.15f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = dashEffect
        )

        fun drawHand(wristX: Float, wristY: Float, isPeace: Boolean) {
            val fingerLength = realScale * 0.3f
            if (isPeace) {
                drawLine(
                    wireColor,
                    Offset(wristX, wristY),
                    Offset(wristX - fingerLength * 0.3f, wristY - fingerLength),
                    strokeWidth = realScale * 0.08f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    wireColor,
                    Offset(wristX, wristY),
                    Offset(wristX + fingerLength * 0.3f, wristY - fingerLength),
                    strokeWidth = realScale * 0.08f,
                    cap = StrokeCap.Round
                )
            } else {
                drawRoundRect(
                    wireColor,
                    topLeft = Offset(wristX - realScale * 0.15f, wristY),
                    size = Size(realScale * 0.3f, realScale * 0.3f),
                    cornerRadius = CornerRadius(10f),
                    style = strokeStyle
                )
            }
        }

        // --- KEPALA & BADAN ---
        // Posisikan kepala agak naik sedikit dari bahu
        drawCircle(
            wireColor,
            radius = realScale * 0.45f,
            center = Offset(realAnchorX, realAnchorY - realScale * 0.65f),
            style = strokeStyle
        )

        // Torso mulai tepat dari bahu persis
        val torsoTop = realAnchorY
        val torsoBottom = realAnchorY + realScale * 1.8f
        drawRoundRect(
            wireColor,
            topLeft = Offset(realAnchorX - realScale * 0.7f, torsoTop),
            size = Size(realScale * 1.4f, realScale * 1.8f),
            cornerRadius = CornerRadius(realScale * 0.2f),
            style = strokeStyle
        )

        // --- TANGAN & KAKI ---
        val path = Path()
        when (poseType) {
            // ================= HALF BODY =================
            Y2KPoseType.HALF_BODY_PEACE -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.2f, torsoTop + realScale * 1.2f)
                path.lineTo(realAnchorX - realScale * 0.7f, torsoBottom - realScale * 0.2f)
                drawHand(
                    realAnchorX - realScale * 0.7f,
                    torsoBottom - realScale * 0.2f,
                    isPeace = false
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.4f, torsoTop + realScale * 0.5f)
                path.lineTo(realAnchorX + realScale * 1.0f, realAnchorY - realScale * 0.5f)
                drawHand(
                    realAnchorX + realScale * 1.0f,
                    realAnchorY - realScale * 0.5f,
                    isPeace = true
                )
            }

            Y2KPoseType.HALF_BODY_COOL -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.2f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX, torsoTop + realScale * 0.8f)

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX, torsoTop + realScale * 1.0f)
            }

            Y2KPoseType.HALF_BODY_SALUTE -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.4f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 0.5f, realAnchorY - realScale * 0.8f)
                drawHand(
                    realAnchorX - realScale * 0.5f,
                    realAnchorY - realScale * 0.8f,
                    isPeace = true
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoTop + realScale * 1.2f)
                path.lineTo(realAnchorX + realScale * 0.7f, torsoBottom - realScale * 0.2f)
                drawHand(
                    realAnchorX + realScale * 0.7f,
                    torsoBottom - realScale * 0.2f,
                    isPeace = false
                )
            }

            Y2KPoseType.HALF_BODY_FRAME -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.0f, torsoTop + realScale * 1.0f)
                path.lineTo(realAnchorX - realScale * 0.5f, realAnchorY - realScale * 0.2f)

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.0f, torsoTop + realScale * 1.0f)
                path.lineTo(realAnchorX + realScale * 0.5f, realAnchorY - realScale * 0.2f)
            }

            Y2KPoseType.HALF_BODY_FLEX -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.4f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX - realScale * 1.4f, realAnchorY - realScale * 0.2f)
                drawHand(
                    realAnchorX - realScale * 1.4f,
                    realAnchorY - realScale * 0.2f,
                    isPeace = false
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.4f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX + realScale * 1.4f, realAnchorY - realScale * 0.2f)
                drawHand(
                    realAnchorX + realScale * 1.4f,
                    realAnchorY - realScale * 0.2f,
                    isPeace = false
                )
            }

            Y2KPoseType.HALF_BODY_POINT -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 0.8f, torsoTop + realScale * 0.8f)
                path.lineTo(
                    realAnchorX - realScale * 0.4f,
                    torsoTop + realScale * 0.6f
                ) // Point forward

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.0f, torsoTop + realScale * 1.2f)
                path.lineTo(realAnchorX + realScale * 0.5f, torsoBottom - realScale * 0.2f)
                drawHand(
                    realAnchorX + realScale * 0.5f,
                    torsoBottom - realScale * 0.2f,
                    isPeace = false
                )
            }

            Y2KPoseType.HALF_BODY_GUNS -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.0f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX - realScale * 0.2f, realAnchorY)
                drawHand(realAnchorX - realScale * 0.2f, realAnchorY, isPeace = true)

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.0f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX + realScale * 0.2f, realAnchorY)
                drawHand(realAnchorX + realScale * 0.2f, realAnchorY, isPeace = true)
            }

            // ================= FULL BODY =================
            Y2KPoseType.FULL_BODY_WIDE -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.5f, torsoBottom)
                drawHand(realAnchorX - realScale * 1.5f, torsoBottom, isPeace = false)

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.5f, torsoBottom)
                drawHand(realAnchorX + realScale * 1.5f, torsoBottom, isPeace = false)

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 1.0f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 1.0f, torsoBottom + realScale * 3f)
            }

            Y2KPoseType.FULL_BODY_ACTION -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.2f, realAnchorY - realScale * 1.5f)
                drawHand(
                    realAnchorX - realScale * 1.2f,
                    realAnchorY - realScale * 1.5f,
                    isPeace = false
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoTop + realScale * 1.2f)
                path.lineTo(realAnchorX + realScale * 0.7f, torsoBottom)

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 0.4f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoBottom + realScale * 1.5f)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoBottom + realScale * 3f)
            }

            Y2KPoseType.FULL_BODY_HANDS_UP -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 0.8f, realAnchorY - realScale * 1.5f)
                drawHand(
                    realAnchorX - realScale * 0.8f,
                    realAnchorY - realScale * 1.5f,
                    isPeace = false
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 0.8f, realAnchorY - realScale * 1.5f)
                drawHand(
                    realAnchorX + realScale * 0.8f,
                    realAnchorY - realScale * 1.5f,
                    isPeace = false
                )

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 0.6f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 0.6f, torsoBottom + realScale * 3f)
            }

            Y2KPoseType.FULL_BODY_T_POSE -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 2.0f, torsoTop + realScale * 0.2f)
                drawHand(
                    realAnchorX - realScale * 2.0f,
                    torsoTop + realScale * 0.2f,
                    isPeace = false
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 2.0f, torsoTop + realScale * 0.2f)
                drawHand(
                    realAnchorX + realScale * 2.0f,
                    torsoTop + realScale * 0.2f,
                    isPeace = false
                )

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 0.4f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 0.4f, torsoBottom + realScale * 3f)
            }

            Y2KPoseType.FULL_BODY_ONE_UP -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 0.8f, realAnchorY - realScale * 1.5f)
                drawHand(
                    realAnchorX - realScale * 0.8f,
                    realAnchorY - realScale * 1.5f,
                    isPeace = false
                )

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoBottom)
                drawHand(realAnchorX + realScale * 1.2f, torsoBottom, isPeace = false)

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 0.8f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 0.8f, torsoBottom + realScale * 3f)
            }

            Y2KPoseType.FULL_BODY_HEAD -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.2f, realAnchorY - realScale * 0.5f)
                path.lineTo(realAnchorX - realScale * 0.4f, realAnchorY - realScale * 1.0f)

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.2f, realAnchorY - realScale * 0.5f)
                path.lineTo(realAnchorX + realScale * 0.4f, realAnchorY - realScale * 1.0f)

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 0.4f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 0.4f, torsoBottom + realScale * 3f)
            }

            Y2KPoseType.FULL_BODY_CROSS -> {
                path.moveTo(realAnchorX - realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX - realScale * 1.2f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX, torsoTop + realScale * 0.8f)

                path.moveTo(realAnchorX + realScale * 0.7f, torsoTop + realScale * 0.2f)
                path.lineTo(realAnchorX + realScale * 1.2f, torsoTop + realScale * 0.8f)
                path.lineTo(realAnchorX, torsoTop + realScale * 1.0f)

                path.moveTo(realAnchorX - realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX - realScale * 0.8f, torsoBottom + realScale * 3f)
                path.moveTo(realAnchorX + realScale * 0.4f, torsoBottom)
                path.lineTo(realAnchorX + realScale * 0.8f, torsoBottom + realScale * 3f)
            }
        }

        drawPath(path, wireColor, style = strokeStyle)
    }
}