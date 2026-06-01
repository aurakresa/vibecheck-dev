package com.example.vibecheck_dev.di

import com.example.vibecheck_dev.data.repository_impl.P2pRepositoryImpl
import com.example.vibecheck_dev.domain.repository.P2pRepository
import com.example.vibecheck_dev.presentation.camera.CameraViewModel
import com.example.vibecheck_dev.presentation.purikura.PurikuraViewModel
import com.example.vibecheck_dev.presentation.remote.RemoteViewModel
import com.example.vibecheck_dev.presentation.studio.StudioViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    // 1. REPOSITORY (Sebagai Singleton: Hanya ada 1 objek untuk seluruh aplikasi)
    single<P2pRepository> {
        // Koin otomatis menyuntikkan 'Context' Android ke dalam P2pRepositoryImpl
        P2pRepositoryImpl(androidContext())
    }

    // 2. VIEWMODELS (Koin otomatis menyuntikkan P2pRepository ke dalamnya)
    viewModel { CameraViewModel(get()) }
    viewModel { RemoteViewModel(get()) }
    viewModel { StudioViewModel() }
    viewModel { PurikuraViewModel() }
}