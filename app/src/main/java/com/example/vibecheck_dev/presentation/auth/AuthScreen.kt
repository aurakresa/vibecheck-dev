package com.example.vibecheck_dev.presentation.auth


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.presentation.components.TypewriterText
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun AuthScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToGuest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .border(2.dp, Color.White, RectangleShape)
                .background(Color.DarkGray.copy(alpha = 0.2f))
                .padding(32.dp)
        ) {
            Text(
                text = "SYSTEM_LOGIN.exe",
                style = Y2KTypography.titleLarge,
                color = Color.Green,
                // TAMBAHKAN MODIFIER GLITCH DI SINI
                modifier = Modifier.y2kGlitchEffect()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pilih mode akses lu. Login buat buka semua fitur premium, atau lanjut lokal sebagai Guest.",
                style = Y2KTypography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Tombol Login (Premium)
            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White, RectangleShape)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "CONNECT SERVER (LOGIN)",
                    color = Color.White,
                    style = Y2KTypography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Tombol Guest Mode (Sesuai REQ-AUTH02)
            OutlinedButton(
                onClick = onNavigateToGuest,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Cyan),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Cyan, RectangleShape)
            ) {
                Text(
                    text = "GUEST MODE (LOKAL)",
                    color = Color.Cyan,
                    style = Y2KTypography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}