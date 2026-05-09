package com.example.vibecheck_dev.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun VaultScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("C:\\VIBECHECK\\VAULT>", style = Y2KTypography.titleMedium, color = Color.Green)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color.Green,
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
                .padding(16.dp)
        ) {
            Text(
                text = "Total Files: 0 (Simulasi)", // Nanti diganti state asli
                style = Y2KTypography.bodySmall,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Grid Galeri
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3 kolom ke samping
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Simulasi 12 foto kosong
                items(12) { index ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f) // Bikin jadi kotak sempurna (1:1)
                            .background(Color.DarkGray.copy(alpha = 0.5f))
                            .border(2.dp, Color.Cyan, RectangleShape)
                            .clickable { /* TODO: Buka foto full screen */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Foto $index",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}