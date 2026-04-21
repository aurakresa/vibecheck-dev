package com.example.vibecheck

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.vibecheck_dev.ui.screens.CameraHostScreen
import com.example.vibecheck_dev.ui.theme.VibeCheckdevTheme // Sesuaikan dengan nama project lu
import com.example.vibecheck_dev.server.VibeServer

class MainActivity : ComponentActivity() {

    // 1. Inisialisasi Mesin Server Lu
    private val vibeServer = VibeServer()

    // 2. Launcher untuk minta Izin (Camera & Audio)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (!cameraGranted) {
            Toast.makeText(this, "Izin Kamera ditolak!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Minta izin pas aplikasi baru dibuka
        checkAndRequestPermissions()

        // 4. Mulai render UI (Menghubungkan Layar dengan Mesin Server)
        setContent {
            VibeCheckdevTheme { // Ini tema bawaan Compose

                // Variabel state untuk nyimpen IP yang reaktif (UI otomatis berubah kalau ini berubah)
                var ipAddress by remember { mutableStateOf("") }

                // Panggil layar UI Host
                CameraHostScreen(
                    serverIp = ipAddress,
                    onStartServer = {
                        // Nanti di sini lu panggil fungsi getWifiIP() bawaan Android lu
                        val dummyIp = "192.168.1.10"
                        ipAddress = dummyIp

                        // Nyalakan Ktor Server (Nanti)
                        // vibeServer.startServer(dummyIp)
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Jangan lupa matiin server kalau aplikasinya ditutup!
        // vibeServer.stopServer()
    }
}