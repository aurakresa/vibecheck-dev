package com.example.vibecheck_dev.domain.repository

import com.example.vibecheck_dev.data.remote.dto.LogDto

interface UserRepository {
    suspend fun updateUsername(username: String): Result<String>
    suspend fun updateProfilePicture(photoUrl: String): Result<String> // 🔴 Tambahan
    suspend fun getUserLogs(): Result<List<LogDto>>
    suspend fun addClientLog(action: String, details: String, deviceName: String? = null): Result<Unit>}