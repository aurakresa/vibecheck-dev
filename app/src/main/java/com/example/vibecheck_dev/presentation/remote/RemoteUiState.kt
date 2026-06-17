package com.example.vibecheck_dev.presentation.remote

import android.graphics.Bitmap
import androidx.camera.core.AspectRatio

data class RemoteUiState(
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val currentZoom: Float = 1f,
    val remoteBitmap: Bitmap? = null,

    // Kita pindahkan state lokal (dari remember) ke sini
    val flashMode: String = "OFF",
    val timerSeconds: Int = 0,
    val isDigicamFilterActive: Boolean = false,
    val isPhotoboothMode: Boolean = false,

    // --- TAMBAHAN BARU ---
    val isDiscovering: Boolean = false,
    // --- STATE BARU Y2K PRO ---
    val isVideoMode: Boolean = false,
    val aspectRatio: Int = AspectRatio.RATIO_4_3,
    val iso: Int = 100,
    val shutterSpeed: Long = 0L,
    val isRecording: Boolean = false, // Simulasi status rekaman di Remote

    // --- STATE AI REMOTE ---
    val isAiModeActive: Boolean = false,
    val aiPhase: String = "IDLE",
    val currentPoseType: String = "HALF_BODY_PEACE",
    val isPoseMatched: Boolean = false,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
    val bodyScale: Float = 0f,
    val isPersonDetected: Boolean = false,

    // 🛡️ TAMBAHAN BARU: Buat nendang UI langsung ke halaman pencarian
    val isHostDisconnected: Boolean = false
)