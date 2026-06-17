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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class RemoteViewModel(
    private val p2pRepository: P2pRepository
) : ViewModel() {

    val isWifiEnabled = p2pRepository.isWifiP2pEnabled
    val peersList = p2pRepository.peersList
    val connectionInfo = p2pRepository.connectionInfo

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState = _uiState.asStateFlow()

    private val _savePhotoTrigger = kotlinx.coroutines.flow.MutableSharedFlow<ByteArray>()
    val savePhotoTrigger = _savePhotoTrigger.asSharedFlow()

    // 🛡️ Kunci gembok anti-jebol RAM (Frame Dropper)
    private val isDecodingFrame = AtomicBoolean(false)

    // 🛡️ VARIABEL WATCHDOG (Anti Zombie Socket)
    private var lastMessageTime = System.currentTimeMillis()

    init {
        // 1. WATCHDOG TIMER (Anjing Penjaga Anti-Stuck)
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(2000) // Ngecek setiap 2 detik
                if (connectionInfo.value?.groupFormed == true) {
                    // Kalau 3.5 detik Host gak ngirim apapun (Kabur/Crash/Wi-Fi putus)
                    if (System.currentTimeMillis() - lastMessageTime > 3500) {
                        Log.e("P2P", "Host terputus mendadak! Memutus koneksi...")
                        p2pRepository.disconnect()
                        // 🛡️ TAMBAH isHostDisconnected = true
                        _uiState.update { it.copy(remoteBitmap = null, isHostDisconnected = true) }
                    }
                }
            }
        }

        // 2. PENERIMA PESAN (Receiver)
        viewModelScope.launch {
            p2pRepository.incomingMessages.collect { msg ->

                // 🛡️ RESET TIMER karena Host masih hidup & ngirim data!
                lastMessageTime = System.currentTimeMillis()

                // 🛡️ TANGKAP SINYAL PAMIT DARI HOST
                if (msg == "CMD_GOODBYE") {
                    Log.d("P2P", "Host pamit keluar. Memutus koneksi...")
                    p2pRepository.disconnect()
                    // 🛡️ TAMBAH isHostDisconnected = true
                    _uiState.update { it.copy(remoteBitmap = null, isHostDisconnected = true) }
                    return@collect
                }

                if (msg.startsWith("CMD_SAVE_PHOTO_")) {
                    viewModelScope.launch(Dispatchers.Default) {
                        try {
                            val base64 = msg.removePrefix("CMD_SAVE_PHOTO_")
                            val bytes =
                                android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                            _savePhotoTrigger.emit(bytes) // Suruh RemoteScreen nyimpen ke Galeri
                        } catch (e: Exception) {
                            Log.e("REMOTE_VM", "Gagal nerima foto HD", e)
                        }
                    }
                } else if (msg.startsWith("CMD_FRAME_")) {
                    // 🛡️ LOGIKA DEWA: Kalau masih sibuk decode frame sebelumnya, BUANG frame baru ini!
                    if (isDecodingFrame.compareAndSet(false, true)) {
                        viewModelScope.launch(Dispatchers.Default) {
                            try {
                                val payload = msg.removePrefix("CMD_FRAME_")

                                // 🛡️ UBAH LIMIT JADI 10 GERBONG
                                val parts = payload.split("|", limit = 10)

                                if (parts.size == 10) {
                                    val hardwareRotation = parts[0].toFloatOrNull() ?: 0f
                                    val isFrontCamera = parts[1].toBoolean()
                                    val phase = parts[2]
                                    val pose = parts[3]
                                    val matched = parts[4].toBoolean()
                                    val ax = parts[5].toFloatOrNull() ?: 0f
                                    val ay = parts[6].toFloatOrNull() ?: 0f
                                    val scale = parts[7].toFloatOrNull() ?: 0f
                                    // 🛡️ TANGKAP STATUS ORANG DI INDEX 8
                                    val personDetected = parts[8].toBoolean()
                                    // 🛡️ BASE64 GESER KE INDEX 9
                                    val base64String = parts[9]

                                    val bytes = android.util.Base64.decode(
                                        base64String,
                                        android.util.Base64.NO_WRAP
                                    )
                                    val originalBitmap =
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                    // 🛡️ CEGAH CRASH: Pastikan gambar nggak cacat gara-gara sinyal kepotong
                                    if (originalBitmap != null) {
                                        val matrix = Matrix()
                                        matrix.postRotate(hardwareRotation)
                                        if (isFrontCamera) matrix.postScale(-1f, 1f)

                                        val rotatedBitmap = Bitmap.createBitmap(
                                            originalBitmap,
                                            0,
                                            0,
                                            originalBitmap.width,
                                            originalBitmap.height,
                                            matrix,
                                            true
                                        )

                                        _uiState.update {
                                            it.copy(
                                                remoteBitmap = rotatedBitmap,
                                                aiPhase = phase,
                                                currentPoseType = pose,
                                                isPoseMatched = matched,
                                                anchorX = ax,
                                                anchorY = ay,
                                                bodyScale = scale,
                                                isPersonDetected = personDetected // <-- Masukkan ke UI State
                                            )
                                        }
                                    } else {
                                        Log.e("REMOTE_VM", "Frame korup di jalan, diabaikan.")
                                    }
                                }
                            } catch (e: Throwable) {
                                // 🛡️ PERBAIKAN: Gunakan Throwable untuk nangkap OutOfMemoryError
                                Log.e("REMOTE_VM", "Hardware kewalahan decode frame", e)
                            } finally {
                                // Buka gembok biar frame selanjutnya bisa masuk
                                isDecodingFrame.set(false)
                            }
                        }
                    }
                } else if (msg.startsWith("SYNC_ZOOM_")) {
                    val parts = msg.removePrefix("SYNC_ZOOM_").split("_")
                    if (parts.size == 2) {
                        val min = parts[0].toFloatOrNull() ?: 1f
                        val max = parts[1].toFloatOrNull() ?: 1f
                        _uiState.update { it.copy(minZoom = min, maxZoom = max) }
                    }
                }
            }
        }
    }

    fun onEvent(event: RemoteEvent) {
        when (event) {
            is RemoteEvent.StartDiscovery -> {
                _uiState.update { it.copy(isDiscovering = true) }
                p2pRepository.startDiscovery()
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(isDiscovering = false) }
                }
            }

            is RemoteEvent.StopDiscovery -> p2pRepository.stopDiscovery()
            is RemoteEvent.ConnectToDevice -> {
                // 🛡️ RESET JADI FALSE BIAR BISA MASUK KE KAMERA LAGI
                _uiState.update { it.copy(isHostDisconnected = false) }
                p2pRepository.connectToDevice(event.deviceAddress)
            }

            is RemoteEvent.Disconnect -> {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        p2pRepository.sendMessage("CMD_GOODBYE")
                        kotlinx.coroutines.delay(200)
                        p2pRepository.disconnect()
                        // 🛡️ TAMBAH isHostDisconnected = true
                        _uiState.update { it.copy(remoteBitmap = null, isHostDisconnected = true) }
                    } catch (e: Exception) {
                        p2pRepository.disconnect()
                        _uiState.update { it.copy(remoteBitmap = null, isHostDisconnected = true) }
                    }
                }
            }

            is RemoteEvent.ToggleDigicamFilter -> {
                val newState = !_uiState.value.isDigicamFilterActive
                _uiState.update { it.copy(isDigicamFilterActive = newState) }
                p2pRepository.sendMessage("CMD_FILTER_${if (newState) "ON" else "OFF"}")
            }

            is RemoteEvent.TogglePhotoboothMode -> {
                val newState = !_uiState.value.isPhotoboothMode
                _uiState.update { it.copy(isPhotoboothMode = newState) }
                p2pRepository.sendMessage("CMD_PHOTOBOOTH_${if (newState) "ON" else "OFF"}")
            }

            is RemoteEvent.TakePhoto -> {
                if (_uiState.value.isVideoMode) {
                    _uiState.update { it.copy(isRecording = !it.isRecording) }
                }
                p2pRepository.sendMessage("CMD_JEPRET")
            }

            is RemoteEvent.FlipCamera -> p2pRepository.sendMessage("CMD_FLIP")

            is RemoteEvent.ToggleFlash -> {
                val nextMode = if (_uiState.value.flashMode == "OFF") "ON" else "OFF"
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

            is RemoteEvent.ToggleAspectRatio -> {
                val newRatio =
                    if (_uiState.value.aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) {
                        androidx.camera.core.AspectRatio.RATIO_16_9
                    } else androidx.camera.core.AspectRatio.RATIO_4_3
                _uiState.update { it.copy(aspectRatio = newRatio) }
                p2pRepository.sendMessage("CMD_TOGGLE_ASPECT")
            }

            is RemoteEvent.ToggleVideoMode -> {
                _uiState.update { it.copy(isVideoMode = !it.isVideoMode) }
                p2pRepository.sendMessage("CMD_TOGGLE_VIDEO")
            }

            is RemoteEvent.SetIso -> {
                _uiState.update { it.copy(iso = event.iso) }
                p2pRepository.sendMessage("CMD_ISO_${event.iso}")
            }

            is RemoteEvent.SetShutterSpeed -> {
                _uiState.update { it.copy(shutterSpeed = event.speed) }
                p2pRepository.sendMessage("CMD_SHT_${event.speed}")
            }

            is RemoteEvent.ToggleAiMode -> {
                val newState = !_uiState.value.isAiModeActive
                _uiState.update { it.copy(isAiModeActive = newState) }
                p2pRepository.sendMessage("CMD_TOGGLE_AI")
            }
        }
    }
}