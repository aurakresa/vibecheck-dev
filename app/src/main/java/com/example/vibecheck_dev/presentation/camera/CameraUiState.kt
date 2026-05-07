package com.example.vibecheck_dev.presentation.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val timerSeconds: Int = 0,
    val zoomRatio: Float = 1f,
    // Menyimpan spek hardware di dalam state juga
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val isDigicamFilterActive: Boolean = false,
    val isPhotoboothMode: Boolean = false
)