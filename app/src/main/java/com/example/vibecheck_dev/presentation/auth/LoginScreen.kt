package com.example.vibecheck_dev.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.data.local.UserPreferences
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userPreferences: UserPreferences
) {
    // State untuk nge-toggle antara Login atau Sign Up
    var isLoginMode by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") } // Khusus buat Sign Up
    var errorMessage by remember { mutableStateOf("") }

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
                .background(Color.DarkGray.copy(alpha = 0.2f))
                .padding(24.dp)
        ) {
            // Judul berubah otomatis sesuai mode
            Text(
                text = if (isLoginMode) "ACCESS_DB.exe" else "NEW_NODE_REG.exe",
                style = Y2KTypography.titleLarge,
                color = Color.Magenta,
                // TAMBAHKAN MODIFIER GLITCH DI SINI
                modifier = Modifier.y2kGlitchEffect()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isLoginMode) "Konek ke cloud buat buka fitur Photobooth & Filter Premium."
                else "Daftarin node lu ke jaringan cloud VibeCheck.",
                style = Y2KTypography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Input Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                placeholder = {
                    Text("EMAIL", style = Y2KTypography.bodyMedium, color = Color.Gray)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Magenta,
                    unfocusedBorderColor = Color.White,
                    cursorColor = Color.Cyan
                ),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                placeholder = {
                    Text("PASSWORD", style = Y2KTypography.bodyMedium, color = Color.Gray)
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Magenta,
                    unfocusedBorderColor = Color.White,
                    cursorColor = Color.Cyan
                ),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            )

            // Input Konfirmasi Password (Hanya muncul kalau lagi mode Sign Up)
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = "" },
                    textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                    placeholder = {
                        Text("CONFIRM_PASSWORD", style = Y2KTypography.bodyMedium, color = Color.Gray)
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Magenta,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.Cyan
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Teks Error (kalau password nggak sama pas Sign Up)
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = Y2KTypography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Action (Berubah teksnya sesuai mode)
            Button(
                onClick = {
                    if (!isLoginMode && password != confirmPassword) {
                        errorMessage = "ERR: PASSWORD_MISMATCH"
                        return@Button
                    }

                    if (email.isNotBlank() && password.isNotBlank()) {
                        coroutineScope.launch {
                            // TODO: Eksekusi Supabase Auth (signInWith / signUp)

                            userPreferences.saveAuthSession("dummy_token_123", true)
                            userPreferences.savePlayerName(email.substringBefore("@"))
                            onLoginSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Magenta,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White, RectangleShape)
            ) {
                Text(
                    text = if (isLoginMode) "INITIALIZE >>" else "CREATE_NODE >>",
                    color = Color.White,
                    style = Y2KTypography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Toggle Login <-> Sign Up
            Text(
                text = if (isLoginMode) "Belum punya akses? [ REGISTER ]" else "Udah terdaftar? [ LOGIN ]",
                style = Y2KTypography.bodySmall,
                color = Color.Yellow,
                modifier = Modifier.clickable {
                    isLoginMode = !isLoginMode
                    errorMessage = "" // Bersihin error pas ganti mode
                }
            )
        }
    }
}