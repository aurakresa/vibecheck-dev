package com.example.vibecheck_dev.domain.util

data class TargetPose(
    val name: String,

    // Parameter Logika AI (Derajat Sendi)
    val rightElbowAngle: Double? = null,
    val leftElbowAngle: Double? = null,
    val rightShoulderAngle: Double? = null,
    val leftShoulderAngle: Double? = null,
    val tolerance: Double = 25.0,

    // Parameter Visual UI (Rasio Koordinat Stickman)
    val uiLeftElbowX: Float = -0.5f, val uiLeftElbowY: Float = 1f,
    val uiLeftWristX: Float = -0.3f, val uiLeftWristY: Float = 2f,

    val uiRightElbowX: Float = 0.5f, val uiRightElbowY: Float = 1f,
    val uiRightWristX: Float = 0.3f, val uiRightWristY: Float = 2f
)

object PoseDictionary {
    // 1. Tangan Kanan Naik (Peace Sign)
    val PEACE_Y2K = TargetPose(
        name = "PEACE_Y2K",
        rightElbowAngle = 45.0,
        uiRightElbowX = 0.8f, uiRightElbowY = 0.3f,
        uiRightWristX = 0.4f, uiRightWristY = -0.6f,
        uiLeftElbowX = -0.5f, uiLeftElbowY = 1f,
        uiLeftWristX = -0.2f, uiLeftWristY = 1.8f
    )

    // 2. Dua Tangan Masuk Saku (Cool)
    val CHILL_POCKET = TargetPose(
        name = "CHILL_POCKET",
        rightElbowAngle = 170.0, leftElbowAngle = 170.0,
        uiLeftElbowX = -0.5f, uiLeftElbowY = 1f,
        uiLeftWristX = -0.1f, uiLeftWristY = 1.8f,
        uiRightElbowX = 0.5f, uiRightElbowY = 1f,
        uiRightWristX = 0.1f, uiRightWristY = 1.8f
    )

    // 3. Tolak Pinggang Dua Tangan (Sassy)
    val SASSY_HIP = TargetPose(
        name = "SASSY_HIP",
        uiLeftElbowX = -1.2f, uiLeftElbowY = 0.5f,
        uiLeftWristX = -0.5f, uiLeftWristY = 1.1f,
        uiRightElbowX = 1.2f, uiRightElbowY = 0.5f,
        uiRightWristX = 0.5f, uiRightWristY = 1.1f
    )

    // Daftar semua pose untuk mode Random
    val POSES = listOf(PEACE_Y2K, CHILL_POCKET, SASSY_HIP)
}