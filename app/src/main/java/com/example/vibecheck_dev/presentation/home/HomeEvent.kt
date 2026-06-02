package com.example.vibecheck_dev.presentation.home

import android.net.Uri

sealed class HomeEvent {
    data class UpdateProfileImage(val uri: Uri) : HomeEvent()
    data class UpdateUsername(val username: String) : HomeEvent()
    data class UpdatePassword(val password: String) : HomeEvent()
    object SaveProfile : HomeEvent()
}