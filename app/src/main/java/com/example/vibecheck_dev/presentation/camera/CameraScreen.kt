package com.example.vibecheck_dev.presentation.camera

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.presentation.components.y2kPressEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

// Fungsi bantuan untuk mencari Activity
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel, onNavigateBack: () -> Unit) {
    // Inisialisasi ML Kit Pose Detector
    val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    val poseDetector = PoseDetection.getClient(options)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()

    // AMBIL STATUS KONEKSI (Apakah ada Remote yang nyambung?)
    val connectionInfo by viewModel.connectionInfo.collectAsState(initial = null)
    val isConnectedToRemote = connectionInfo?.groupFormed == true

    val uiState by viewModel.uiState.collectAsState() // Ambil UI State[cite: 6]

    val imageCapture = remember { ImageCapture.Builder().build() } // CameraX Capture[cite: 6]
    val previewView = remember { PreviewView(context) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(640, 480))
            .build()
    } // CameraX Analysis[cite: 6]

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }
    val screenFlashAlpha = remember { Animatable(0f) } // Animasi Flash[cite: 6]
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var countdownDisplay by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        viewModel.onEvent(CameraEvent.StartHosting)
        onDispose {
            viewModel.onEvent(CameraEvent.StopHosting)
            backgroundExecutor.shutdown()
        }
    } // Lifecycle Camera[cite: 6]

    // Set Zoom Ratio & Flash[cite: 6]
    LaunchedEffect(uiState.flashMode) { imageCapture.flashMode = uiState.flashMode }
    LaunchedEffect(uiState.zoomRatio) {
        if (uiState.zoomRatio > 0f) cameraControl?.setZoomRatio(uiState.zoomRatio)
    }

    LaunchedEffect(uiState.lensFacing) {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context) // Camera Provider[cite: 6]
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(uiState.lensFacing).build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
                )
                cameraControl = camera.cameraControl

                // Bikin variabel buat nyatet waktu terakhir frame dikirim
                // Bikin variabel buat nyatet waktu terakhir frame dikirim
                var lastAnalyzedTimestamp = 0L

                imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val currentTimestamp = System.currentTimeMillis()

                        // FPS THROTTLING: Cuma kirim frame setiap ~66 milidetik (sekitar 15 FPS)
                        if (currentTimestamp - lastAnalyzedTimestamp >= 66) {
                            lastAnalyzedTimestamp = currentTimestamp

                            val bitmapForWebRTC = imageProxy.toBitmap()
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val isFront = uiState.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT

                            // Kirim ke ViewModel
                            viewModel.onEvent(
                                CameraEvent.SendVideoFrame(
                                    byteArray = bitmapToByteArray(bitmapForWebRTC),
                                    rotationDegrees = rotation,
                                    isFrontCamera = isFront
                                )
                            )
                        }

                        // PROSES AI ML KIT
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        poseDetector.process(image)
                            .addOnSuccessListener { /* TODO: Logika V-Sign / Gesture */ }
                            .addOnFailureListener { Log.e("FIKAL_ERROR", "AI gagal: ${it.message}") }
                            .addOnCompleteListener {
                                // WAJIB TUTUP DI SINI SETELAH AI SELESAI BACA!
                                imageProxy.close()
                            }
                    } else {
                        // Kalau mediaImage gagal ditangkap, langsung tutup
                        imageProxy.close()
                    }
                }
            } catch (exc: Exception) {
                Log.e("CAMERA_LOG", "Gagal membuka kamera", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Penjepretan & Flash Screen[cite: 6]
    // Penjepretan & Flash Screen
    LaunchedEffect(Unit) {
        viewModel.takePhotoTrigger.collect {
            // JALANKAN COUNTDOWN KALAU TIMER > 0
            if (uiState.timerSeconds > 0) {
                countdownDisplay = uiState.timerSeconds
                while (countdownDisplay > 0) {
                    delay(1000)
                    countdownDisplay--
                }
            }

            screenFlashAlpha.snapTo(1f)
            delay(800)
            takePhoto(imageCapture, context) {
                launch { screenFlashAlpha.animateTo(0f) }
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        // --- 1. AREA PREVIEW KAMERA ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .border(2.dp, Color.White, RectangleShape)
        ) {

            // EFEK COUNTDOWN RAKSASA (Tampil kalau countdownDisplay > 0)
            if (countdownDisplay > 0) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownDisplay.toString(),
                        style = Y2KTypography.titleLarge.copy(fontSize = 120.sp), // Angka Super Gede
                        color = Color.Red
                    )
                }
            }
            // Tampilkan Kamera Asli!
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Flash Screen Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = screenFlashAlpha.value))
            )

            // OVERLAY RETRO (Viewfinder Frame)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(24.dp)
                    .border(4.dp, Color.White, RectangleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .border(4.dp, Color.White, RectangleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(24.dp)
                    .border(4.dp, Color.White, RectangleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .border(4.dp, Color.White, RectangleShape)
            )

            // Indikator REC berkedip
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .y2kBlinkEffect(800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "REC", color = Color.Red, style = Y2KTypography.bodyMedium)
            }
        }

// --- 2. CONTROL BAR BAWAH ---
        val connectionInfo by viewModel.connectionInfo.collectAsState(initial = null)
        val isConnectedToRemote = connectionInfo?.groupFormed == true

        val shutterInteractionSource =
            remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        if (isConnectedToRemote) {
            // JIKA TERHUBUNG REMOTE: Sembunyikan tombol, tampilkan status
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.border(2.dp, Color.Red, RectangleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.Red)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AI_POSE_ENGINE", color = Color.Cyan, style = Y2KTypography.bodySmall)
                    Text(
                        "> ACTIVE...",
                        color = Color.Yellow,
                        style = Y2KTypography.bodyMedium,
                        modifier = Modifier.y2kBlinkEffect(300)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("P2P: LINKED", color = Color.Green, style = Y2KTypography.bodySmall)
                    Text(
                        "> REMOTE CTRL",
                        color = Color.Magenta,
                        style = Y2KTypography.bodyMedium,
                        modifier = Modifier.y2kBlinkEffect(500)
                    )
                }
            }
        } else {
            // JIKA TIDAK TERHUBUNG: Tampilkan FULL CONTROL DECK ala Remote
            // JIKA TIDAK TERHUBUNG: Tampilkan FULL CONTROL DECK ala Remote
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp).padding(16.dp).border(4.dp, Color.DarkGray, RectangleShape).background(Color(0xFF1A1A1A))
            ) {
                // Tombol Exit di Kiri Atas
                IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).border(1.dp, Color.Red, RectangleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.Red)
                }

                // BARIS 1: MINI BUTTONS (Timer, Flip, Zoom)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 60.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // TIMER
                    Box(modifier = Modifier.border(1.dp, Color.White, RectangleShape).clickable { viewModel.onEvent(CameraEvent.ToggleTimerLocal) }.padding(8.dp)) {
                        Text("TMR: ${if(uiState.timerSeconds == 0) "OFF" else "${uiState.timerSeconds}s"}", color = Color.White, style = Y2KTypography.bodySmall)
                    }
                    // FLIP
                    Box(modifier = Modifier.border(1.dp, Color.White, RectangleShape).clickable { viewModel.onEvent(CameraEvent.FlipCameraLocal) }.padding(8.dp)) {
                        Text("FLIP_CAM", color = Color.White, style = Y2KTypography.bodySmall)
                    }
                    // ZOOM
                    Box(modifier = Modifier.border(1.dp, Color.White, RectangleShape).clickable { viewModel.onEvent(CameraEvent.ToggleZoomLocal) }.padding(8.dp)) {
                        Text("ZOOM: ${uiState.zoomRatio}x", color = Color.White, style = Y2KTypography.bodySmall)
                    }
                }

                // BARIS 2: MAIN BUTTONS (Flash, Capture, Filter)
                Row(
                    modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFlashOn = uiState.flashMode == androidx.camera.core.ImageCapture.FLASH_MODE_ON
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(60.dp).border(2.dp, Color.White, RectangleShape).clickable { viewModel.onEvent(CameraEvent.ToggleFlashLocal) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = if (isFlashOn) Color.Yellow else Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("FLASH", color = Color.White, style = Y2KTypography.bodySmall)
                    }

                    // TOMBOL SHUTTER RAKSASA LOKAL
                    Box(modifier = Modifier.size(90.dp).y2kPressEffect(shutterInteractionSource)
                        .clickable(interactionSource = shutterInteractionSource, indication = null) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.onEvent(CameraEvent.TakePhotoLocal)
                        }.background(Color.Magenta).border(4.dp, Color.White, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("SNAP", color = Color.White, style = Y2KTypography.bodyMedium) }

                    // FILTER
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(60.dp).border(2.dp, Color.White, RectangleShape).clickable { viewModel.onEvent(CameraEvent.ToggleFilterLocal) }, contentAlignment = Alignment.Center) {
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

private fun takePhoto(imageCapture: ImageCapture, context: Context, onCaptureComplete: () -> Unit) {
    val name =
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Pictures/VibeCheck"
        )
    } // Menyimpan gambar via MediaStore[cite: 6]
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                onCaptureComplete()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Foto tersimpan!", Toast.LENGTH_SHORT).show()
                onCaptureComplete()
            }
        }
    ) // Callback CameraX[cite: 6]
}

private fun bitmapToByteArray(bitmap: android.graphics.Bitmap): ByteArray {
    val stream = java.io.ByteArrayOutputStream()
    // Kualitas 15-20 udah cukup buat preview P2P biar gak patah-patah
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 15, stream)
    return stream.toByteArray()
}