package com.example.vibecheck_dev.presentation.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.R
import com.example.vibecheck_dev.data.local.UserPreferences
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userPreferences: UserPreferences,
    viewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // 🔴 Perbaikan Sistem Pesan Error & Sukses
    var localMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(true) }

    val webClientId = stringResource(id = R.string.default_web_client_id)
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.signInWithGoogle(credential)
            } catch (e: ApiException) {
                isError = true
                localMessage = "ERR: GOOGLE_AUTH_FAILED"
            }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val successState = authState as AuthState.Success
                val dummyDisplayName = successState.displayName ?: successState.email.substringBefore("@")
                userPreferences.saveAuthSession(token = successState.uid, isLogged = true)
                userPreferences.savePlayerName(dummyDisplayName)
                viewModel.resetState()
                onLoginSuccess()
            }
            is AuthState.Error -> {
                isError = true
                localMessage = (authState as AuthState.Error).message
            }
            is AuthState.VerificationSent -> {
                isError = false
                localMessage = (authState as AuthState.VerificationSent).message
                isLoginMode = true // Kembalikan ke mode login
                password = ""
                confirmPassword = ""
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.border(2.dp, Color.White, RectangleShape).background(Color.DarkGray.copy(alpha = 0.2f)).padding(24.dp)
        ) {
            Text(
                text = if (isLoginMode) "ACCESS_DB.exe" else "NEW_NODE_REG.exe",
                style = Y2KTypography.titleLarge,
                color = Color.Magenta,
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

            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; localMessage = "" },
                    textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                    placeholder = { Text("USERNAME", style = Y2KTypography.bodyMedium, color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Magenta, unfocusedBorderColor = Color.White, cursorColor = Color.Cyan),
                    shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localMessage = "" },
                textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                placeholder = { Text("EMAIL", style = Y2KTypography.bodyMedium, color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Magenta, unfocusedBorderColor = Color.White, cursorColor = Color.Cyan),
                shape = RectangleShape, modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localMessage = "" },
                textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                placeholder = { Text("PASSWORD", style = Y2KTypography.bodyMedium, color = Color.Gray) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, contentDescription = "Toggle password", tint = Color.Cyan) }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Magenta, unfocusedBorderColor = Color.White, cursorColor = Color.Cyan),
                shape = RectangleShape, modifier = Modifier.fillMaxWidth()
            )

            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localMessage = "" },
                    textStyle = Y2KTypography.bodyMedium.copy(color = Color.Cyan),
                    placeholder = { Text("CONFIRM_PASSWORD", style = Y2KTypography.bodyMedium, color = Color.Gray) },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(imageVector = image, contentDescription = "Toggle confirm password", tint = Color.Cyan) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Magenta, unfocusedBorderColor = Color.White, cursorColor = Color.Cyan),
                    shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                )
            }

            if (localMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // 🔴 Warnanya dinamis: Hijau kalau sukses verifikasi, Merah kalau error
                Text(text = localMessage, color = if(isError) Color.Red else Color.Green, style = Y2KTypography.bodySmall, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isLoading = authState is AuthState.Loading

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (!isLoginMode && username.isBlank())) {
                        isError = true; localMessage = "ERR: FIELD_EMPTY"; return@Button
                    }
                    if (!isLoginMode && password != confirmPassword) {
                        isError = true; localMessage = "ERR: PASSWORD_MISMATCH"; return@Button
                    }
                    if (isLoginMode) viewModel.signInWithEmail(email, password) else viewModel.signUpWithEmail(email, password, username)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta, disabledContainerColor = Color.DarkGray),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth().border(2.dp, if(isLoading) Color.Gray else Color.White, RectangleShape)
            ) {
                Text(text = if (isLoading) "PROCESSING..." else if (isLoginMode) "INITIALIZE >>" else "CREATE_NODE >>", color = Color.White, style = Y2KTypography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { googleSignInClient.signOut().addOnCompleteListener { googleAuthLauncher.launch(googleSignInClient.signInIntent) } },
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RectangleShape)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(painter = painterResource(id = R.drawable.ic_google), contentDescription = "Google Logo", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isLoginMode) "Belum punya akses? [ REGISTER ]" else "Udah terdaftar? [ LOGIN ]",
                style = Y2KTypography.bodySmall, color = Color.Yellow,
                modifier = Modifier.clickable { isLoginMode = !isLoginMode; localMessage = "" }
            )
        }
    }
}