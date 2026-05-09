package com.example.vibecheck_dev.presentation.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.SettingsCell
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.presentation.components.TypewriterText
import kotlinx.coroutines.launch
import com.example.vibecheck_dev.ui.theme.Y2KTypography // Panggil font retro lu

// Data class buat nyimpen info tiap halaman
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val neonColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit // Fungsi buat loncat ke layar berikutnya
) {
    val pages = listOf(
        OnboardingPage(
            title = "P2P CAMERA",
            description = "Satu jadi Kamera, satu jadi Remote. Konek tanpa internet via Wi-Fi Lokal!",
            icon = Icons.Default.SettingsCell,
            neonColor = Color.Magenta
        ),
        OnboardingPage(
            title = "SMART GESTURE",
            description = "Pose V-Sign atau Buka Telapak Tangan buat auto-jepret. Nggak perlu lari-larian lagi!",
            icon = Icons.Default.WavingHand,
            neonColor = Color.Cyan
        ),
        OnboardingPage(
            title = "Y2K DIGICAM",
            description = "Otomatis apply filter kamera saku jadul & date stamp di setiap foto lu.",
            icon = Icons.Default.CameraRoll,
            neonColor = Color.Green
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Background pekat ala DOS
            .padding(16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { position ->
            PagerScreen(page = pages[position])
        }

        // Navigasi Bawah (Indikator & Tombol)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indikator Titik-Titik
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.White else Color.DarkGray
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RectangleShape) // Kotak, bukan bulat, demi Y2K!
                            .background(color)
                    )
                }
            }

            // Tombol Next / Let's Vibe
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onFinishOnboarding() // Keluar dari onboarding
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Magenta
                ),
                shape = RectangleShape,
                modifier = Modifier.border(2.dp, Color.White, RectangleShape)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "START_VIBIN.exe" else "NEXT >>",
                    color = Color.White,
                    style = Y2KTypography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PagerScreen(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ikon dikotakin dengan border Neon
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(4.dp, page.neonColor, RectangleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                tint = page.neonColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = Y2KTypography.titleLarge,
            color = page.neonColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        TypewriterText(
            text = page.description,
            style = Y2KTypography.bodyMedium,
            color = Color.LightGray,
            modifier = Modifier.padding(horizontal = 24.dp),
            typingSpeed = 30L // Makin kecil angkanya makin ngebut ngetiknya
        )
    }
}