package com.example.vibecheck_dev.domain.model

import androidx.compose.ui.geometry.Offset

// Representasi dari 1 cetakan pose di layar
data class TargetPose(
    val leftShoulder: Offset,
    val rightShoulder: Offset,
    val leftElbow: Offset,
    val rightElbow: Offset,
    val leftWrist: Offset,
    val rightWrist: Offset,
    val leftHip: Offset,
    val rightHip: Offset
)

// Hasil analisis YOLO
enum class DetectedScene {
    MALL, NATURE, INDOOR_STUDIO, UNKNOWN
}