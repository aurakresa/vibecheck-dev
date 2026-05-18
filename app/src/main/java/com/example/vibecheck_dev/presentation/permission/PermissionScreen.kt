package com.example.vibecheck_dev.presentation.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    // 1. TAMBAHAN: Kita selipkan RECORD_AUDIO di sini!
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO, // Izin Mic
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO, // Izin Mic
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var showRationale by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isAllGranted = permissionsMap.values.all { it }
        if (isAllGranted) {
            onAllPermissionsGranted()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(3.dp, Color.Cyan, RectangleShape).background(Color(0xFF001A1A)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Ikon Peringatan",
                modifier = Modifier.size(64.dp).y2kGlitchEffect(),
                tint = Color.Yellow
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("SYSTEM_REQ.cfg", style = Y2KTypography.titleLarge, color = Color.Cyan)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "VibeCheck butuh akses ke Hardware Kamera, Mikrofon (Audio), dan Modul Wi-Fi P2P buat transmisi data. Berikan akses untuk melanjutkan operasi.",
                style = Y2KTypography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    showRationale = false
                    permissionLauncher.launch(permissionsToRequest)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth().border(2.dp, Color.White, RectangleShape)
            ) {
                Text("GRANT_ACCESS >>", color = Color.Black, style = Y2KTypography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}