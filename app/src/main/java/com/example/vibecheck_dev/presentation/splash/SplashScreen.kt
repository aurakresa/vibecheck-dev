package com.example.vibecheck_dev.presentation.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.presentation.navigation.Screen
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.delay

// Logo Pixel Art Mini (Disket / Save Icon Retro)
val PIXEL_DISK = listOf(
    " XXXXXXXX ",
    " X    X X ",
    " X    XXX ",
    " X      X ",
    " X XXXX X ",
    " X X  X X ",
    " XXXXXXXX "
)

@Composable
fun SplashScreen(navController: NavController) {
    var progress by remember { mutableIntStateOf(0) }
    var terminalLogs by remember { mutableStateOf("") }
    var showLogo by remember { mutableStateOf(false) }

    // Efek Animasi Booting Terminal
    LaunchedEffect(Unit) {
        val logs = listOf(
            "> INITIALIZING VIBECHECK_OS...",
            "> MOUNTING /vault/purikura...",
            "> LOADING ML_KIT KERNEL...",
            "> ESTABLISHING P2P PROTOCOL...",
            "> SYSTEM READY."
        )

        delay(300) // Jeda awal
        showLogo = true

        for (i in 1..100) {
            delay(25) // Kecepatan loading (total sekitar 2.5 detik)
            progress = i

            // Munculin log berdasarkan persentase
            when (i) {
                10 -> terminalLogs += logs[0] + "\n"
                35 -> terminalLogs += logs[1] + "\n"
                60 -> terminalLogs += logs[2] + "\n"
                85 -> terminalLogs += logs[3] + "\n"
                100 -> terminalLogs += logs[4] + "\n"
            }
        }

        delay(800) // Tahan sebentar di 100% biar user sempet baca "SYSTEM READY"

        // Pindah ke HomeScreen dan hancurkan SplashScreen dari backstack (biar kalau di-back gak balik ke loading)
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(4.dp, Color.DarkGray, RectangleShape)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LOGO GLITCH
            if (showLogo) {
                Canvas(modifier = Modifier.size(60.dp).aspectRatio(1f).y2kGlitchEffect()) {
                    val rows = PIXEL_DISK.size
                    val cols = PIXEL_DISK.maxOf { it.length }
                    val pixelWidth = size.width / cols
                    val pixelHeight = size.height / rows

                    for (r in 0 until rows) {
                        for (c in 0 until PIXEL_DISK[r].length) {
                            if (PIXEL_DISK[r][c] == 'X') {
                                drawRect(
                                    color = Color.Magenta,
                                    topLeft = Offset(c * pixelWidth, r * pixelHeight),
                                    size = Size(pixelWidth, pixelHeight)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // JUDUL
            Text(
                text = "VIBECHECK_OS",
                color = Color.Cyan,
                style = Y2KTypography.titleLarge,
                modifier = Modifier.y2kBlinkEffect(400)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // LOG TERMINAL (Rata Kiri ala CMD)
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.BottomStart) {
                Text(
                    text = terminalLogs,
                    color = Color.Green,
                    style = Y2KTypography.bodySmall.copy(lineHeight = 18.sp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PROGRESS BAR RETRO [██████----]
            val totalBars = 20
            val filledBars = (progress / 100f * totalBars).toInt()
            val emptyBars = totalBars - filledBars
            val barString = "[${"█".repeat(filledBars)}${"-".repeat(emptyBars)}] $progress%"

            Text(
                text = barString,
                color = if (progress == 100) Color.Green else Color.Magenta,
                style = Y2KTypography.bodyMedium
            )
        }
    }
}