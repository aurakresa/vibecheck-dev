package com.example.vibecheck_dev.presentation.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.vibecheck_dev.presentation.components.PoseOverlay
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.presentation.components.y2kPressEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalGetImage::class, ExperimentalCamera2Interop::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel, onNavigateBack: () -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- STATE UNTUK AI POSE DIRECTOR (REAL-TIME TRACKING) ---
    var suggestedPose by remember { mutableStateOf("") }
    var realTimeBones by remember { mutableStateOf<List<com.example.vibecheck_dev.domain.util.PoseBone>>(emptyList()) }

    var suggestedGhostPose by remember { mutableStateOf<com.example.vibecheck_dev.domain.util.GhostPose?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    // --- INISIALISASI MATA AI ---
    val poseAnalyzer = remember(uiState.lensFacing) {
        com.example.vibecheck_dev.domain.util.PoseAnalyzer(
            isFrontCamera = uiState.lensFacing == CameraSelector.LENS_FACING_FRONT,
            onBonesUpdated = { bones ->
                realTimeBones = bones
            },
            onUserConfused = { userContext ->
                // Kalau user diam 4 detik, kasih Object Ghost Pose!
                suggestedGhostPose = when (userContext) {
                    "HANDS_DOWN" -> com.example.vibecheck_dev.domain.util.GhostPoseDictionary.SASSY_HIP
                    else -> com.example.vibecheck_dev.domain.util.GhostPoseDictionary.PEACE_Y2K
                }
            }
        )
    }

    val connectionInfo by viewModel.connectionInfo.collectAsState(initial = null)
    val isConnectedToRemote = connectionInfo?.groupFormed == true

    val is169 = uiState.aspectRatio == AspectRatio.RATIO_16_9

    val previewView = remember { PreviewView(context) }

    val preview = remember(uiState.aspectRatio) {
        Preview.Builder().setTargetAspectRatio(uiState.aspectRatio).build()
    }
    val imageCapture = remember(uiState.aspectRatio) {
        ImageCapture.Builder().setTargetAspectRatio(uiState.aspectRatio).build()
    }
    val imageAnalysis = remember(uiState.aspectRatio) {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(480, 360))
            .build()
    }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }
    val screenFlashAlpha = remember { Animatable(0f) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val isRecording = activeRecording != null
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var countdownDisplay by remember { mutableIntStateOf(0) }
    var dynamicMinZoom by remember { mutableFloatStateOf(0.5f) }

    var latestPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { latestPhotoUri = getLatestVibeCheckImage(context) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (activeRecording != null) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.onEvent(CameraEvent.StartHosting)
        onDispose {
            viewModel.onEvent(CameraEvent.StopHosting)
            backgroundExecutor.shutdown()
            activeRecording?.stop()
        }
    }

    LaunchedEffect(previewView, preview) { preview.setSurfaceProvider(previewView.surfaceProvider) }

    LaunchedEffect(uiState.flashMode, imageCapture, uiState.isVideoMode, cameraControl) {
        imageCapture.flashMode = uiState.flashMode
        cameraControl?.let {
            val isFlashOn = uiState.flashMode == ImageCapture.FLASH_MODE_ON
            it.enableTorch(if (uiState.isVideoMode) isFlashOn else false)
        }
    }

    LaunchedEffect(uiState.lensFacing, uiState.isUltrawideActive, uiState.isVideoMode, preview, imageCapture, imageAnalysis, videoCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val baseSelector = CameraSelector.Builder().requireLensFacing(uiState.lensFacing).build()
            var canNativeMinZoom = false

            try {
                val filteredCameras = baseSelector.filter(cameraProvider.availableCameraInfos)
                if (filteredCameras.isNotEmpty() && (filteredCameras.first().zoomState.value?.minZoomRatio ?: 1f) < 1f) {
                    canNativeMinZoom = true
                }
            } catch (e: Exception) {}

            val cameraSelector = if (uiState.isUltrawideActive && uiState.lensFacing == CameraSelector.LENS_FACING_BACK && !canNativeMinZoom) {
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .addCameraFilter { cameraInfos ->
                        val sorted = cameraInfos.sortedBy { info ->
                            try {
                                val cam2Info = Camera2CameraInfo.from(info)
                                cam2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull() ?: Float.MAX_VALUE
                            } catch (e: Exception) { Float.MAX_VALUE }
                        }
                        listOf(sorted.first())
                    }.build()
            } else { baseSelector }

            try {
                cameraProvider.unbindAll()
                val useCases = mutableListOf<UseCase>(preview)
                if (uiState.isVideoMode) {
                    useCases.add(videoCapture)
                    useCases.add(imageAnalysis)
                } else {
                    useCases.add(imageCapture)
                    useCases.add(imageAnalysis)
                }

                var camera: Camera? = null
                try {
                    camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases.toTypedArray())
                } catch (e: IllegalArgumentException) {
                    if (uiState.isVideoMode) {
                        Log.w("CAMERA_LOG", "OS membatasi UseCase. Mematikan Remote Preview.")
                        useCases.remove(imageAnalysis)
                        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases.toTypedArray())
                    } else { throw e }
                }

                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                val minZ = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                if (minZ < 1f) dynamicMinZoom = minZ

                cameraControl?.setZoomRatio(if (uiState.isUltrawideActive && minZ >= 1f) 1f else uiState.zoomRatio)

                if (!uiState.isVideoMode) {
                    var lastAnalyzedTimestamp = 0L
                    imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                        val currentTimestamp = System.currentTimeMillis()
                        if (currentTimestamp - lastAnalyzedTimestamp >= 66) {
                            lastAnalyzedTimestamp = currentTimestamp
                            // Convert image to bitmap for P2P before the AI consumes and closes it
                            val bmp = imageProxy.toBitmap()
                            viewModel.onEvent(CameraEvent.SendVideoFrame(bitmapToByteArray(bmp), imageProxy.imageInfo.rotationDegrees, uiState.lensFacing == CameraSelector.LENS_FACING_FRONT))
                        }

                        // Lempar gambar ke otak AI buat dicek posenya
                        poseAnalyzer.analyze(imageProxy)
                    }
                }
            } catch (exc: Exception) {
                Log.e("CAMERA_LOG", "Gagal bind", exc)
                if (uiState.isUltrawideActive) {
                    Toast.makeText(context, "OS memblokir mode ini pada lensa Ultrawide", Toast.LENGTH_SHORT).show()
                    viewModel.onEvent(CameraEvent.SetZoomLocal(1f))
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(uiState.zoomRatio, uiState.isUltrawideActive, cameraControl, cameraInfo) {
        cameraControl?.let { control ->
            try {
                val minZ = cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                val maxZ = cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
                val applyZoom = if (uiState.isUltrawideActive && minZ >= 1f) 1f else if (uiState.zoomRatio < minZ) minZ else uiState.zoomRatio
                control.setZoomRatio(applyZoom.coerceIn(minZ, maxZ))
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(uiState.iso, uiState.shutterSpeed, cameraControl) {
        cameraControl?.let { control ->
            try {
                val c2Control = Camera2CameraControl.from(control)
                val builder = CaptureRequestOptions.Builder()

                if (uiState.iso != 100 || uiState.shutterSpeed > 0L) {
                    builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    val applyIso = if (uiState.iso == 100) 800 else uiState.iso
                    val applyShutter = if (uiState.shutterSpeed == 0L) 33333333L else uiState.shutterSpeed
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, applyIso)
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, applyShutter)
                } else {
                    builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                }
                c2Control.captureRequestOptions = builder.build()
            } catch (e: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun executeCapture() {
        if (uiState.isVideoMode) {
            if (activeRecording != null) {
                activeRecording?.stop()
                activeRecording = null
                coroutineScope.launch { delay(500); latestPhotoUri = getLatestVibeCheckImage(context) }
            } else {
                val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VibeCheck")
                }
                val mediaStoreOutput = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(contentValues).build()

                val hasAudioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                var pendingRecording = videoCapture.output.prepareRecording(context, mediaStoreOutput)
                if (hasAudioPerm) pendingRecording = pendingRecording.withAudioEnabled()

                activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        activeRecording = null
                        if (!event.hasError()) Toast.makeText(context, "Video tersimpan!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            coroutineScope.launch {
                // --- HACK DEWA: PAKSA FLASH NYALA DENGAN SENTER SEBELUM FOTO ---
                val isFlashOn = uiState.flashMode == ImageCapture.FLASH_MODE_ON
                if (isFlashOn) {
                    cameraControl?.enableTorch(true)
                    delay(400) // Kasih waktu 400ms biar cahaya stabil dan fokus ngunci
                }

                screenFlashAlpha.snapTo(1f)
                delay(100)
                takePhoto(imageCapture, context, onPhotoSaved = {
                    latestPhotoUri = getLatestVibeCheckImage(context)
                }) {
                    coroutineScope.launch {
                        screenFlashAlpha.animateTo(0f)
                        // MATIKAN KEMBALI SENTER SETELAH FOTO JADI
                        if (isFlashOn) cameraControl?.enableTorch(false)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.takePhotoTrigger.collect {
            coroutineScope.launch {
                if (uiState.timerSeconds > 0 && !isRecording) {
                    countdownDisplay = uiState.timerSeconds
                    while (countdownDisplay > 0) { delay(1000); countdownDisplay-- }
                }
                executeCapture()
            }
        }
    }

    val shutterInteractionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    val isoList = listOf(100, 400, 800, 1600, 3200)
    val shutterList = listOf(0L, 66666666L, 33333333L, 16666666L, 8333333L)
    val shutterLabels = listOf("AUTO", "1/15", "1/30", "1/60", "1/120")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(modifier = Modifier.fillMaxSize().padding(bottom = if(is169) 0.dp else 120.dp)) {
            // LAYER 1: KAMERA ASLI
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // LAYER 2: OVERLAY STICKMAN REAL-TIME + BAYANGAN GHOST
            PoseOverlay(
                ghostPose = suggestedGhostPose, // <- Ngirim Data Bayangan (Bisa Null)
                realTimeBones = realTimeBones   // <- Ngirim Data Gerakan Asli
            )

            // LAYER 3: HITUNG MUNDUR (Kalau timer nyala)
            if (countdownDisplay > 0) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Text(text = countdownDisplay.toString(), style = Y2KTypography.titleLarge.copy(fontSize = 180.sp), color = Color.Red)
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = screenFlashAlpha.value)))

            if (isRecording) {
                Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 100.dp, start = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color.Red, RectangleShape).y2kBlinkEffect(800))
                    Spacer(modifier = Modifier.width(8.dp))
                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    Text(String.format(Locale.US, "%02d:%02d", mins, secs), color = Color.Red, style = Y2KTypography.bodyMedium)
                }
            }

            if (!isConnectedToRemote && !uiState.isVideoMode) {
                Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier.background(Color.Black.copy(0.7f)).border(1.dp, Color.Green, RectangleShape).clickable {
                            viewModel.onEvent(CameraEvent.SetIso(isoList[(isoList.indexOf(uiState.iso) + 1) % isoList.size]))
                        }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ISO", color = Color.Green, fontSize = 10.sp, style = Y2KTypography.bodySmall)
                        Text(if(uiState.iso == 100) "AUTO" else uiState.iso.toString(), color = Color.White, style = Y2KTypography.bodyMedium)
                    }

                    Column(
                        modifier = Modifier.background(Color.Black.copy(0.7f)).border(1.dp, Color.Green, RectangleShape).clickable {
                            val idx = shutterList.indexOf(uiState.shutterSpeed).takeIf { it >= 0 } ?: 0
                            viewModel.onEvent(CameraEvent.SetShutterSpeed(shutterList[(idx + 1) % shutterList.size]))
                        }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SHT", color = Color.Green, fontSize = 10.sp, style = Y2KTypography.bodySmall)
                        val idx = shutterList.indexOf(uiState.shutterSpeed).takeIf { it >= 0 } ?: 0
                        Text(shutterLabels[idx], color = Color.White, style = Y2KTypography.bodyMedium)
                    }
                }
            }

            // --- TOP OVERLAY BAR ---
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                if (!isConnectedToRemote) {
                    val isFlashOn = uiState.flashMode == ImageCapture.FLASH_MODE_ON
                    Box(modifier = Modifier.align(Alignment.CenterStart).size(45.dp).background(Color.Black.copy(alpha = 0.5f)).border(2.dp, Color.White, RectangleShape).clickable(enabled = !isRecording) { viewModel.onEvent(CameraEvent.ToggleFlashLocal) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = if (isFlashOn) Color.Yellow else Color.White)
                    }

                    Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeControlBtn(if(uiState.timerSeconds == 0) "TMR:OFF" else "TMR:${uiState.timerSeconds}s", Color.White, !isRecording) { viewModel.onEvent(CameraEvent.ToggleTimerLocal) }
                        ModeControlBtn(if(is169) "16:9" else "4:3", Color.Cyan, !isRecording) { viewModel.onEvent(CameraEvent.ToggleAspectRatio) }
                        ModeControlBtn(if(uiState.isVideoMode) "VID" else "PIC", if(uiState.isVideoMode) Color.Red else Color.Cyan, !isRecording) { viewModel.onEvent(CameraEvent.ToggleVideoModeLocal) }
                    }
                }

                Box(modifier = Modifier.align(Alignment.CenterEnd).size(45.dp).background(Color.Black.copy(alpha = 0.5f)).border(2.dp, Color.Red, RectangleShape).clickable(enabled = !isRecording) { onNavigateBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.Red)
                }
            }

            if (!isConnectedToRemote && !isRecording && uiState.lensFacing == CameraSelector.LENS_FACING_BACK) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if(is169) 130.dp else 16.dp).background(Color.Black.copy(alpha = 0.6f)).border(2.dp, Color.White, RectangleShape).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val uwLabel = if (dynamicMinZoom == 0.6f) ".6" else if (dynamicMinZoom == 0.5f) ".5" else String.format(Locale.US, "%.1f", dynamicMinZoom).replace("0.", ".")
                    val zooms = listOf(dynamicMinZoom, 1f, 2f)
                    zooms.forEach { z ->
                        val isSelected = (z < 1f && uiState.isUltrawideActive) || (z >= 1f && uiState.zoomRatio == z && !uiState.isUltrawideActive)
                        val label = if (z < 1f) uwLabel else "${z.toInt()}x"

                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            style = Y2KTypography.bodyMedium,
                            modifier = Modifier.background(if (isSelected) Color.Cyan else Color.Transparent).clickable {
                                viewModel.onEvent(CameraEvent.SetZoomLocal(z))
                            }.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        val bottomBarBg = if (is169) Color.Black.copy(alpha = 0.5f) else Color(0xFF1A1A1A)
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp).background(bottomBarBg).border(if(is169) 0.dp else 2.dp, Color.DarkGray, RectangleShape)) {
            if (isConnectedToRemote) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("P2P: LINKED", color = Color.Green, style = Y2KTypography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("> REMOTE CTRL ACTIVE <", color = Color.Magenta, style = Y2KTypography.bodyLarge, modifier = Modifier.y2kBlinkEffect(500))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {

                    if (!isRecording) {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            AlbumThumbnail(uri = latestPhotoUri) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    type = "image/*"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try { context.startActivity(intent) } catch (e: Exception) { Log.e("ALBUM", "Gagal buka galeri") }
                            }
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.Center).size(70.dp).y2kPressEffect(shutterInteractionSource)
                        .clickable(interactionSource = shutterInteractionSource, indication = null) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.onEvent(CameraEvent.TakePhotoLocal)
                        }.background(when { isRecording -> Color.Red; uiState.isVideoMode -> Color.DarkGray; else -> Color.Magenta }).border(4.dp, Color.White, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) { Box(modifier = Modifier.size(24.dp).background(Color.White, RectangleShape)) }
                        else if (uiState.isVideoMode) { Box(modifier = Modifier.size(24.dp).background(Color.Red, RectangleShape)) }
                    }

                    if (!isRecording) {
                        Box(modifier = Modifier.align(Alignment.CenterEnd).border(2.dp, Color.White, RectangleShape).clickable {
                            if (uiState.isUltrawideActive) viewModel.onEvent(CameraEvent.SetZoomLocal(1f))
                            viewModel.onEvent(CameraEvent.FlipCameraLocal)
                        }.padding(12.dp)) {
                            Text("FLIP", color = Color.White, style = Y2KTypography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

fun getLatestVibeCheckImage(context: Context): Uri? {
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
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
    } catch (e: Exception) { Log.e("ALBUM", "Error getting latest photo", e) }
    return null
}

@Composable
fun AlbumThumbnail(uri: Uri?, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    bitmap = bmp.asImageBitmap()
                } catch (e: Exception) { Log.e("ALBUM", "Failed to load thumb", e) }
            }
        }
    }

    Box(modifier = Modifier.size(50.dp).background(Color.DarkGray, RectangleShape).border(2.dp, Color.White, RectangleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!, contentDescription = "Album", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("ALBUM", color = Color.White, fontSize = 9.sp, style = Y2KTypography.bodySmall)
        }
    }
}

fun takePhoto(imageCapture: ImageCapture, context: Context, onPhotoSaved: () -> Unit, onCaptureComplete: () -> Unit) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VibeCheck")
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()

    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("FIKAL_ERROR", "Gagal menyimpan foto", exc)
                onCaptureComplete()
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Foto tersimpan!", Toast.LENGTH_SHORT).show()
                onPhotoSaved()
                onCaptureComplete()
            }
        }
    )
}

fun bitmapToByteArray(bitmap: android.graphics.Bitmap): ByteArray {
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 15, stream)
    return stream.toByteArray()
}

@Composable
fun ProControlBtn(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.background(Color.Black.copy(alpha = 0.7f)).border(1.dp, Color.Green, RectangleShape).clickable { onClick() }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Green, fontSize = 10.sp)
        Text(value, color = Color.White, style = Y2KTypography.bodyMedium)
    }
}

@Composable
fun ModeControlBtn(txt: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).border(2.dp, color, RectangleShape).clickable(enabled) { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(txt, color = color, style = Y2KTypography.bodySmall)
    }
}