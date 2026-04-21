package com.example.vibecheck_dev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibecheck_dev.ui.theme.VibeCheckdevTheme
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun CameraHostScreen(
    serverIp: String,
    onStartServer: () -> Unit
) {
    // Background Hitam Pekat
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        // TODO: Nanti AndroidView CameraX ditaruh di sini

        // UI Overlay Retro
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Status Baterai & Rekaman
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REC \u25CF",
                    color = Color.Red,
                    style = Y2KTypography.titleLarge // GANTI PAKAI INI
                )
                Text(
                    text = "BATT: 99%",
                    color = Color(0xFF00FF00),
                    style = Y2KTypography.bodyMedium // GANTI PAKAI INI
                )
            }

            // Footer: Kotak IP & Tombol
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                // Kotak IP ala DOS
                Box(
                    modifier = Modifier
                        .border(4.dp, Color.White, RectangleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (serverIp.isEmpty()) "SYSTEM STANDBY..." else "IP: $serverIp:8080",
                        color = Color(0xFF00FF00),
                        style = TextStyle(fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Mulai yang kaku (Nggak ada sudut melengkung)
                Button(
                    onClick = onStartServer,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
                ) {
                    Text(
                        text = "START HOST SERVER",
                        color = Color.White,
                        style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

// ==========================================
// INI BAGIAN AJAIBNYA: PREVIEW DI ANDROID STUDIO
// ==========================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CameraHostScreenPreview() {
    // BUNGKUS PAKAI TEMA INI BIAR FONT RETRO-NYA MUNCUL!
    VibeCheckdevTheme {
        CameraHostScreen(
            serverIp = "192.168.43.1",
            onStartServer = { /* Do nothing di preview */ }
        )
    }
}