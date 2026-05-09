package com.example.vibecheck_dev.presentation.remote

import android.graphics.Bitmap

data class RemoteUiState(
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val currentZoom: Float = 1f,
    val remoteBitmap: Bitmap? = null,

    // Kita pindahkan state lokal (dari remember) ke sini
    val flashMode: String = "OFF",
    val timerSeconds: Int = 0
)