package com.example.vibecheck_dev.presentation.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibecheck_dev.domain.repository.P2pRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RemoteViewModel(
    private val p2pRepository: P2pRepository
) : ViewModel() {

    // Aliran Data Jaringan (Biarkan terpisah karena dari Repository)
    val isWifiEnabled = p2pRepository.isWifiP2pEnabled
    val peersList = p2pRepository.peersList
    val connectionInfo = p2pRepository.connectionInfo

    // SATU SUMBER KEBENARAN (Single Source of Truth) UNTUK UI
    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            p2pRepository.incomingMessages.collect { msg ->
                if (msg.startsWith("CMD_FRAME_")) {
                    viewModelScope.launch(Dispatchers.Default) {
                        try {
                            val payload = msg.removePrefix("CMD_FRAME_")
                            val parts = payload.split("_", limit = 3)
                            if (parts.size < 3) return@launch

                            val hardwareRotation = parts[0].toFloat()
                            val isFrontCamera = parts[1].toBoolean()
                            val base64String = parts[2]

                            val bytes = Base64.decode(base64String, Base64.NO_WRAP)
                            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            val matrix = Matrix()
                            matrix.postRotate(hardwareRotation)
                            if (isFrontCamera) matrix.postScale(-1f, 1f)

                            val rotatedBitmap = Bitmap.createBitmap(
                                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                            )

                            // UPDATE STATE DENGAN CARA YANG BERSIH
                            _uiState.update { it.copy(remoteBitmap = rotatedBitmap) }
                        } catch (e: Exception) {
                            Log.e("REMOTE_VM", "Gagal decode frame: ${e.message}")
                        }
                    }
                }
                else if (msg.startsWith("SYNC_ZOOM_")) {
                    val parts = msg.removePrefix("SYNC_ZOOM_").split("_")
                    if (parts.size == 2) {
                        val min = parts[0].toFloatOrNull() ?: 1f
                        val max = parts[1].toFloatOrNull() ?: 1f
                        _uiState.update { it.copy(minZoom = min, maxZoom = max, currentZoom = 1f) }
                    }
                }
            }
        }
    }

    // SEMUA AKSI MASUK LEWAT SATU PINTU (Mencegah fungsi berceceran)
    fun onEvent(event: RemoteEvent) {
        when (event) {
            is RemoteEvent.StartDiscovery -> {
                _uiState.update { it.copy(isDiscovering = true) }
                p2pRepository.startDiscovery()

                // Matikan efek loading setelah 3 detik (simulasi pencarian selesai)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(isDiscovering = false) }
                }
            }
            is RemoteEvent.StopDiscovery -> p2pRepository.stopDiscovery()
            is RemoteEvent.ConnectToDevice -> p2pRepository.connectToDevice(event.deviceAddress)
            is RemoteEvent.Disconnect -> p2pRepository.disconnect()
            is RemoteEvent.TakePhoto -> p2pRepository.sendMessage("CMD_JEPRET")

            is RemoteEvent.FlipCamera -> p2pRepository.sendMessage("CMD_FLIP")

            is RemoteEvent.ToggleFlash -> {
                val nextMode = when (_uiState.value.flashMode) {
                    "OFF" -> "AUTO"; "AUTO" -> "ON"; else -> "OFF"
                }
                _uiState.update { it.copy(flashMode = nextMode) }
                p2pRepository.sendMessage("CMD_FLASH_$nextMode")
            }

            is RemoteEvent.ToggleTimer -> {
                val nextTimer = when (_uiState.value.timerSeconds) {
                    0 -> 3; 3 -> 6; 6 -> 10; else -> 0
                }
                _uiState.update { it.copy(timerSeconds = nextTimer) }
                p2pRepository.sendMessage("CMD_TIMER_$nextTimer")
            }

            is RemoteEvent.ChangeZoom -> {
                _uiState.update { it.copy(currentZoom = event.ratio) }
                p2pRepository.sendMessage("CMD_ZOOM_${event.ratio}")
            }

            is RemoteEvent.ToggleDigicamFilter -> {
                val newState = !_uiState.value.isDigicamFilterActive
                _uiState.update { it.copy(isDigicamFilterActive = newState) }
                p2pRepository.sendMessage("CMD_FILTER_${if(newState) "ON" else "OFF"}")
            }
            is RemoteEvent.TogglePhotoboothMode -> {
                val newState = !_uiState.value.isPhotoboothMode
                _uiState.update { it.copy(isPhotoboothMode = newState) }
                p2pRepository.sendMessage("CMD_PHOTOBOOTH_${if(newState) "ON" else "OFF"}")
            }
        }
    }

    fun requestCameraSpecs() {
        p2pRepository.sendMessage("CMD_REQUEST_SPECS")
    }
}