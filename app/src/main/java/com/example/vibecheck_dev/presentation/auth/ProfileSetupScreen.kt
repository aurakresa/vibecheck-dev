package com.example.vibecheck_dev.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import com.example.vibecheck_dev.data.local.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    userPreferences: UserPreferences,
    onSaveSuccess: () -> Unit // Fungsi buat lanjut ke Home setelah save
) {
    var nickname by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

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
                .background(Color.DarkGray.copy(alpha = 0.3f))
                .padding(32.dp)
        ) {
            Text(
                text = "NEW_PLAYER.exe",
                style = Y2KTypography.titleLarge,
                color = Color.Green
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Masukkan Nickname lu. Nama ini bakal muncul di radar P2P biar temen lu gampang nyarinya.",
                style = Y2KTypography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Input Retro Y2K
            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 12) nickname = it }, // Batasin 12 huruf biar ga kepanjangan
                textStyle = Y2KTypography.bodyLarge.copy(color = Color.Cyan),
                placeholder = {
                    Text("PLAYER_1", style = Y2KTypography.bodyMedium, color = Color.Gray)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Magenta,
                    unfocusedBorderColor = Color.White,
                    cursorColor = Color.Cyan
                ),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Simpan
            Button(
                onClick = {
                    if (nickname.isNotBlank()) {
                        coroutineScope.launch {
                            userPreferences.savePlayerName(nickname)
                            userPreferences.setFirstTimeCompleted()
                            onSaveSuccess()
                        }
                    }
                },
                enabled = nickname.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Magenta,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, if(nickname.isNotBlank()) Color.White else Color.Gray, RectangleShape)
            ) {
                Text(
                    text = "SAVE & BOOT >>",
                    color = if(nickname.isNotBlank()) Color.White else Color.Gray,
                    style = Y2KTypography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}