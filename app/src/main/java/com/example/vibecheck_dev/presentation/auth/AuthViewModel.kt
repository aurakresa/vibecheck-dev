package com.example.vibecheck_dev.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibecheck_dev.domain.repository.UserRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val uid: String, val email: String, val displayName: String?) : AuthState()
    data class Error(val message: String) : AuthState()
    data class VerificationSent(val message: String) : AuthState() // 🔴 State Khusus Verifikasi
}

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun resetState() {
        _authState.update { AuthState.Idle }
    }

    fun signUpWithEmail(email: String, pass: String, username: String) {
        _authState.update { AuthState.Loading }
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = username
                    }
                    user.updateProfile(profileUpdates).await()

                    // 🔴 Kirim email verifikasi & langsung paksa Logout
                    user.sendEmailVerification().await()
                    auth.signOut()

                    _authState.update { AuthState.VerificationSent("REGISTRASI SUKSES! Cek Inbox/Spam email lu untuk aktivasi.") }
                }
            } catch (e: Exception) {
                _authState.update { AuthState.Error(e.localizedMessage ?: "Gagal Register") }
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        _authState.update { AuthState.Loading }
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    if (user.isEmailVerified) {

                        // 🔴 1. TANGKAP NAMA HP ALA INSTAGRAM
                        // 🔴 TANGKAP NAMA HP LALU KIRIM KE VERCEL
                        val merk = android.os.Build.MANUFACTURER.uppercase()
                        val tipe = android.os.Build.MODEL
                        val deviceName = "$merk $tipe"

                        try {
                            // Kirim deviceName di parameter ke-3
                            userRepository.addClientLog("SEC_LOGIN", "Login aktif pada $deviceName", deviceName)
                        } catch (e: Exception) { /* Biarin lolos kalau error */ }

                        _authState.update { AuthState.Success(user.uid, user.email ?: "", user.displayName) }
                    } else {
                        auth.signOut()
                        _authState.update { AuthState.Error("ACCESS DENIED: Email belum diverifikasi! Cek Inbox/Spam lu.") }
                    }
                }
            } catch (e: Exception) {
                _authState.update { AuthState.Error("Login Gagal: Email atau Password salah") }
            }
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        _authState.update { AuthState.Loading }
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user != null) {
                    // 🔴 TANGKAP NAMA HP BUAT GOOGLE LOGIN
                    val merk = android.os.Build.MANUFACTURER.uppercase()
                    val tipe = android.os.Build.MODEL
                    val deviceName = "$merk $tipe"

                    try {
                        userRepository.addClientLog("SEC_LOGIN", "Sesi aktif (Google) pada $deviceName", deviceName)
                    } catch (e: Exception) { /* Abaikan jika gagal */ }

                    _authState.update { AuthState.Success(user.uid, user.email ?: "", user.displayName) }
                }
            } catch (e: Exception) {
                _authState.update { AuthState.Error(e.localizedMessage ?: "Google Login Gagal") }
            }
        }
    }
}