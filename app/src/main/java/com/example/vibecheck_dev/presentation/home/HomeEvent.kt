package com.example.vibecheck_dev.presentation.home

import android.net.Uri

sealed class HomeEvent {
    data class UpdateProfileImage(val uri: Uri) : HomeEvent()
    data class UpdateUsername(val username: String) : HomeEvent()
    data class UpdateOldPassword(val password: String) : HomeEvent()
    data class UpdateNewPassword(val password: String) : HomeEvent()
    data class SaveProfile(val context: android.content.Context) : HomeEvent()
    object ResetSaveStatus : HomeEvent()
    object FetchLogs : HomeEvent()
}