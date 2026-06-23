package com.example.vibecheck_dev.presentation.remote

sealed class RemoteEvent {
    object StartDiscovery : RemoteEvent()
    object StopDiscovery : RemoteEvent()
    data class ConnectToDevice(val deviceAddress: String) : RemoteEvent()
    object Disconnect : RemoteEvent()
    object TakePhoto : RemoteEvent()
    object FlipCamera : RemoteEvent()
    object ToggleFlash : RemoteEvent()
    object ToggleTimer : RemoteEvent()

    object ToggleDigicamFilter : RemoteEvent()
    object TogglePhotoboothMode : RemoteEvent()
    data class ChangeZoom(val ratio: Float) : RemoteEvent()

    object ToggleAspectRatio : RemoteEvent()
    object ToggleVideoMode : RemoteEvent()
    data class SetIso(val iso: Int) : RemoteEvent()
    data class SetShutterSpeed(val speed: Long) : RemoteEvent()

    object ToggleAiMode : RemoteEvent()
}