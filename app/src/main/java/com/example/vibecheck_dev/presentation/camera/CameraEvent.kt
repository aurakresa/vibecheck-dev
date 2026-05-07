package com.example.vibecheck_dev.presentation.camera

sealed class CameraEvent {
    object StartHosting : CameraEvent()
    object StopHosting : CameraEvent()
    data class UpdateHardwareSpecs(val minZoom: Float, val maxZoom: Float) : CameraEvent()
    data class SendVideoFrame(val byteArray: ByteArray, val rotationDegrees: Int, val isFrontCamera: Boolean) : CameraEvent()
    object GestureDetected : CameraEvent()
    object TakePhotoLocal : CameraEvent()
    object ToggleFlashLocal : CameraEvent()
    object ToggleFilterLocal : CameraEvent()

    // --- TAMBAHAN BARU OPSI B ---
    object FlipCameraLocal : CameraEvent()
    object ToggleTimerLocal : CameraEvent()
    object ToggleZoomLocal : CameraEvent()
}