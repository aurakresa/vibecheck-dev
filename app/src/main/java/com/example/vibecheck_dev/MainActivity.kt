package com.example.vibecheck_dev

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vibecheck_dev.data.local.UserPreferences
import com.example.vibecheck_dev.presentation.navigation.Screen
import org.koin.androidx.compose.koinViewModel
import com.example.vibecheck_dev.ui.theme.VibeCheckdevTheme
import com.example.vibecheck_dev.presentation.permission.PermissionScreen
import com.example.vibecheck_dev.presentation.home.HomeScreen
import com.example.vibecheck_dev.presentation.camera.CameraScreen
import com.example.vibecheck_dev.presentation.camera.CameraViewModel
import com.example.vibecheck_dev.presentation.remote.RemoteScreen
import com.example.vibecheck_dev.presentation.remote.RemoteViewModel
import com.example.vibecheck_dev.presentation.components.VibeBottomNav
import com.example.vibecheck_dev.presentation.auth.AuthScreen
import com.example.vibecheck_dev.presentation.auth.LoginScreen
import com.example.vibecheck_dev.presentation.auth.OnboardingScreen
import com.example.vibecheck_dev.presentation.auth.ProfileSetupScreen
import com.example.vibecheck_dev.presentation.studio.StudioScreen
import com.example.vibecheck_dev.presentation.studio.StudioViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FIKAL_DEBUG", "Aplikasi VibeCheck Mulai Berjalan!")

        setContent {
            VibeCheckdevTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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
    val coroutineScope = rememberCoroutineScope()

    val userPreferences = remember { UserPreferences(context) }

    val isFirstTime by userPreferences.isFirstTimeFlow.collectAsState(initial = null)
    val playerName by userPreferences.playerNameFlow.collectAsState(initial = null)
    val isLoggedIn by userPreferences.isLoggedInFlow.collectAsState(initial = null)

    if (isFirstTime == null || isLoggedIn == null) {
        return // Tunggu DataStore kelar baca
    }

    // LOGIKA PENENTUAN GUEST MODE
    // Guest = Udah punya nama (lewat setup), tapi status login-nya false
    val isGuestMode = !playerName.isNullOrEmpty() && isLoggedIn == false
    // User Asli = Status login-nya true
    val isRealUser = isLoggedIn == true

    // ATURAN BOTTOM NAVIGATION
    // Bottom Nav TAMPIL kalau:
    // 1. Lagi di layar Home, Studio, atau Purikura
    // DAN
    // 2. BUKAN Guest (Artinya User Asli)
    val isMainScreen = currentRoute in listOf(Screen.Home.route, Screen.Studio.route, Screen.Purikura.route)
    val showBottomNav = isMainScreen && isRealUser
    // ^ Guest Mode TIDAK AKAN melihat bottom nav karena showBottomNav = false

    // LOGIKA ROUTING PINTAR:
    val startDestination = when {
        isFirstTime == true -> Screen.Onboarding.route
        isRealUser || isGuestMode -> Screen.Splash.route // Baik user asli maupun guest yang udah setup, bypass permission dan masuk splash
        else -> Screen.Auth.route // Belum first time, tapi belum milih mode
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) VibeBottomNav(navController)
        }) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Splash.route) {
                com.example.vibecheck_dev.presentation.splash.SplashScreen(navController = navController)
            }

            // --- LAYAR SETUP & ONBOARDING ---
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    })
            }

            composable(Screen.Auth.route) {
                AuthScreen(
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToGuest = { navController.navigate(Screen.ProfileSetup.route) }
                )
            }

            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onSaveSuccess = {
                        // Guest Baru -> Habis setup lempar ke Permission
                        navController.navigate(Screen.Permission.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }, userPreferences = userPreferences
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        // User Baru -> Habis login lempar ke Permission
                        navController.navigate(Screen.Permission.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }, userPreferences = userPreferences
                )
            }

            // --- LAYAR UTAMA ---
            composable(Screen.Permission.route) {
                PermissionScreen(
                    onAllPermissionsGranted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    })
            }

            composable(Screen.Home.route) {
                val isGuestMode = !playerName.isNullOrEmpty() && isLoggedIn == false

                HomeScreen(
                    onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                    onNavigateToRemote = { navController.navigate(Screen.Remote.route) },
                    onLogout = {
                        coroutineScope.launch {
                            // FIX: Pakai saveAuthSession dan kosongin tokennya
                            userPreferences.saveAuthSession(token = "", isLogged = false)
                            userPreferences.savePlayerName("")

                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true } // Kill semua backstack
                            }
                        }
                    },
                    isGuestMode = isGuestMode,
                    guestName = playerName ?: "GUEST_USER"
                )
            }

            composable(Screen.Camera.route) {
                val cameraViewModel: CameraViewModel = koinViewModel()
                CameraScreen(viewModel = cameraViewModel, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Remote.route) {
                val remoteViewModel: RemoteViewModel = koinViewModel()
                RemoteScreen(viewModel = remoteViewModel, onNavigateBack = { navController.popBackStack() })
            }

            // STUDIO & PURIKURA (Sebenarnya Guest gak bisa ke sini karena bottom nav-nya ilang, tapi buat jaga-jaga kalau dipanggil via deep link dll)
            composable(Screen.Studio.route) {
                if (isRealUser) {
                    val studioViewModel: StudioViewModel = koinViewModel()
                    StudioScreen(viewModel = studioViewModel)
                }
            }

            composable(Screen.Purikura.route) {
                if (isRealUser) {
                    val purikuraViewModel: com.example.vibecheck_dev.presentation.purikura.PurikuraViewModel = koinViewModel()
                    com.example.vibecheck_dev.presentation.purikura.PurikuraScreen(viewModel = purikuraViewModel)
                }
            }
        }
    }
}