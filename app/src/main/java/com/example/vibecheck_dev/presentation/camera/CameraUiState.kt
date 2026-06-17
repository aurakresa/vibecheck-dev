package com.example.vibecheck_dev.presentation.camera

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.example.vibecheck_dev.domain.model.TargetPose
import com.google.mlkit.vision.pose.Pose

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val timerSeconds: Int = 0,
    val zoomRatio: Float = 1f,
    // Menyimpan spek hardware di dalam state juga
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    // Pastikan ini ada di CameraUiState
    val isUltrawideActive: Boolean = false,
    val isDigicamFilterActive: Boolean = false,
    val isPhotoboothMode: Boolean = false,
    val isVideoMode: Boolean = false, // Tambahin baris ini
    val aspectRatio: Int = AspectRatio.RATIO_4_3, // Default 4:3
    val iso: Int = 100,
    val shutterSpeed: Long = 0L, // 0L berarti Auto
    val isVideoRecording: Boolean = false,

    val isPoseSuggestionActive: Boolean = false,
    val currentPose: Pose? = null,
    val poseImageWidth: Int = 480, // Tambahan
    val poseImageHeight: Int = 640, // Tambahan

    val aiPhase: AiPhase = AiPhase.IDLE,
    val currentTargetPose: TargetPose? = null, // Cetakan pose yang akan dirender

    // ... state lu yang lain ...

    // Fase AI (Scanning YOLO vs Tracking ML Kit)
    val isScanningEnvironment: Boolean = false,

    // Hasil rekomendasi yang akan digambar oleh PoseOverlay
    val recommendedPoses: List<TargetPose> = emptyList(),

    // TAMBAHAN BARU:
    val currentPoseType: Y2KPoseType = Y2KPoseType.HALF_BODY_PEACE

)

enum class AiPhase {
    IDLE,
    SCANNING,
    READY_TO_MATCH, // Solo Mode (Kawat Tulang)
    GROUP_MATCH     // Group Mode (Frame Majalah, tanpa kawat)
}

enum class Y2KPoseType {
    // --- 7 POSE HALF BODY ---
    HALF_BODY_PEACE,      // Pose V-Sign
    HALF_BODY_COOL,       // Tangan menyilang di dada
    HALF_BODY_SALUTE,     // Hormat 2 jari ala Y2K
    HALF_BODY_FRAME,      // Tangan bikin bingkai kotak di depan muka
    HALF_BODY_FLEX,       // Pamer bicep (tangan nekuk ke atas)
    HALF_BODY_POINT,      // Tunjuk kamera
    HALF_BODY_GUNS,       // Pose pistol di bawah dagu

    // --- 7 POSE FULL BODY ---
    FULL_BODY_WIDE,       // Merentang ke bawah
    FULL_BODY_ACTION,     // Kaki melangkah, satu tangan di atas
    FULL_BODY_HANDS_UP,   // Dua tangan angkat ke langit
    FULL_BODY_T_POSE,     // Rentangkan tangan lurus horizontal
    FULL_BODY_ONE_UP,     // Satu tangan lurus ke atas, satu ke bawah
    FULL_BODY_HEAD,       // Tangan di belakang kepala nyantai
    FULL_BODY_CROSS       // Tangan nyilang di dada (versi full body)
}

