package com.example.vibecheck_dev.presentation.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    // 1. Logika Izin yang TEGAS berdasarkan versi OS
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // KHUSUS ANDROID 13 KE ATAS: Kamera & Perangkat Sekitar (TANPA LOKASI)
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            // KHUSUS ANDROID 12 KE BAWAH: Kamera & Lokasi
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var showRationale by remember { mutableStateOf(true) }

    // 2. Launcher peminta izin bawaan Jetpack Compose
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isAllGranted = permissionsMap.values.all { it }
        if (isAllGranted) {
            onAllPermissionsGranted()
        }
    }

    // 3. Antarmuka (UI) Layar Izin
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Ikon Peringatan",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Akses Dibutuhkan",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Aplikasi Asisten Kamera membutuhkan akses Kamera untuk mengambil gambar, dan akses Lokasi/Perangkat Sekitar agar kedua HP dapat saling terhubung tanpa internet.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    showRationale = false
                    // Menjalankan pop-up dialog sistem Android
                    permissionLauncher.launch(permissionsToRequest)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Berikan Izin Akses")
            }
        }
    }
}