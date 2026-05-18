package com.example.vibecheck_dev.presentation.camera

import android.util.Base64
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibecheck_dev.domain.repository.P2pRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
    private val p2pRepository: P2pRepository
) : ViewModel() {

    val connectionInfo = p2pRepository.connectionInfo

    // Trigger jepretan (Sifatnya sesaat, jadi tidak dimasukkan ke dalam UiState)
    private val _takePhotoTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 10)
    val takePhotoTrigger = _takePhotoTrigger.asSharedFlow()

    // SATU SUMBER KEBENARAN UNTUK UI KAMERA
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            p2pRepository.incomingMessages.collect { message ->
                when {
                    message == "CMD_JEPRET" -> _takePhotoTrigger.tryEmit(Unit)

                    message.startsWith("CMD_FLASH_") -> {
                        val mode = message.removePrefix("CMD_FLASH_")
                        val flashInt = when(mode) {
                            "ON" -> ImageCapture.FLASH_MODE_ON
                            "AUTO" -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        _uiState.update { it.copy(flashMode = flashInt) }
                    }

                    message == "CMD_FLIP" -> {
                        _uiState.update {
                            val newLens = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                            it.copy(lensFacing = newLens)
                        }
                    }

                    message.startsWith("CMD_TIMER_") -> {
                        val secs = message.removePrefix("CMD_TIMER_").toIntOrNull() ?: 0
                        _uiState.update { it.copy(timerSeconds = secs) }
                    }

                    message.startsWith("CMD_ZOOM_") -> {
                        val zoom = message.removePrefix("CMD_ZOOM_").toFloatOrNull() ?: 1f
                        _uiState.update { it.copy(zoomRatio = zoom) }
                    }

                    message == "CMD_REQUEST_SPECS" -> {
                        // Kirim spek terbaru dari State saat Remote bertanya
                        val currentMin = _uiState.value.minZoom
                        val currentMax = _uiState.value.maxZoom
                        p2pRepository.sendMessage("SYNC_ZOOM_${currentMin}_${currentMax}")
                    }

                    message.startsWith("CMD_FILTER_") -> {
                        val isOn = message.removePrefix("CMD_FILTER_") == "ON"
                        _uiState.update { it.copy(isDigicamFilterActive = isOn) }
                    }
                    message.startsWith("CMD_PHOTOBOOTH_") -> {
                        val isOn = message.removePrefix("CMD_PHOTOBOOTH_") == "ON"
                        _uiState.update { it.copy(isPhotoboothMode = isOn) }
                    }
                    message == "CMD_JEPRET" -> {
                        if (_uiState.value.isPhotoboothMode) {
                            startPhotoboothSequence() // Panggil loop 4x
                        } else {
                            _takePhotoTrigger.tryEmit(Unit)
                        }
                    }
                    // --- TAMBAHIN KODE INI DI DALAM WHEN INCOMING MESSAGES ---
                    message == "CMD_TOGGLE_ASPECT" -> {
                        val newRatio = if (_uiState.value.aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) {
                            androidx.camera.core.AspectRatio.RATIO_16_9
                        } else androidx.camera.core.AspectRatio.RATIO_4_3
                        _uiState.update { it.copy(aspectRatio = newRatio) }
                    }
                    message == "CMD_TOGGLE_VIDEO" -> {
                        _uiState.update { it.copy(isVideoMode = !it.isVideoMode) }
                    }
                    message.startsWith("CMD_ISO_") -> {
                        val iso = message.removePrefix("CMD_ISO_").toIntOrNull() ?: 100
                        _uiState.update { it.copy(iso = iso) }
                    }
                    message.startsWith("CMD_SHT_") -> {
                        val sht = message.removePrefix("CMD_SHT_").toLongOrNull() ?: 0L
                        _uiState.update { it.copy(shutterSpeed = sht) }
                    }
                }
            }
        }
    }

    // SATU PINTU UNTUK SEMUA EVENT
    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.StartHosting -> p2pRepository.startDiscovery()
            is CameraEvent.StopHosting -> {
                p2pRepository.disconnect()
                p2pRepository.stopDiscovery()
            }
            is CameraEvent.UpdateHardwareSpecs -> {
                _uiState.update { it.copy(minZoom = event.minZoom, maxZoom = event.maxZoom) }
                p2pRepository.sendMessage("SYNC_ZOOM_${event.minZoom}_${event.maxZoom}")
            }
            is CameraEvent.SendVideoFrame -> {
                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val base64String = Base64.encodeToString(event.byteArray, Base64.NO_WRAP)
                        p2pRepository.sendMessage("CMD_FRAME_${event.rotationDegrees}_${event.isFrontCamera}_$base64String")
                    } catch (e: Exception) {
                        Log.e("CAMERA_VM", "Gagal encode frame: ${e.message}")
                    }
                }
            }
            is CameraEvent.GestureDetected -> {
                if (_uiState.value.isPhotoboothMode) {
                    startPhotoboothSequence()
                } else {
                    _takePhotoTrigger.tryEmit(Unit)
                }
            }
            is CameraEvent.TakePhotoLocal -> {
                _takePhotoTrigger.tryEmit(Unit)
            }
            is CameraEvent.ToggleFlashLocal -> {
                val nextMode = when (_uiState.value.flashMode) {
                    androidx.camera.core.ImageCapture.FLASH_MODE_OFF -> androidx.camera.core.ImageCapture.FLASH_MODE_ON
                    else -> androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                }
                _uiState.update { it.copy(flashMode = nextMode) }
            }
            is CameraEvent.ToggleFilterLocal -> {
                _uiState.update { it.copy(isDigicamFilterActive = !it.isDigicamFilterActive) }
            }
            // ... di dalam fungsi onEvent ...
            is CameraEvent.FlipCameraLocal -> {
                _uiState.update {
                    val newLens = if (it.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                        androidx.camera.core.CameraSelector.LENS_FACING_FRONT
                    else androidx.camera.core.CameraSelector.LENS_FACING_BACK
                    it.copy(lensFacing = newLens)
                }
            }
            is CameraEvent.ToggleTimerLocal -> {
                val nextTimer = when (_uiState.value.timerSeconds) {
                    0 -> 3; 3 -> 6; 6 -> 10; else -> 0
                }
                _uiState.update { it.copy(timerSeconds = nextTimer) }
            }
            is CameraEvent.ToggleZoomLocal -> {
                // Siklus: 1.0x (Wide) -> 2.0x (Zoom) -> 0.5x (Ultrawide) -> Balik ke 1.0x
                when (_uiState.value.zoomRatio) {
                    1f -> { // Ke 2x Zoom (Tetap di lensa utama)
                        _uiState.update { it.copy(zoomRatio = 2f, isUltrawideActive = false) }
                    }
                    2f -> { // Ke 0.5x Ultrawide (Harus pindah lensa)
                        _uiState.update { it.copy(zoomRatio = 0.5f, isUltrawideActive = true) }
                    }
                    else -> { // Balik ke 1x Normal
                        _uiState.update { it.copy(zoomRatio = 1f, isUltrawideActive = false) }
                    }
                }
            }

            is CameraEvent.SetZoomLocal -> {
                // Kalau milih 0.5, otomatis trigger mode Ultrawide
                val isUltrawide = event.zoom < 1f
                _uiState.update { it.copy(zoomRatio = event.zoom, isUltrawideActive = isUltrawide) }
            }
            is CameraEvent.ToggleVideoModeLocal -> {
                _uiState.update { it.copy(isVideoMode = !it.isVideoMode) }
            }
            // --- TAMBAHAN EVENT BARU ---
            is CameraEvent.ToggleAspectRatio -> {
                val newRatio = if (_uiState.value.aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) {
                    androidx.camera.core.AspectRatio.RATIO_16_9
                } else {
                    androidx.camera.core.AspectRatio.RATIO_4_3
                }
                _uiState.update { it.copy(aspectRatio = newRatio) }
            }
            is CameraEvent.SetIso -> {
                _uiState.update { it.copy(iso = event.iso) }
            }
            is CameraEvent.SetShutterSpeed -> {
                _uiState.update { it.copy(shutterSpeed = event.speed) }
            }

// ... sisa kode lainnya ...
        }
    }

    private fun startPhotoboothSequence() {
        viewModelScope.launch {
            for (i in 1..4) {
                // Set timer ke 3 detik setiap kali mau foto
                _uiState.update { it.copy(timerSeconds = 3) }
                _takePhotoTrigger.tryEmit(Unit) // Trigger proses hitung mundur di CameraScreen

                // Tunggu 3 detik timer + 1 detik proses save
                kotlinx.coroutines.delay(4000)
            }
            // TODO: Gabungkan 4 foto terakhir pakai ImageProcessor.createPhotoStrip()
        }
    }
}