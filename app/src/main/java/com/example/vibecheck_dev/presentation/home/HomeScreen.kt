package com.example.vibecheck_dev.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToRemote: () -> Unit
) {
    // State untuk mengontrol Sidebar (buka/tutup)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Komponen Sidebar (Drawer)
    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.8f), // Background luar pas sidebar kebuka
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Black,
                drawerShape = RectangleShape,
                modifier = Modifier
                    .width(280.dp)
                    .border(2.dp, Color.Cyan, RectangleShape)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "SYS_MENU",
                        style = Y2KTypography.titleLarge,
                        color = Color.Magenta
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.Cyan, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Menu Item: Profile
                    DrawerMenuItem(title = "PROFILE.cfg") {
                        // TODO: Navigasi ke layar edit profil
                    }
                    // Menu Item: Theme
                    DrawerMenuItem(title = "THEME.ini") {
                        // TODO: Buka modal ganti tema
                    }
                    // Menu Item: About
                    DrawerMenuItem(title = "ABOUT.txt") {
                        // TODO: Navigasi ke layar About
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Tombol Logout / Keluar
                    OutlinedButton(
                        onClick = { /* TODO: Logika Logout */ },
                        shape = RectangleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.border(2.dp, Color.Red, RectangleShape)
                    ) {
                        Text("SYSTEM_LOGOUT", style = Y2KTypography.bodyMedium)
                    }
                }
            }
        }
    ) {
        // Konten Layar Utama (Home)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("VIBECHECK_OS", style = Y2KTypography.titleMedium, color = Color.Green)
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Green)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black
                    ),
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT ROLE",
                    style = Y2KTypography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pilih perangkat ini mau jadi lensa kamera atau remote kontrol.",
                    style = Y2KTypography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))

                // Tombol Host (Kamera)
                RoleButton(
                    title = "HOST (CAMERA)",
                    description = "Aktifin AI & Pancarin P2P",
                    icon = Icons.Default.Camera,
                    color = Color.Magenta,
                    onClick = onNavigateToCamera
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Remote (Controller)
                RoleButton(
                    title = "REMOTE (CTRL)",
                    description = "Konek ke Host & Jepret",
                    icon = Icons.Default.PhoneAndroid,
                    color = Color.Cyan,
                    onClick = onNavigateToRemote
                )
            }
        }
    }
}

// Komponen Reusable buat tombol peran yang Y2K banget
@Composable
fun RoleButton(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.DarkGray.copy(alpha = 0.3f))
            .border(2.dp, color, RectangleShape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = Y2KTypography.bodyLarge, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = Y2KTypography.bodySmall, color = Color.White)
            }
        }
    }
}

// Komponen Reusable buat teks menu di Sidebar
@Composable
fun DrawerMenuItem(title: String, onClick: () -> Unit) {
    Text(
        text = "> $title",
        style = Y2KTypography.bodyMedium,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    )
}