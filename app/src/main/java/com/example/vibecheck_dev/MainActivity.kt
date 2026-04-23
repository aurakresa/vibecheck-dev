package com.example.vibecheck_dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Import semua layar dan komponen yang sudah kita buat sejauh ini
import com.example.vibecheck_dev.ui.theme.VibeCheckdevTheme
import com.example.vibecheck_dev.presentation.permission.PermissionScreen
import com.example.vibecheck_dev.presentation.home.HomeScreen
import com.example.vibecheck_dev.presentation.camera.CameraScreen
import com.example.vibecheck_dev.presentation.camera.CameraViewModel
import com.example.vibecheck_dev.presentation.remote.RemoteScreen
import com.example.vibecheck_dev.presentation.remote.RemoteViewModel
import com.example.vibecheck_dev.data.repository_impl.P2pRepositoryImpl

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    // Mesin utama navigasi layar
    val navController = rememberNavController()
    val context = LocalContext.current

    // 1. INISIALISASI MESIN UTAMA
    // Kita gunakan 'remember' agar mesin ini tidak dibuat ulang saat UI berubah
    val p2pRepository = remember { P2pRepositoryImpl(context) }

    // 2. INISIALISASI VIEWMODEL
    // Menyuntikkan repository ke dalam masing-masing ViewModel
    val cameraViewModel = remember { CameraViewModel(p2pRepository) }
    val remoteViewModel = remember { RemoteViewModel(p2pRepository) }

    NavHost(navController = navController, startDestination = "permission_screen") {

        // Layar 1: Minta Izin
        composable("permission_screen") {
            PermissionScreen(
                onAllPermissionsGranted = {
                    navController.navigate("home_screen") {
                        popUpTo("permission_screen") { inclusive = true }
                    }
                }
            )
        }

        // Layar 2: Beranda (Pilih Mode)
        composable("home_screen") {
            HomeScreen(
                onNavigateToCamera = { navController.navigate("camera_screen") },
                onNavigateToRemote = { navController.navigate("remote_screen") }
            )
        }

        // Layar 3: Mode HP ditaruh di tripod (Host/Kamera)
        composable("camera_screen") {
            CameraScreen(
                viewModel = cameraViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Layar 4: Mode HP dipegang tangan (Client/Remote)
        composable("remote_screen") {
            RemoteScreen(
                viewModel = remoteViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}