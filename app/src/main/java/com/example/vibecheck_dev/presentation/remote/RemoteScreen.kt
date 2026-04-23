package com.example.vibecheck_dev.presentation.remote

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteScreen(viewModel: RemoteViewModel, onNavigateBack: () -> Unit) {
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState(initial = false)
    val peers by viewModel.peersList.collectAsState()
    val connectionInfo by viewModel.connectionInfo.collectAsState()

    // AMBIL SATU STATE UTAMA
    val uiState by viewModel.uiState.collectAsState()

    if (connectionInfo?.groupFormed == true) {
        ProRemoteHud(
            ipAddress = connectionInfo?.groupOwnerAddress?.hostAddress ?: "UNKNOWN",
            uiState = uiState,
            onEvent = viewModel::onEvent, // Lempar fungsi Event ke dalam
            requestSpecs = viewModel::requestCameraSpecs
        )
    } else {
        ScannerView(
            isWifiEnabled = isWifiEnabled,
            peers = peers,
            onRefresh = { viewModel.onEvent(RemoteEvent.StartDiscovery) },
            onConnect = { address -> viewModel.onEvent(RemoteEvent.ConnectToDevice(address)) }
        )
    }
}

@Composable
fun ProRemoteHud(
    ipAddress: String,
    uiState: RemoteUiState,
    onEvent: (RemoteEvent) -> Unit,
    requestSpecs: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        requestSpecs()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // --- LAYER 1: VIDEO (FULL SCREEN) ---
        if (uiState.remoteBitmap != null) {
            Image(
                bitmap = uiState.remoteBitmap.asImageBitmap(),
                contentDescription = "Live View",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // --- LAYER 2: UI CONTROLS ---
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP CONTROL BAR ---
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(RemoteEvent.ToggleFlash)
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if(uiState.flashMode == "OFF") Icons.Default.FlashOff else Icons.Default.FlashOn,
                                contentDescription = "Flash",
                                tint = if(uiState.flashMode != "OFF") Color.Cyan else Color.LightGray
                            )
                            Text(uiState.flashMode, fontSize = 10.sp, color = Color.White)
                        }
                    }

                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(RemoteEvent.ToggleTimer)
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Timer, contentDescription = "Timer", tint = if(uiState.timerSeconds > 0) Color.Magenta else Color.LightGray)
                            Text(if(uiState.timerSeconds == 0) "OFF" else "${uiState.timerSeconds}s", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(RemoteEvent.FlipCamera)
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White)
                            Text("FLIP", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- HEADER STATUS ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LIVE_FEED", color = Color.Green, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("TARGET_IP: $ipAddress", color = Color.White, fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(RemoteEvent.Disconnect)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("ABORT", color = Color.White) }
                }
            }

            // --- BOTTOM PANEL (Shutter & Zoom) ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                Row(modifier = Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
                    if (uiState.minZoom < 1.0f) {
                        ZoomButton("0.5x", uiState.minZoom, uiState.currentZoom) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onEvent(RemoteEvent.ChangeZoom(uiState.minZoom))
                        }
                    }
                    ZoomButton("1x", 1f, uiState.currentZoom) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onEvent(RemoteEvent.ChangeZoom(1f))
                    }
                    if (uiState.maxZoom >= 2.0f) {
                        ZoomButton("2x", 2f, uiState.currentZoom) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onEvent(RemoteEvent.ChangeZoom(2f))
                        }
                    }
                }

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(RemoteEvent.TakePhoto)
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.Red.copy(alpha = 0.9f),
                    border = BorderStroke(4.dp, Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(6.dp).border(2.dp, Color.White, CircleShape))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ZoomButton(text: String, ratio: Float, currentZoom: Float, onClick: () -> Unit) {
    val isSelected = currentZoom == ratio
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(40.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray.copy(alpha = 0.7f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ScannerView tetap sama seperti sebelumnya
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerView(isWifiEnabled: Boolean, peers: List<WifiP2pDevice>, onRefresh: () -> Unit, onConnect: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Scanner Mode", style = MaterialTheme.typography.titleLarge) }) },
        floatingActionButton = { FloatingActionButton(onClick = onRefresh, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Refresh, contentDescription = "Cari Kamera") } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (!isWifiEnabled) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text("Wi-Fi mati. Harap nyalakan.", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp)) }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text("Perangkat Ditemukan:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (peers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tekan tombol Refresh.") }
            } else {
                LazyColumn {
                    items(peers) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onConnect(device.deviceAddress) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column { Text(text = device.deviceName, fontWeight = FontWeight.Bold); Text(text = device.deviceAddress, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}