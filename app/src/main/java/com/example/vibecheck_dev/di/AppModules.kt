package com.example.vibecheck_dev.di

import com.example.vibecheck_dev.data.remote.network.AuthInterceptor
import com.example.vibecheck_dev.data.remote.network.ImgBBApi
import com.example.vibecheck_dev.data.remote.network.VibeCheckApi
import com.example.vibecheck_dev.data.repository_impl.P2pRepositoryImpl
import com.example.vibecheck_dev.data.repository_impl.UserRepositoryImpl
import com.example.vibecheck_dev.domain.repository.P2pRepository
import com.example.vibecheck_dev.domain.repository.UserRepository
import com.example.vibecheck_dev.presentation.auth.AuthViewModel
import com.example.vibecheck_dev.presentation.camera.CameraViewModel
import com.example.vibecheck_dev.presentation.home.HomeViewModel
import com.example.vibecheck_dev.presentation.purikura.PurikuraViewModel
import com.example.vibecheck_dev.presentation.remote.RemoteViewModel
import com.example.vibecheck_dev.presentation.studio.StudioViewModel
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single<P2pRepository> { P2pRepositoryImpl(androidContext()) }

    // --- TAMBAHAN UNTUK AUTH ---
    single { FirebaseAuth.getInstance() }

    viewModel { CameraViewModel(get()) }
    viewModel { RemoteViewModel(get()) }
    viewModel { StudioViewModel() }
    viewModel { PurikuraViewModel() }
    // --- TAMBAHAN UNTUK AUTH ---
    viewModel { AuthViewModel(get(), get()) }
    single { AuthInterceptor(get()) }

    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Biar bisa liat log API di Logcat
        }
        OkHttpClient.Builder()
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single {
        Retrofit.Builder()
            // 🔴 URL INI NANTI GANTI PAKE URL VERCEL LU KALAU UDAH DEPLOY (misal: https://vibecheck.vercel.app/)
            .baseUrl("https://vibecheck-backend-jshl.vercel.app/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<ImgBBApi> {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://api.imgbb.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ImgBBApi::class.java)
    }

    single<VibeCheckApi> { get<Retrofit>().create(VibeCheckApi::class.java) }

    // --- REPOSITORY ---
    single<UserRepository> { UserRepositoryImpl(get()) }

    // --- VIEW MODEL UPDATE ---
    // Ubah HomeViewModel lu biar nerima UserRepository juga
    viewModel { HomeViewModel(get(), get(), get()) }
}