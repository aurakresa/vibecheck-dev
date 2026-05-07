package com.example.vibecheck_dev.presentation.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen() {
    // State sementara buat slider & toggle
    var isDateStampActive by remember { mutableStateOf(true) }
    var filterIntensity by remember { mutableStateOf(0.5f) }
    var noiseLevel by remember { mutableStateOf(0.2f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("STUDIO.exe", style = Y2KTypography.titleMedium, color = Color.Magenta)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color.Magenta,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Kotak Preview Gambar (Simulasi)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f/3f) // Rasio kamera jadul
                    .background(Color.DarkGray)
                    .border(4.dp, Color.White, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("PREVIEW_RENDER", style = Y2KTypography.bodyMedium, color = Color.Gray)

                // Overlay Date Stamp bohongan
                if (isDateStampActive) {
                    Text(
                        text = "10 24 '99",
                        style = Y2KTypography.bodyMedium,
                        color = Color(0xFFFF9800), // Oranye khas kamera saku
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Control Panel
            Text("PARAMETERS", style = Y2KTypography.titleMedium, color = Color.Cyan)
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Date Stamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ENABLE_DATE_STAMP", style = Y2KTypography.bodyMedium, color = Color.White)
                Switch(
                    checked = isDateStampActive,
                    onCheckedChange = { isDateStampActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Magenta,
                        checkedTrackColor = Color.DarkGray,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slider Filter Intensity (Warmth)
            Text("CCD_WARMTH_LEVEL: ${(filterIntensity * 100).toInt()}%", style = Y2KTypography.bodySmall, color = Color.White)
            Slider(
                value = filterIntensity,
                onValueChange = { filterIntensity = it },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Yellow,
                    activeTrackColor = Color.Yellow,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider Noise/Grain
            Text("DIGITAL_NOISE: ${(noiseLevel * 100).toInt()}%", style = Y2KTypography.bodySmall, color = Color.White)
            Slider(
                value = noiseLevel,
                onValueChange = { noiseLevel = it },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Cyan,
                    activeTrackColor = Color.Cyan,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Tombol Generate Photobooth Strip
            Button(
                onClick = { /* TODO: Gabungin foto jadi strip */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White, RectangleShape)
            ) {
                Text(
                    text = "GENERATE_PHOTOSTRIP()",
                    color = Color.Black, // Teks hitam di atas ijo biar kontras
                    style = Y2KTypography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}