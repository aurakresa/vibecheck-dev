package com.example.vibecheck_dev.domain.util

// Struktur data untuk Titik Sendi Bayangan (Contekan)
data class GhostPose(
    val name: String,
    val head: Pair<Float, Float>,
    val shoulderL: Pair<Float, Float>, val shoulderR: Pair<Float, Float>,
    val elbowL: Pair<Float, Float>, val elbowR: Pair<Float, Float>,
    val wristL: Pair<Float, Float>, val wristR: Pair<Float, Float>,
    val hipL: Pair<Float, Float>, val hipR: Pair<Float, Float>
)

object GhostPoseDictionary {
    // 1. GAYA TOLAK PINGGANG (SASSY HIP)
    val SASSY_HIP = GhostPose(
        name = "SASSY HIP",
        head = Pair(0.5f, 0.25f),
        shoulderL = Pair(0.35f, 0.35f), shoulderR = Pair(0.65f, 0.35f),
        hipL = Pair(0.4f, 0.6f), hipR = Pair(0.6f, 0.6f),
        // Tangan nekuk ke pinggang
        elbowL = Pair(0.2f, 0.45f), elbowR = Pair(0.8f, 0.45f),
        wristL = Pair(0.35f, 0.55f), wristR = Pair(0.65f, 0.55f)
    )

    // 2. GAYA PEACE Y2K
    val PEACE_Y2K = GhostPose(
        name = "PEACE SIGN Y2K",
        head = Pair(0.5f, 0.25f),
        shoulderL = Pair(0.35f, 0.35f), shoulderR = Pair(0.65f, 0.35f),
        hipL = Pair(0.4f, 0.6f), hipR = Pair(0.6f, 0.6f),
        // Tangan kiri santai ke bawah, Tangan kanan naik ngebentuk V di samping kepala
        elbowL = Pair(0.3f, 0.55f), elbowR = Pair(0.8f, 0.35f),
        wristL = Pair(0.35f, 0.7f), wristR = Pair(0.65f, 0.15f)
    )
}