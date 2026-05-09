package com.example.vibecheck_dev.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToRemote: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PILIH MODE",
                style = MaterialTheme.typography.headlineLarge, // Menggunakan PixelFont dari temamu
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Tentukan apakah HP ini akan menjadi kamera utama atau pengendali jarak jauh.",
                style = MaterialTheme.typography.bodyMedium, // Menggunakan font sans-serif agar mudah dibaca
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            // Opsi 1: Jadi Kamera (Host)
            RoleCard(
                title = "MODE KAMERA",
                description = "HP ini diletakkan di tripod untuk mengambil gambar.",
                icon = Icons.Default.CameraAlt,
                onClick = onNavigateToCamera
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Opsi 2: Jadi Remote (Client)
            RoleCard(
                title = "MODE REMOTE",
                description = "HP ini dipegang untuk melihat POV dan memotret.",
                icon = Icons.Default.SettingsRemote,
                onClick = onNavigateToRemote
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium // Sedikit kotak agar nuansa retro terasa
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 16.sp
                )
            }
        }
    }
}