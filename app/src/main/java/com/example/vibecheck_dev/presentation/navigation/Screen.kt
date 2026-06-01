package com.example.vibecheck_dev.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

// Tambahin nullable icon karena layar onboarding ga butuh icon di Bottom Nav
sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Onboarding : Screen("onboarding_screen", "ONBOARDING", null)

    object Auth : Screen("auth_screen", "AUTH", null)
    object Login : Screen("login_screen", "LOGIN", null)
    object ProfileSetup : Screen("profile_setup_screen", "PROFILE", null)
    object Permission : Screen("permission_screen", "PERMISSION", null)

    // Rute buat Bottom Nav
    object Home : Screen("home_screen", "HOME", Icons.Default.Home)
    object Host : Screen("camera_screen", "HOST", Icons.Default.Camera)
    object Remote : Screen("remote_screen", "REMOTE", Icons.Default.PhoneAndroid)

    object Studio : Screen("studio_screen", "STUDIO", Icons.Default.Edit) // Import Icons.Default.Edit

    object Purikura : Screen("purikura_screen", "PURIKURA", Icons.Default.PhotoLibrary) // Import Icons.Default.PhotoLibrary
}