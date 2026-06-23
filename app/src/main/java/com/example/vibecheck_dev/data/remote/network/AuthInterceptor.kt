package com.example.vibecheck_dev.data.remote.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val auth: FirebaseAuth) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = auth.currentUser

        // Kalau guest (belum login), lanjutin request polosan
        if (user == null) {
            return chain.proceed(originalRequest)
        }

        return try {
            // Ambil token secara sinkron (karena OkHttp udah jalan di background thread)
            val tokenResult = Tasks.await(user.getIdToken(false))
            val token = tokenResult.token

            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

            chain.proceed(newRequest)
        } catch (e: Exception) {
            // Kalau gagal dapet token, lanjutin aja, nanti server Express nolak (401)
            chain.proceed(originalRequest)
        }
    }
}