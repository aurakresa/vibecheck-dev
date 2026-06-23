package com.example.vibecheck_dev.data.remote.network

import com.example.vibecheck_dev.data.remote.dto.AddLogRequest
import com.example.vibecheck_dev.data.remote.dto.BaseResponse
import com.example.vibecheck_dev.data.remote.dto.LogDto
import com.example.vibecheck_dev.data.remote.dto.UpdateUsernameRequest
import com.example.vibecheck_dev.data.remote.dto.UpdateProfilePictureRequest // Tambahan
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface VibeCheckApi {
    @PUT("api/users/username")
    suspend fun updateUsername(@Body request: UpdateUsernameRequest): Response<BaseResponse<Any>>

    // 🔴 TAMBAHAN ENDPOINT FOTO
    @PUT("api/users/profile-picture")
    suspend fun updateProfilePicture(@Body request: UpdateProfilePictureRequest): Response<BaseResponse<Any>>

    // 🔴 TAMBAHAN ENDPOINT UNTUK TARIK DATA ACTIVITY LOG
    @GET("api/users/logs")
    suspend fun getUserLogs(): Response<BaseResponse<List<LogDto>>>

    @POST("api/users/logs")
    suspend fun addClientLog(@Body request: AddLogRequest): Response<BaseResponse<Any>>
}