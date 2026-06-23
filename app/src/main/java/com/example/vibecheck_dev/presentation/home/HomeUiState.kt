package com.example.vibecheck_dev.presentation.home

import android.net.Uri
import com.example.vibecheck_dev.data.remote.dto.LogDto

data class HomeUiState(
    val profileImageUri: Uri? = null,
    val username: String = "",
    val oldPassword: String = "",
    val newPassword: String = "",
    val isSaving: Boolean = false,
    val isGoogleLogin: Boolean = false, // Buat nentuin form password muncul/enggak
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val userLogs: List<LogDto> = emptyList(),
    val isLoadingLogs: Boolean = false,
    val logsError: String? = null
)