package com.example.vibecheck_dev.presentation.remote

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.presentation.components.y2kGlitchEffect
import com.example.vibecheck_dev.presentation.components.y2kPressEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun RemoteScreen(viewModel: RemoteViewModel, onNavigateBack: () -> Unit) {
    val shutterInteractionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val peers by viewModel.peersList.collectAsState(initial = emptyList())
    val connectionInfo by viewModel.connectionInfo.collectAsState(initial = null)

    var isScanning by remember { mutableStateOf(true) }
    var connectedHostAddress by remember { mutableStateOf("") }

    var recordingSeconds by remember { mutableIntStateOf(0) }
    var latestPhotoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(RemoteEvent.StartDiscovery)
        latestPhotoUri = getRemoteLatestVibeCheckImage(context)
    }

    LaunchedEffect(connectionInfo?.groupFormed) {
        if (connectionInfo?.groupFormed == false && !isScanning) {
            isScanning = true
            connectedHostAddress = ""
            viewModel.onEvent(RemoteEvent.Disconnect)
        }
    }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            recordingSeconds = 0
            while (uiState.isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    val is169 = uiState.aspectRatio == AspectRatio.RATIO_16_9

    val isoList = listOf(100, 400, 800, 1600, 3200)
    val shutterList = listOf(0L, 66666666L, 33333333L, 16666666L, 8333333L)
    val shutterLabels = listOf("AUTO", "1/15", "1/30", "1/60", "1/120")
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {

        if (isScanning) {
            P2pScannerOverlay(
                peers = peers,
                isDiscovering = uiState.isDiscovering,
                onCancel = {
                    viewModel.onEvent(RemoteEvent.StopDiscovery)
                    viewModel.onEvent(RemoteEvent.Disconnect)
                    onNavigateBack()
                },
                onRefresh = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onEvent(RemoteEvent.StartDiscovery)
                },
                onConnect = { deviceAddress ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    connectedHostAddress = deviceAddress
                    isScanning = false
                    viewModel.onEvent(RemoteEvent.ConnectToDevice(deviceAddress))
                }
            )
        } else {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (is169) 0.dp else 120.dp)) {

                if (uiState.remoteBitmap != null) {
                    // 1. RENDER VIDEO STREAM (LAPISAN DASAR)
                    Image(
                        bitmap = uiState.remoteBitmap!!.asImageBitmap(),
                        contentDescription = "Live View",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 2. RENDER OVERLAY AI DI ATAS VIDEO (LAPISAN ATAS)
                    if (uiState.isAiModeActive) {
                        when (uiState.aiPhase) {
                            "SCANNING" -> {
                                com.example.vibecheck_dev.presentation.components.ScanningOverlay(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            "READY_TO_MATCH" -> {
                                if (uiState.bodyScale > 0f) {
                                    // Convert string balik ke Enum Y2KPoseType
                                    val poseEnum = try {
                                        com.example.vibecheck_dev.presentation.camera.Y2KPoseType.valueOf(
                                            uiState.currentPoseType
                                        )
                                    } catch (e: Exception) {
                                        com.example.vibecheck_dev.presentation.camera.Y2KPoseType.HALF_BODY_PEACE
                                    }

                                    com.example.vibecheck_dev.presentation.components.PoseOverlay(
                                        poseType = poseEnum,
                                        isMatched = uiState.isPoseMatched,
                                        anchorX = uiState.anchorX,
                                        anchorY = uiState.anchorY,
                                        bodyScale = uiState.bodyScale,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            "GROUP_MATCH" -> {
                                com.example.vibecheck_dev.presentation.components.GroupOverlay(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    // RENDER JIKA VIDEO BELUM MASUK
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF001100))
                            .border(2.dp, Color.Green, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AWAITING_VIDEO_STREAM...",
                            color = Color.Green,
                            style = Y2KTypography.titleLarge,
                            modifier = Modifier.y2kBlinkEffect(1000)
                        )
                    }
                }

                if (uiState.timerSeconds > 0 && !uiState.isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.timerSeconds.toString(),
                            style = Y2KTypography.titleLarge.copy(fontSize = 180.sp),
                            color = Color.Red
                        )
                    }
                }

                if (uiState.isRecording) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 100.dp, start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Red, RectangleShape)
                                .y2kBlinkEffect(800)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val mins = recordingSeconds / 60
                        val secs = recordingSeconds % 60
                        Text(
                            String.format(Locale.US, "%02d:%02d", mins, secs),
                            color = Color.Red,
                            style = Y2KTypography.bodyMedium
                        )
                    }
                }

                if (!uiState.isVideoMode) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RemoteProControlBtn(
                            "ISO",
                            if (uiState.iso == 100) "AUTO" else uiState.iso.toString()
                        ) {
                            val nextIso = isoList[(isoList.indexOf(uiState.iso) + 1) % isoList.size]
                            viewModel.onEvent(RemoteEvent.SetIso(nextIso))
                        }
                        RemoteProControlBtn(
                            "SHT",
                            if (uiState.shutterSpeed == 0L) "AUTO" else "MAN"
                        ) {
                            val currentIndex =
                                shutterList.indexOf(uiState.shutterSpeed).takeIf { it >= 0 } ?: 0
                            viewModel.onEvent(RemoteEvent.SetShutterSpeed(shutterList[(currentIndex + 1) % shutterList.size]))
                        }

                        // --- TOMBOL AI KHUSUS REMOTE ---
                        Column(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .border(
                                    1.dp,
                                    if (uiState.isAiModeActive) Color.Magenta else Color.Cyan,
                                    RectangleShape
                                )
                                .clickable { viewModel.onEvent(RemoteEvent.ToggleAiMode) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "AI",
                                color = if (uiState.isAiModeActive) Color.Magenta else Color.Cyan,
                                fontSize = 10.sp,
                                style = Y2KTypography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (uiState.isAiModeActive) "ON" else "OFF",
                                color = Color.White,
                                style = Y2KTypography.bodyMedium
                            )
                        }
                    }
                }

                // --- TOP OVERLAY BAR (RESPONSIF CENTER - KEMBARAN HOST) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                ) {
                    // KIRI: FLASH
                    val isFlashOn = uiState.flashMode == "ON"
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(45.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(2.dp, Color.White, RectangleShape)
                            .clickable(!uiState.isRecording) { viewModel.onEvent(RemoteEvent.ToggleFlash) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            "",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }

                    // TENGAH: GROUP
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RemoteModeControlBtn(
                            if (uiState.timerSeconds == 0) "TMR:OFF" else "TMR:${uiState.timerSeconds}s",
                            Color.White,
                            !uiState.isRecording
                        ) { viewModel.onEvent(RemoteEvent.ToggleTimer) }
                        RemoteModeControlBtn(
                            if (is169) "16:9" else "4:3",
                            Color.Cyan,
                            !uiState.isRecording
                        ) { viewModel.onEvent(RemoteEvent.ToggleAspectRatio) }
                        RemoteModeControlBtn(
                            if (uiState.isVideoMode) "VID" else "PIC",
                            if (uiState.isVideoMode) Color.Red else Color.Cyan,
                            !uiState.isRecording
                        ) { viewModel.onEvent(RemoteEvent.ToggleVideoMode) }
                    }

                    // KANAN: EXIT
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(45.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(2.dp, Color.Red, RectangleShape)
                            .clickable(!uiState.isRecording) {
                                viewModel.onEvent(RemoteEvent.Disconnect)
                                isScanning = true
                            }, contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "", tint = Color.Red)
                    }
                }

                if (!uiState.isRecording) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (is169) 135.dp else 20.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(2.dp, Color.White, RectangleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f, 1f, 2f).forEach { z ->
                            val sel = uiState.currentZoom == z
                            Text(
                                if (z == 0.5f) ".5" else "${z.toInt()}x",
                                modifier = Modifier
                                    .background(if (sel) Color.Cyan else Color.Transparent)
                                    .clickable { viewModel.onEvent(RemoteEvent.ChangeZoom(z)) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                color = if (sel) Color.Black else Color.White,
                                style = Y2KTypography.bodyMedium
                            )
                        }
                    }
                }
            }

            val btmBg = if (is169) Color.Black.copy(alpha = 0.5f) else Color(0xFF1A1A1A)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(btmBg)
                    .border(if (is169) 0.dp else 2.dp, Color.DarkGray, RectangleShape)
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)) {

                    if (!uiState.isRecording) {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            RemoteAlbumThumbnail(uri = latestPhotoUri) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    type = "image/*"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("ALBUM", "Gagal buka galeri")
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(70.dp)
                            .y2kPressEffect(shutterInteractionSource)
                            .clickable(shutterInteractionSource, null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onEvent(RemoteEvent.TakePhoto)
                                coroutineScope.launch {
                                    delay(2500); latestPhotoUri =
                                    getRemoteLatestVibeCheckImage(context)
                                }
                            }
                            .background(if (uiState.isRecording) Color.Red else if (uiState.isVideoMode) Color.DarkGray else Color.Magenta)
                            .border(4.dp, Color.White, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isRecording) Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, RectangleShape)
                        )
                        else if (uiState.isVideoMode) Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Red, RectangleShape)
                        )
                    }

                    if (!uiState.isRecording) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .border(2.dp, Color.White, RectangleShape)
                                .clickable { viewModel.onEvent(RemoteEvent.FlipCamera) }
                                .padding(12.dp)
                        ) {
                            Text("FLIP", color = Color.White, style = Y2KTypography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// --- FUNGSI PRIVATE SUPAYA TIDAK ERROR UNRESOLVED REFERENCE ---

private fun getRemoteLatestVibeCheckImage(context: Context): Uri? {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    } else "${MediaStore.Images.Media.DATA} LIKE ?"

    val selectionArgs = arrayOf("%VibeCheck%")
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    try {
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    return ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                }
            }
    } catch (e: Exception) {
        Log.e("ALBUM", "Error getting latest photo", e)
    }
    return null
}

@Composable
private fun RemoteAlbumThumbnail(uri: Uri?, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(
                                context.contentResolver,
                                uri
                            )
                        )
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    bitmap = bmp.asImageBitmap()
                } catch (e: Exception) {
                    Log.e("ALBUM", "Failed to load thumb", e)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(50.dp)
            .background(Color.DarkGray, RectangleShape)
            .border(2.dp, Color.White, RectangleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Album",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("ALBUM", color = Color.White, fontSize = 9.sp, style = Y2KTypography.bodySmall)
        }
    }
}

@Composable
private fun RemoteProControlBtn(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f))
            .border(1.dp, Color.Green, RectangleShape)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.Green, fontSize = 10.sp)
        Text(value, color = Color.White, style = Y2KTypography.bodyMedium)
    }
}

@Composable
private fun RemoteModeControlBtn(txt: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .border(2.dp, color, RectangleShape)
            .clickable(enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center
    ) {
        Text(txt, color = color, style = Y2KTypography.bodySmall)
    }
}

@Composable
fun P2pScannerOverlay(
    peers: List<WifiP2pDevice>,
    isDiscovering: Boolean,
    onCancel: () -> Unit,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit
) {
    // --- SEDOT WARNA DINAMIS DARI TEMA ---
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val errorColor = MaterialTheme.colorScheme.error
    val borderColor = onBgColor.copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Kotak Overlay utama
        Box(
            modifier = Modifier
                .border(2.dp, primaryColor, RectangleShape)
                .background(surfaceColor.copy(alpha = 0.95f)) // Pakai warna surface dengan sedikit transparansi
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "RADAR_SCAN.exe",
                    style = Y2KTypography.titleLarge,
                    color = primaryColor,
                    modifier = Modifier.y2kGlitchEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (peers.isEmpty()) {
                    Text(
                        "Tekan tombol REF untuk mencari host...",
                        style = Y2KTypography.bodySmall,
                        color = secondaryColor
                    )
                } else {
                    Text(
                        "Ditemukan ${peers.size} Host!",
                        style = Y2KTypography.bodySmall,
                        color = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(peers) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderColor, RectangleShape)
                                .background(bgColor)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "> ${device.deviceName}",
                                    style = Y2KTypography.bodyMedium,
                                    color = primaryColor
                                )
                                Text(
                                    device.deviceAddress,
                                    style = Y2KTypography.bodySmall,
                                    color = onBgColor.copy(alpha = 0.6f)
                                )
                            }
                            Button(
                                onClick = { onConnect(device.deviceAddress) },
                                colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                                shape = RectangleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("LINK", color = bgColor, style = Y2KTypography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor),
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, errorColor, RectangleShape)
                    ) {
                        Text("ABORT", style = Y2KTypography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onRefresh,
                        enabled = !isDiscovering,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDiscovering) surfaceColor else primaryColor,
                            disabledContainerColor = surfaceColor
                        ),
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                2.dp,
                                if (isDiscovering) borderColor else borderColor,
                                RectangleShape
                            )
                    ) {
                        Text(
                            text = if (isDiscovering) "SCANNING..." else "REF",
                            color = if (isDiscovering) onBgColor.copy(alpha = 0.5f) else bgColor,
                            style = Y2KTypography.bodyMedium,
                            modifier = if (isDiscovering) Modifier.y2kBlinkEffect(300) else Modifier
                        )
                    }
                }
            }
        }
    }
}