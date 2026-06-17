package com.example.vibecheck_dev.data.remote.network

import com.example.vibecheck_dev.data.remote.dto.ImgBBResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ImgBBApi {
    // 🔴 KITA PAKAI MULTIPART SESUAI DOKUMENTASI IMGBB
    @Multipart
    @POST("1/upload")
    suspend fun uploadImage(
        @Query("key") apiKey: String,
        @Part image: MultipartBody.Part // Kirim file mentah, bukan string Base64
    ): Response<ImgBBResponse>
}