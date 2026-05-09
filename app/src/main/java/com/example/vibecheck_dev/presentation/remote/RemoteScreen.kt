package com.example.vibecheck_dev.presentation.remote

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.presentation.components.y2kPressEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun RemoteScreen(viewModel: RemoteViewModel, onNavigateBack: () -> Unit) {
    val shutterInteractionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current // Haptic Feedback[cite: 8]

    // 1. Ambil UI State dari ViewModel[cite: 8]
    val uiState by viewModel.uiState.collectAsState()

    // 2. Ambil daftar peers langsung dari aliran data terpisah di ViewModel lu[cite: 10]
    // Beri nilai awal emptyList() jaga-jaga kalau flow-nya belum ngeluarin data
    val peers by viewModel.peersList.collectAsState(initial = emptyList())

    // TAMBAHKAN PEMBACAAN STATUS KONEKSI ASLI DARI VIEWMODEL
    val connectionInfo by viewModel.connectionInfo.collectAsState(initial = null)

    var isScanning by remember { mutableStateOf(true) }
    var connectedHostAddress by remember { mutableStateOf("") }

    // 1. OTOMATIS SCAN SAAT LAYAR DIBUKA
    LaunchedEffect(Unit) {
        viewModel.onEvent(RemoteEvent.StartDiscovery)
    }

    // 2. SENSOR AUTO-DISCONNECT (Kalau tiba-tiba putus)
    LaunchedEffect(connectionInfo?.groupFormed) {
        if (connectionInfo?.groupFormed == false && !isScanning) {
            isScanning = true
            connectedHostAddress = ""
            // Paksa bersihkan bitmap yang nyangkut di layar
            viewModel.onEvent(RemoteEvent.Disconnect)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // --- LAYAR 1: RADAR SCANNER P2P ---
        if (isScanning) {
            P2pScannerOverlay(
                peers = peers, // Oper data perangkat asli yang udah kita collect di atas
                isDiscovering = uiState.isDiscovering,
                onCancel = {
                    // Berhentikan pencarian dan putuskan koneksi saat batal
                    viewModel.onEvent(RemoteEvent.StopDiscovery)
                    viewModel.onEvent(RemoteEvent.Disconnect)
                    onNavigateBack()
                },
                onRefresh = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    // Trigger Event StartDiscovery ke ViewModel
                    viewModel.onEvent(RemoteEvent.StartDiscovery)
                },
                onConnect = { deviceAddress ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    connectedHostAddress = deviceAddress
                    isScanning = false
                    // Trigger Event ConnectToDevice beserta MAC Address/IP-nya[cite: 7, 10]
                    viewModel.onEvent(RemoteEvent.ConnectToDevice(deviceAddress))
                }
            )
        }
        // --- LAYAR 2: REMOTE CONTROL DECK ---
        else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP BAR
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onEvent(RemoteEvent.Disconnect) // Putuskan koneksi asli[cite: 7, 10]
                            isScanning = true
                        },
                        modifier = Modifier.border(2.dp, Color.Red, RectangleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = Color.Red)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TARGET_IP:", color = Color.Gray, style = Y2KTypography.bodySmall)
                        Text("[$connectedHostAddress]", color = Color.Cyan, style = Y2KTypography.bodyMedium, modifier = Modifier.y2kBlinkEffect(800))
                    }
                }

                // 2. STREAMING PREVIEW AREA
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).background(Color(0xFF001100)).border(2.dp, Color.Green, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Cek apakah ada frame dari P2P yang udah di-decode di ViewModel lu[cite: 10]
                    if (uiState.remoteBitmap != null) {
                        Image(
                            bitmap = uiState.remoteBitmap!!.asImageBitmap(),
                            contentDescription = "Live View",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("AWAITING_VIDEO_STREAM...", color = Color.Green, style = Y2KTypography.titleLarge, modifier = Modifier.y2kBlinkEffect(1000))
                    }
                }

                // 3. REMOTE CONTROL DECK (Area Tombol)
                // 3. REMOTE CONTROL DECK (Area Tombol)
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp).border(4.dp, Color.DarkGray, RectangleShape).background(Color(0xFF1A1A1A))
                ) {
                    // BARIS 1: MINI BUTTONS (Timer, Flip, Zoom)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // TIMER P2P
                        Box(modifier = Modifier.border(1.dp, Color.White, RectangleShape).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onEvent(RemoteEvent.ToggleTimer)
                        }.padding(8.dp)) {
                            Text("TMR: ${if(uiState.timerSeconds == 0) "OFF" else "${uiState.timerSeconds}s"}", color = Color.White, style = Y2KTypography.bodySmall)
                        }
                        // FLIP P2P
                        Box(modifier = Modifier.border(1.dp, Color.White, RectangleShape).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onEvent(RemoteEvent.FlipCamera)
                        }.padding(8.dp)) {
                            Text("FLIP_CAM", color = Color.White, style = Y2KTypography.bodySmall)
                        }
                        // ZOOM P2P (Siklus 1x -> 2x -> 0.5x)
                        Box(modifier = Modifier.border(1.dp, Color.White, RectangleShape).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val nextZoom = when (uiState.currentZoom) {
                                1f -> if (uiState.maxZoom >= 2f) 2f else 1f
                                2f -> if (uiState.minZoom < 1f) uiState.minZoom else 1f
                                else -> 1f
                            }
                            viewModel.onEvent(RemoteEvent.ChangeZoom(nextZoom))
                        }.padding(8.dp)) {
                            Text("ZOOM: ${uiState.currentZoom}x", color = Color.White, style = Y2KTypography.bodySmall)
                        }
                    }

                    // BARIS 2: MAIN BUTTONS (Flash, Capture, Filter)
                    Row(
                        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Flash
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(60.dp).border(2.dp, Color.White, RectangleShape).clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onEvent(RemoteEvent.ToggleFlash)
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = if (uiState.flashMode != "OFF") Color.Yellow else Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uiState.flashMode, color = Color.White, style = Y2KTypography.bodySmall)
                        }

                        // TOMBOL SHUTTER RAKSASA P2P
                        Box(modifier = Modifier.size(90.dp).y2kPressEffect(shutterInteractionSource)
                            .clickable(interactionSource = shutterInteractionSource, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onEvent(RemoteEvent.TakePhoto)
                            }.background(Color.Magenta).border(4.dp, Color.White, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("CAPTURE", color = Color.White, style = Y2KTypography.bodyMedium) }

                        // Tombol Digicam Filter
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(60.dp).border(2.dp, Color.White, RectangleShape).clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onEvent(RemoteEvent.ToggleDigicamFilter)
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Filter", tint = if (uiState.isDigicamFilterActive) Color.Cyan else Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("RETRO", color = Color.White, style = Y2KTypography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// Komponen UI Radar P2P Scanner
@Composable
fun P2pScannerOverlay(
    peers: List<WifiP2pDevice>, // Menggunakan daftar device asli[cite: 6]
    isDiscovering: Boolean,
    onCancel: () -> Unit,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.border(2.dp, Color.Cyan, RectangleShape).background(Color.DarkGray.copy(alpha = 0.3f)).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RADAR_SCAN.exe", style = Y2KTypography.titleLarge, color = Color.Cyan, modifier = Modifier.y2kGlitchEffect())
                Spacer(modifier = Modifier.height(8.dp))

                // Cek status WifiP2pDevice List[cite: 6]
                if (peers.isEmpty()) {
                    Text("Tekan tombol REF untuk mencari host...", style = Y2KTypography.bodySmall, color = Color.Yellow)
                } else {
                    Text("Ditemukan ${peers.size} Host!", style = Y2KTypography.bodySmall, color = Color.Green)
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(peers) { device -> // Loop melalui device asli[cite: 6]
                        Row(modifier = Modifier.fillMaxWidth().border(1.dp, Color.White, RectangleShape).background(Color.Black).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("> ${device.deviceName}", style = Y2KTypography.bodyMedium, color = Color.Green) // Tampilkan nama[cite: 6]
                                Text(device.deviceAddress, style = Y2KTypography.bodySmall, color = Color.Gray) // Tampilkan MAC/IP[cite: 6]
                            }
                            Button(onClick = { onConnect(device.deviceAddress) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta), shape = RectangleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Text("LINK", color = Color.White, style = Y2KTypography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f).border(2.dp, Color.Red, RectangleShape)
                    ) {
                        Text("ABORT", style = Y2KTypography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    // TOMBOL REFRESH DENGAN EFEK LOADING
                    Button(
                        onClick = onRefresh,
                        enabled = !isDiscovering, // Disable tombol pas lagi loading
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDiscovering) Color.DarkGray else Color.Cyan,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f).border(2.dp, if (isDiscovering) Color.Gray else Color.White, RectangleShape)
                    ) {
                        Text(
                            text = if (isDiscovering) "SCANNING..." else "REF",
                            color = if (isDiscovering) Color.LightGray else Color.Black,
                            style = Y2KTypography.bodyMedium,
                            modifier = if (isDiscovering) Modifier.y2kBlinkEffect(300) else Modifier
                        )
                    }
                }
            }
        }
    }
}