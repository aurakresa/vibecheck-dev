package com.example.vibecheck_dev.presentation.home

import android.net.Uri

data class HomeUiState(
    val profileImageUri: Uri? = null,
    val username: String = "GUEST_USER",
    val password: String = "",
    val isSaving: Boolean = false
)