package com.example.vibecheck_dev.presentation.camera

import com.example.vibecheck_dev.domain.util.SceneResult

sealed class CameraEvent {
    object StartHosting : CameraEvent()
    object StopHosting : CameraEvent()
    data class UpdateHardwareSpecs(val minZoom: Float, val maxZoom: Float) : CameraEvent()
    data class SendVideoFrame(
        val byteArray: ByteArray,
        val rotationDegrees: Int,
        val isFrontCamera: Boolean,
        val aiPhase: String,
        val poseType: String,
        val isMatched: Boolean,
        val anchorX: Float,
        val anchorY: Float,
        val bodyScale: Float,
        val isPersonDetected: Boolean
    ) : CameraEvent()
    object GestureDetected : CameraEvent()
    object TakePhotoLocal : CameraEvent()
    object ToggleFlashLocal : CameraEvent()
    object ToggleFilterLocal : CameraEvent()

    // --- TAMBAHAN BARU OPSI B ---
    object FlipCameraLocal : CameraEvent()
    object ToggleTimerLocal : CameraEvent()
    object ToggleZoomLocal : CameraEvent()
    object ToggleAspectRatio : CameraEvent()
    object ToggleVideoModeLocal : CameraEvent()
    data class SetZoomLocal(val zoom: Float) : CameraEvent()

    data class SetIso(val iso: Int) : CameraEvent()

    data class SetShutterSpeed(val speed: Long) : CameraEvent()

    data class ProcessPose(
        val pose: com.google.mlkit.vision.pose.Pose,
        val imageWidth: Int,
        val imageHeight: Int
    ) : CameraEvent()
    object TogglePoseSuggestion : CameraEvent()

    data class OnYoloScanComplete(val result: SceneResult) : CameraEvent()

    // Tambahkan di dalam sealed class CameraEvent
    data class SetTargetPose(val pose: Y2KPoseType) : CameraEvent()

    data class SwitchAiPhase(val phase: AiPhase) : CameraEvent()

    object CycleTargetPose : CameraEvent()

    // Taruh di dalam sealed class CameraEvent
    data class SharePhotoToRemote(val byteArray: ByteArray) : CameraEvent()
}