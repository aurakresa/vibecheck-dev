package com.example.vibecheck_dev

import android.os.Bundle
import android.util.Log // 1. Import ini buat debugging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vibecheck_dev.data.local.UserPreferences
import com.example.vibecheck_dev.presentation.navigation.Screen

// Import Koin biar ViewModel lu otomatis disuntik
import org.koin.androidx.compose.koinViewModel

import com.example.vibecheck_dev.ui.theme.VibeCheckdevTheme
import com.example.vibecheck_dev.presentation.permission.PermissionScreen
import com.example.vibecheck_dev.presentation.home.HomeScreen
import com.example.vibecheck_dev.presentation.camera.CameraScreen
import com.example.vibecheck_dev.presentation.camera.CameraViewModel
import com.example.vibecheck_dev.presentation.remote.RemoteScreen
import com.example.vibecheck_dev.presentation.remote.RemoteViewModel
// HAPUS import P2pRepositoryImpl karena udah diurus Koin!

// Asumsi lu udah bikin VibeBottomNav dari panduan sebelumnya ya
import com.example.vibecheck_dev.presentation.components.VibeBottomNav
import com.example.vibecheck_dev.presentation.auth.AuthScreen
import com.example.vibecheck_dev.presentation.auth.LoginScreen
import com.example.vibecheck_dev.presentation.auth.OnboardingScreen
import com.example.vibecheck_dev.presentation.auth.ProfileSetupScreen
import com.example.vibecheck_dev.presentation.studio.StudioScreen
import com.example.vibecheck_dev.presentation.vault.VaultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FIKAL_DEBUG", "Aplikasi VibeCheck Mulai Berjalan!") // Contoh nulis log

        setContent {
            VibeCheckdevTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // 1. Panggil pengelola memori lokal yang udah kita buat
    val userPreferences = remember { UserPreferences(context) }

    // 2. Baca data dari memori (nilainya 'null' pas lagi proses baca)
    val isFirstTime by userPreferences.isFirstTimeFlow.collectAsState(initial = null)
    val playerName by userPreferences.playerNameFlow.collectAsState(initial = null)
    val isLoggedIn by userPreferences.isLoggedInFlow.collectAsState(initial = null)

    val showBottomNav = currentRoute in listOf(Screen.Home.route, Screen.Studio.route, Screen.Vault.route)
    // 3. Tampilkan layar hitam sebentar kalau data belum selesai dibaca
    if (isFirstTime == null || isLoggedIn == null) {
        // Tampilkan layar hitam kosong sementara memuat DataStore
        return
    }

    // 4. Logika Penentuan Layar Pertama (Routing Cerdas)
    val startDestination = when {
        isFirstTime == true -> Screen.Onboarding.route
        isLoggedIn == false -> Screen.Auth.route // Atau rute login lu
        else -> Screen.Permission.route // Nanti PermissionScreen yang ngecek otomatis kalau izin udah ada
    }

    Scaffold(
        bottomBar = {
            // Sembunyikan Bottom Nav di layar Onboarding, Setup, dan Permission
            if (showBottomNav) VibeBottomNav(navController)
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDestination, // <-- Pakai hasil logika di atas
            modifier = Modifier.padding(innerPadding)
        ) {

            // --- LAYAR SETUP & ONBOARDING ---
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        // UBAH JADI Screen.Auth.route
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Auth.route) {
                AuthScreen(
                    onNavigateToLogin = {
                        // Loncat ke layar form email/password (Bisa pakai Supabase Auth nanti)
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToGuest = {
                        // Masuk mode lokal, suruh isi nickname
                        navController.navigate(Screen.ProfileSetup.route)
                    }
                )
            }

            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onSaveSuccess = {
                        navController.navigate(Screen.Permission.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    userPreferences = userPreferences
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        // Kalau sukses login, lempar ke Permission
                        navController.navigate(Screen.Permission.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    userPreferences = userPreferences
                )
            }

            // --- LAYAR UTAMA (Sama kayak sebelumnya) ---
            composable(Screen.Permission.route) {
                PermissionScreen(
                    onAllPermissionsGranted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCamera = { navController.navigate(Screen.Host.route) },
                    onNavigateToRemote = { navController.navigate(Screen.Remote.route) }
                )
            }

            composable(Screen.Host.route) {
                val cameraViewModel: CameraViewModel = koinViewModel()
                CameraScreen(
                    viewModel = cameraViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Remote.route) {
                val remoteViewModel: RemoteViewModel = koinViewModel()
                RemoteScreen(
                    viewModel = remoteViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Studio.route) {
                StudioScreen()
            }
            composable(Screen.Vault.route) {
                VaultScreen()
            }
        }
    }
}