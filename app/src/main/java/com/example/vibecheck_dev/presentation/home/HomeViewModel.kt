package com.example.vibecheck_dev.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibecheck_dev.data.remote.network.ImgBBApi
import com.example.vibecheck_dev.domain.repository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class HomeViewModel(
    private val auth: FirebaseAuth,
    private val imgBBApi: ImgBBApi, // 🔴 Pake ImgBBApi sebagai pengganti Firebase Storage
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // 🔴 PASTE API KEY IMGBB LU DI SINI
    private val imgBbApiKey = "c41234255ea37a11c26db297543364d3"

    init { loadFirebaseUser() }

    private fun loadFirebaseUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 1. Tampilkan data dari Cache dulu biar UI gak kosong
            val isGoogle = currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            _uiState.update {
                it.copy(
                    username = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "USER",
                    profileImageUri = currentUser.photoUrl,
                    isGoogleLogin = isGoogle
                )
            }

            // 2. 🔴 PAKSA REFRESH KE SERVER FIREBASE
            // Biar URL ImgBB terbaru ketarik pas relogin
            viewModelScope.launch {
                try {
                    currentUser.reload().await() // Download ulang profil
                    val freshUser = auth.currentUser // Ambil data yang udah fresh
                    if (freshUser != null) {
                        _uiState.update {
                            it.copy(
                                username = freshUser.displayName ?: it.username,
                                profileImageUri = freshUser.photoUrl // Update dengan URL terbaru
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Kalau gagal refresh (misal ga ada sinyal), biarin pake cache
                }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UpdateProfileImage -> _uiState.update { it.copy(profileImageUri = event.uri) }
            is HomeEvent.UpdateUsername -> _uiState.update { it.copy(username = event.username) }
            is HomeEvent.UpdateOldPassword -> _uiState.update { it.copy(oldPassword = event.password) }
            is HomeEvent.UpdateNewPassword -> _uiState.update { it.copy(newPassword = event.password) }
            is HomeEvent.ResetSaveStatus -> _uiState.update { it.copy(saveSuccess = false, saveError = null) }
            is HomeEvent.SaveProfile -> saveProfileToFirebase(event.context)
            is HomeEvent.FetchLogs -> fetchLogsFromServer()
        }
    }

    private fun saveProfileToFirebase(context: android.content.Context) {
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("Session expired.")
                var finalPhotoUrl: String? = null

                // 🔴 PERBAIKAN 1: Cek apakah URI bukan link internet (berarti dari Galeri HP)
                val currentUri = _uiState.value.profileImageUri

                if (currentUri != null && !currentUri.toString().startsWith("http")) {

                    val inputStream = context.contentResolver.openInputStream(currentUri)
                    val bytes = inputStream?.readBytes() ?: throw Exception("Gagal membaca gambar dari Galeri")
                    inputStream.close()

                    // 🔴 PERBAIKAN: Bungkus byte mentah ke dalam Multipart, 100% Anti-Corrupt!
                    val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val multipartImage = MultipartBody.Part.createFormData("image", "profile.jpg", requestBody)

                    // Lempar Multipart ke ImgBB
                    val imgBbResponse = imgBBApi.uploadImage(imgBbApiKey, multipartImage)

                    if (imgBbResponse.isSuccessful && imgBbResponse.body()?.success == true) {
                        // Dapat URL internetnya! (https://i.ibb.co/...)
                        finalPhotoUrl = imgBbResponse.body()?.data?.display_url ?: imgBbResponse.body()?.data?.url
                        android.util.Log.d("VIBECHECK_DEBUG", "Sukses dapet URL ImgBB: $finalPhotoUrl")
                    } else {
                        throw Exception("Gagal upload gambar ke ImgBB Server")
                    }
                }

                // ==========================================
                // UPDATE FIREBASE AUTH METADATA
                // ==========================================
                val profileUpdates = userProfileChangeRequest {
                    if (_uiState.value.username.isNotBlank()) displayName = _uiState.value.username
                    if (finalPhotoUrl != null) photoUri = Uri.parse(finalPhotoUrl)
                }
                user.updateProfile(profileUpdates).await()

                // ==========================================
                // INTEGRASI BACKEND VERCEL
                // ==========================================
                if (_uiState.value.username.isNotBlank()) {
                    val nameResult = userRepository.updateUsername(_uiState.value.username)
                    if (nameResult.isFailure) throw Exception("Username gagal sync: ${nameResult.exceptionOrNull()?.message}")
                }

                if (finalPhotoUrl != null) {
                    val photoResult = userRepository.updateProfilePicture(finalPhotoUrl)
                    if (photoResult.isFailure) throw Exception("Foto gagal sync: ${photoResult.exceptionOrNull()?.message}")
                }

                // ==========================================
                // UPDATE PASSWORD (Khusus Email)
                // ==========================================
                if (!_uiState.value.isGoogleLogin && _uiState.value.newPassword.isNotBlank()) {
                    if (_uiState.value.oldPassword.isBlank()) throw Exception("Old Password wajib diisi.")
                    val credential = EmailAuthProvider.getCredential(user.email!!, _uiState.value.oldPassword)
                    user.reauthenticate(credential).await()
                    user.updatePassword(_uiState.value.newPassword).await()
                }

                // ==========================================
                // UPDATE PASSWORD (Khusus Email)
                // ==========================================
                if (!_uiState.value.isGoogleLogin && _uiState.value.newPassword.isNotBlank()) {
                    if (_uiState.value.oldPassword.isBlank()) throw Exception("Old Password wajib diisi.")
                    val credential = EmailAuthProvider.getCredential(user.email!!, _uiState.value.oldPassword)
                    user.reauthenticate(credential).await()
                    user.updatePassword(_uiState.value.newPassword).await()

                    // 🔴 TAMBAHIN INI BIAR GANTI PASSWORD KECATET!
                    userRepository.addClientLog("SEC_UPDATE", "Update: Password berhasil diubah")
                }

                _uiState.update {
                    it.copy(isSaving = false, saveSuccess = true, oldPassword = "", newPassword = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.localizedMessage) }
            }
        }
    }

    private fun fetchLogsFromServer() {
        _uiState.update { it.copy(isLoadingLogs = true, logsError = null) }
        viewModelScope.launch {
            val result = userRepository.getUserLogs()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoadingLogs = false,
                        userLogs = result.getOrDefault(emptyList())
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingLogs = false,
                        logsError = result.exceptionOrNull()?.message ?: "Gagal memuat log"
                    )
                }
            }
        }
    }
}