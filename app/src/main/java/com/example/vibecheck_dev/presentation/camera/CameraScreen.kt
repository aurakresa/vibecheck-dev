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
import com.example.vibecheck_dev.domain.util.YoloAnalyzer
import com.example.vibecheck_dev.presentation.components.GroupOverlay
import com.example.vibecheck_dev.presentation.components.PoseOverlay
import com.example.vibecheck_dev.presentation.components.ScanningOverlay
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.presentation.components.y2kPressEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseLandmark

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalGetImage::class, ExperimentalCamera2Interop::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel, onNavigateBack: () -> Unit) {
    val options =
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
    val poseDetector = PoseDetection.getClient(options)

    // 1. TARUH SEMUA STATE DI SINI (Di luar blok try-catch)
    var isPoseMatched by remember { mutableStateOf(false) }
    var userAnchorX by remember { mutableStateOf(0f) }
    var userAnchorY by remember { mutableStateOf(0f) }
    var userBodyScale by remember { mutableStateOf(0f) }
    // --- TAMBAHAN BARU ---
    var isPersonDetected by remember { mutableStateOf(false) }
    var lastPhotoTakenTime by remember { mutableStateOf(0L) }
    // --- TAMBAHAN BARU ---
    var poseMatchStartTime by remember { mutableStateOf(0L) }

    val faceOptions = FaceDetectorOptions.Builder()
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    val faceDetector = PoseDetection.getClient(options) // Pose
    val faceClient = FaceDetection.getClient(faceOptions) // Wajah

    // Deklarasi context HARUS di atas sebelum dipanggil oleh YOLO
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

// INISIALISASI YOLO
    val yoloAnalyzer = remember { YoloAnalyzer(context) }
    DisposableEffect(Unit) {
        onDispose { yoloAnalyzer.close() }
    }


    val connectionInfo by viewModel.connectionInfo.collectAsState(initial = null)
    val isConnectedToRemote = connectionInfo?.groupFormed == true
    val uiState by viewModel.uiState.collectAsState()

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
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                )
            )
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

    // ... variabel bawaan lu (uiState, isConnectedToRemote, dll) ...

    // --- 💥 KUNCI PAMUNGKAS ANTI VARIABEL BASI 💥 ---
    val realTimeIsConnected by rememberUpdatedState(newValue = isConnectedToRemote)
    val realTimeUiState by rememberUpdatedState(newValue = uiState)

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

    LaunchedEffect(
        uiState.lensFacing,
        uiState.isUltrawideActive,
        uiState.isVideoMode,
        preview,
        imageCapture,
        imageAnalysis,
        videoCapture
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val baseSelector =
                CameraSelector.Builder().requireLensFacing(uiState.lensFacing).build()
            var canNativeMinZoom = false

            try {
                val filteredCameras = baseSelector.filter(cameraProvider.availableCameraInfos)
                if (filteredCameras.isNotEmpty() && (filteredCameras.first().zoomState.value?.minZoomRatio
                        ?: 1f) < 1f
                ) {
                    canNativeMinZoom = true
                }
            } catch (e: Exception) {
            }

            val cameraSelector =
                if (uiState.isUltrawideActive && uiState.lensFacing == CameraSelector.LENS_FACING_BACK && !canNativeMinZoom) {
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .addCameraFilter { cameraInfos ->
                            val sorted = cameraInfos.sortedBy { info ->
                                try {
                                    val cam2Info = Camera2CameraInfo.from(info)
                                    cam2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                        ?.minOrNull() ?: Float.MAX_VALUE
                                } catch (e: Exception) {
                                    Float.MAX_VALUE
                                }
                            }
                            listOf(sorted.first())
                        }.build()
                } else {
                    baseSelector
                }

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
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        *useCases.toTypedArray()
                    )
                } catch (e: IllegalArgumentException) {
                    Log.e("CAMERA_LOG", "Use case gagal di-bind", e)
                }

                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                val minZ = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                if (minZ < 1f) dynamicMinZoom = minZ

                cameraControl?.setZoomRatio(if (uiState.isUltrawideActive && minZ >= 1f) 1f else uiState.zoomRatio)

                var isYoloProcessing = false
                var isPoseProcessing = false
                var isSendingFrame = false

                if (!realTimeUiState.isVideoMode) {
                    var lastAnalyzedTimestamp = 0L

                    imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val currentTimestamp = System.currentTimeMillis()
                        val rotation = imageProxy.imageInfo.rotationDegrees

                        // GUNAKAN VARIABEL REAL-TIME DI SINI!
                        val isFrontCamera =
                            realTimeUiState.lensFacing == CameraSelector.LENS_FACING_FRONT

                        // ====================================================================
                        // JALUR 1: KHUSUS P2P VIDEO STREAMING
                        // ====================================================================
                        // Cek koneksi pakai realTimeIsConnected!
                        if (realTimeIsConnected && !isSendingFrame && (currentTimestamp - lastAnalyzedTimestamp >= 100)) {
                            lastAnalyzedTimestamp = currentTimestamp
                            isSendingFrame = true

                            val p2pBitmap = try {
                                imageProxy.toBitmap()
                            } catch (e: Exception) {
                                imageProxy.close()
                                isSendingFrame = false
                                return@setAnalyzer
                            }
                            imageProxy.close()

                            coroutineScope.launch(Dispatchers.Default) {
                                try {
                                    val scale = 800f / maxOf(p2pBitmap.width, p2pBitmap.height)
                                    val matrix = android.graphics.Matrix()
                                    matrix.postScale(scale, scale)
                                    val resizedBitmap = android.graphics.Bitmap.createBitmap(
                                        p2pBitmap,
                                        0,
                                        0,
                                        p2pBitmap.width,
                                        p2pBitmap.height,
                                        matrix,
                                        true
                                    )
                                    val stream = java.io.ByteArrayOutputStream()
                                    resizedBitmap.compress(
                                        android.graphics.Bitmap.CompressFormat.JPEG,
                                        60,
                                        stream
                                    )

                                    // Ganti pemanggilan viewModel.onEvent sebelumnya dengan ini:
                                    viewModel.onEvent(
                                        CameraEvent.SendVideoFrame(
                                            stream.toByteArray(),
                                            rotation,
                                            isFrontCamera,
                                            realTimeUiState.aiPhase.name,
                                            realTimeUiState.currentPoseType.name,
                                            isPoseMatched,
                                            userAnchorX,
                                            userAnchorY,
                                            userBodyScale
                                        )
                                    )
                                    resizedBitmap.recycle()
                                } catch (e: Exception) {
                                    Log.e("P2P", "Gagal kirim frame", e)
                                } finally {
                                    p2pBitmap.recycle()
                                    isSendingFrame = false
                                }
                            }
                            return@setAnalyzer
                        }

                        // ====================================================================
                        // JALUR 2: ENSEMBLE AI
                        // ====================================================================
                        // Cek status AI pakai realTimeUiState!
                        if (realTimeUiState.isPoseSuggestionActive) {

                            // --- FASE 1: YOLO SCANNING ---
                            if (realTimeUiState.aiPhase == AiPhase.SCANNING) {
                                if (!isYoloProcessing) {
                                    isYoloProcessing = true

                                    val yoloBitmap = imageProxy.toBitmap()
                                    imageProxy.close()

                                    coroutineScope.launch(Dispatchers.Default) {
                                        try {
                                            val matrix = android.graphics.Matrix()
                                            matrix.postRotate(rotation.toFloat())
                                            if (isFrontCamera) matrix.postScale(
                                                -1f,
                                                1f,
                                                yoloBitmap.width / 2f,
                                                yoloBitmap.height / 2f
                                            )

                                            val softwareBitmap =
                                                android.graphics.Bitmap.createBitmap(
                                                    yoloBitmap,
                                                    0,
                                                    0,
                                                    yoloBitmap.width,
                                                    yoloBitmap.height,
                                                    matrix,
                                                    true
                                                )

                                            val yoloResult =
                                                yoloAnalyzer.analyzeFrame(softwareBitmap)
                                            kotlinx.coroutines.delay(3000)
                                            viewModel.onEvent(
                                                CameraEvent.OnYoloScanComplete(
                                                    yoloResult
                                                )
                                            )
                                            softwareBitmap.recycle()
                                        } catch (e: Exception) {
                                            Log.e("YOLO", "Error", e)
                                        } finally {
                                            yoloBitmap.recycle()
                                            isYoloProcessing = false
                                        }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            // --- FASE 3: SOLO MODE (ML KIT TRACKING) ---
                            else if (realTimeUiState.aiPhase == AiPhase.READY_TO_MATCH) {
                                if (!isPoseProcessing) {
                                    isPoseProcessing = true

                                    val isPortrait = rotation == 90 || rotation == 270
                                    val imgW =
                                        (if (isPortrait) imageProxy.height else imageProxy.width).toFloat()
                                    val imgH =
                                        (if (isPortrait) imageProxy.width else imageProxy.height).toFloat()

                                    val image = InputImage.fromMediaImage(mediaImage, rotation)

                                    poseDetector.process(image).addOnSuccessListener { pose ->
                                        faceClient.process(image).addOnSuccessListener { faces ->

                                            if (faces.size >= 2) {
                                                viewModel.onEvent(CameraEvent.SwitchAiPhase(AiPhase.GROUP_MATCH))
                                                return@addOnSuccessListener
                                            }

                                            val ls =
                                                pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                            val rs =
                                                pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

                                            if (ls != null && rs != null && ls.inFrameLikelihood > 0.4f) {
                                                isPersonDetected = true
                                                val midShoulderX =
                                                    (ls.position.x + rs.position.x) / 2f
                                                val midShoulderY =
                                                    (ls.position.y + rs.position.y) / 2f
                                                userAnchorX =
                                                    if (isFrontCamera) 1f - (midShoulderX / imgW) else (midShoulderX / imgW)
                                                userAnchorY = midShoulderY / imgH
                                                userBodyScale =
                                                    Math.abs(ls.position.x - rs.position.x) / imgW

                                                val lw =
                                                    pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                                                val rw =
                                                    pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

                                                if (lw != null && rw != null) {
                                                    val lwY = lw.position.y / imgH;
                                                    val rwY = rw.position.y / imgH
                                                    val lsY = ls.position.y / imgH;
                                                    val rsY = rs.position.y / imgH
                                                    val lwX = lw.position.x / imgW;
                                                    val rwX = rw.position.x / imgW
                                                    val lsX = ls.position.x / imgW;
                                                    val rsX = rs.position.x / imgW

                                                    val leftHandUp = lwY < lsY - 0.05f;
                                                    val rightHandUp = rwY < rsY - 0.05f
                                                    val leftHandWayUp = lwY < lsY - 0.15f;
                                                    val rightHandWayUp = rwY < rsY - 0.15f
                                                    val leftHandLow = lwY > lsY + 0.1f;
                                                    val rightHandLow = rwY > rsY + 0.1f
                                                    val leftHandMid =
                                                        lwY > lsY - 0.1f && lwY < lsY + 0.3f
                                                    val rightHandMid =
                                                        rwY > rsY - 0.1f && rwY < rsY + 0.3f

                                                    val isLeftHandOnChest =
                                                        lwY > lsY && lwY < lsY + 0.4f && lwX > minOf(
                                                            lsX,
                                                            rsX
                                                        ) && lwX < maxOf(lsX, rsX)
                                                    val isRightHandOnChest =
                                                        rwY > rsY && rwY < rsY + 0.4f && rwX > minOf(
                                                            lsX,
                                                            rsX
                                                        ) && rwX < maxOf(lsX, rsX)
                                                    val wristsSpread =
                                                        Math.abs(lwX - rwX) > (Math.abs(lsX - rsX) * 1.5f)
                                                    val wristsCloseToFace =
                                                        Math.abs(lwX - rwX) < (Math.abs(lsX - rsX) * 1.0f)

                                                    // Pastikan pakai realTimeUiState untuk nangkep perubahan pose!
                                                    isPoseMatched =
                                                        when (realTimeUiState.currentPoseType) {
                                                            Y2KPoseType.HALF_BODY_PEACE -> (leftHandUp && rightHandLow) || (leftHandLow && rightHandUp)
                                                            Y2KPoseType.HALF_BODY_COOL -> isLeftHandOnChest && isRightHandOnChest
                                                            Y2KPoseType.HALF_BODY_SALUTE -> (leftHandWayUp && rightHandLow) || (leftHandLow && rightHandWayUp)
                                                            Y2KPoseType.HALF_BODY_FRAME -> leftHandMid && rightHandMid && wristsCloseToFace
                                                            Y2KPoseType.HALF_BODY_FLEX -> leftHandUp && rightHandUp && wristsSpread
                                                            Y2KPoseType.HALF_BODY_POINT -> (leftHandMid && rightHandLow) || (leftHandLow && rightHandMid)
                                                            Y2KPoseType.HALF_BODY_GUNS -> leftHandMid && rightHandMid && wristsCloseToFace

                                                            Y2KPoseType.FULL_BODY_WIDE -> leftHandLow && rightHandLow && wristsSpread
                                                            Y2KPoseType.FULL_BODY_ACTION -> leftHandUp || rightHandUp
                                                            Y2KPoseType.FULL_BODY_HANDS_UP -> leftHandWayUp && rightHandWayUp
                                                            Y2KPoseType.FULL_BODY_T_POSE -> leftHandMid && rightHandMid && wristsSpread
                                                            Y2KPoseType.FULL_BODY_ONE_UP -> (leftHandWayUp && rightHandLow) || (leftHandLow && rightHandWayUp)
                                                            Y2KPoseType.FULL_BODY_HEAD -> leftHandUp && rightHandUp && wristsCloseToFace
                                                            Y2KPoseType.FULL_BODY_CROSS -> isLeftHandOnChest && isRightHandOnChest
                                                        }
                                                } else isPoseMatched = false
                                            } else {
                                                isPersonDetected = false; isPoseMatched = false
                                            }

                                            val currentTime = System.currentTimeMillis()
                                            if (isPoseMatched) {
                                                if (poseMatchStartTime == 0L) poseMatchStartTime =
                                                    currentTime
                                                if ((faces.firstOrNull()?.smilingProbability?.let { it > 0.15f } == true || currentTime - poseMatchStartTime > 1500L)
                                                    && (currentTime - lastPhotoTakenTime > 3000L)) {
                                                    lastPhotoTakenTime = currentTime
                                                    poseMatchStartTime = 0L
                                                    viewModel.onEvent(CameraEvent.TakePhotoLocal)
                                                    viewModel.onEvent(CameraEvent.CycleTargetPose)
                                                }
                                            } else poseMatchStartTime = 0L

                                        }.addOnCompleteListener {
                                            isPoseProcessing = false
                                            imageProxy.close()
                                        }.addOnFailureListener {
                                            isPoseProcessing = false
                                            imageProxy.close()
                                        }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            // --- FASE 4: GROUP MODE ---
                            else if (realTimeUiState.aiPhase == AiPhase.GROUP_MATCH) {
                                if (!isPoseProcessing) {
                                    isPoseProcessing = true
                                    val image = InputImage.fromMediaImage(mediaImage, rotation)

                                    faceClient.process(image).addOnSuccessListener { faces ->
                                        if (faces.size < 2) {
                                            viewModel.onEvent(CameraEvent.SwitchAiPhase(AiPhase.READY_TO_MATCH))
                                            return@addOnSuccessListener
                                        }
                                        if (faces.count {
                                                (it.smilingProbability ?: 0f) > 0.15f
                                            } >= 2 && (System.currentTimeMillis() - lastPhotoTakenTime > 3000L)) {
                                            lastPhotoTakenTime = System.currentTimeMillis()
                                            viewModel.onEvent(CameraEvent.TakePhotoLocal)
                                        }
                                    }.addOnCompleteListener {
                                        isPoseProcessing = false
                                        imageProxy.close()
                                    }.addOnFailureListener {
                                        isPoseProcessing = false
                                        imageProxy.close()
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            } else {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            } catch (exc: Exception) {
                Log.e("CAMERA_LOG", "Gagal bind", exc)
                if (uiState.isUltrawideActive) {
                    Toast.makeText(
                        context,
                        "OS memblokir mode ini pada lensa Ultrawide",
                        Toast.LENGTH_SHORT
                    ).show()
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
                val applyZoom =
                    if (uiState.isUltrawideActive && minZ >= 1f) 1f else if (uiState.zoomRatio < minZ) minZ else uiState.zoomRatio
                control.setZoomRatio(applyZoom.coerceIn(minZ, maxZ))
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(uiState.iso, uiState.shutterSpeed, cameraControl) {
        cameraControl?.let { control ->
            try {
                val c2Control = Camera2CameraControl.from(control)
                val builder = CaptureRequestOptions.Builder()

                if (uiState.iso != 100 || uiState.shutterSpeed > 0L) {
                    builder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    )
                    val applyIso = if (uiState.iso == 100) 800 else uiState.iso
                    val applyShutter =
                        if (uiState.shutterSpeed == 0L) 33333333L else uiState.shutterSpeed
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, applyIso)
                    builder.setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        applyShutter
                    )
                } else {
                    builder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                }
                c2Control.captureRequestOptions = builder.build()
            } catch (e: Exception) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun executeCapture() {
        if (uiState.isVideoMode) {
            if (activeRecording != null) {
                activeRecording?.stop()
                activeRecording = null
                coroutineScope.launch {
                    delay(500); latestPhotoUri = getLatestVibeCheckImage(context)
                }
            } else {
                val name = SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US
                ).format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/VibeCheck"
                    )
                }
                val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                    .setContentValues(contentValues).build()

                val hasAudioPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                var pendingRecording =
                    videoCapture.output.prepareRecording(context, mediaStoreOutput)
                if (hasAudioPerm) pendingRecording = pendingRecording.withAudioEnabled()

                activeRecording =
                    pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            activeRecording = null
                            if (!event.hasError()) Toast.makeText(
                                context,
                                "Video tersimpan!",
                                Toast.LENGTH_SHORT
                            ).show()
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
                    while (countdownDisplay > 0) {
                        delay(1000); countdownDisplay--
                    }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (is169) 0.dp else 120.dp)
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // --- RENDER ANIMASI SCANNING (SAAT YOLO MIKIR) ---
            if (uiState.isPoseSuggestionActive && uiState.aiPhase == AiPhase.SCANNING) {
                ScanningOverlay(modifier = Modifier.fillMaxSize())
            }

            // --- RENDER SILUET TUBUH JIKA AI AKTIF & ORANG TERDETEKSI ---
            else if (uiState.isPoseSuggestionActive && uiState.aiPhase == AiPhase.READY_TO_MATCH && isPersonDetected) {
                PoseOverlay(
                    poseType = uiState.currentPoseType,
                    isMatched = isPoseMatched,
                    anchorX = userAnchorX,
                    anchorY = userAnchorY,
                    bodyScale = userBodyScale,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- RENDER FRAME MAJALAH JIKA AI AKTIF & GRUP ---
            else if (uiState.isPoseSuggestionActive && uiState.aiPhase == AiPhase.GROUP_MATCH) {
                GroupOverlay(modifier = Modifier.fillMaxSize())
            }

            if (countdownDisplay > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownDisplay.toString(),
                        style = Y2KTypography.titleLarge.copy(fontSize = 180.sp),
                        color = Color.Red
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = screenFlashAlpha.value))
            )

            if (isRecording) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 100.dp, start = 32.dp),
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

            if (!isConnectedToRemote && !uiState.isVideoMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- TOMBOL ISO ---
                    Column(
                        modifier = Modifier
                            .size(56.dp) // <-- BIKIN UKURAN FIXED (KOTAK SOLID)
                            .background(Color.Black.copy(0.7f))
                            .border(1.dp, Color.Green, RectangleShape)
                            .clickable {
                                viewModel.onEvent(
                                    CameraEvent.SetIso(
                                        isoList[(isoList.indexOf(
                                            uiState.iso
                                        ) + 1) % isoList.size]
                                    )
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center // <-- BIAR TEXT KE TENGAH
                    ) {
                        Text(
                            "ISO",
                            color = Color.Green,
                            fontSize = 10.sp,
                            style = Y2KTypography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (uiState.iso == 100) "AUTO" else uiState.iso.toString(),
                            color = Color.White,
                            style = Y2KTypography.bodyMedium
                        )
                    }

                    // --- TOMBOL SHT ---
                    Column(
                        modifier = Modifier
                            .size(56.dp) // <-- BIKIN UKURAN FIXED
                            .background(Color.Black.copy(0.7f))
                            .border(1.dp, Color.Green, RectangleShape)
                            .clickable {
                                val idx =
                                    shutterList.indexOf(uiState.shutterSpeed).takeIf { it >= 0 }
                                        ?: 0
                                viewModel.onEvent(CameraEvent.SetShutterSpeed(shutterList[(idx + 1) % shutterList.size]))
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "SHT",
                            color = Color.Green,
                            fontSize = 10.sp,
                            style = Y2KTypography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val idx = shutterList.indexOf(uiState.shutterSpeed).takeIf { it >= 0 } ?: 0
                        Text(
                            shutterLabels[idx],
                            color = Color.White,
                            style = Y2KTypography.bodyMedium
                        )
                    }

                    if (uiState.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        Column(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(0.7f))
                                .border(
                                    1.dp,
                                    if (uiState.isPoseSuggestionActive) Color.Magenta else Color.DarkGray,
                                    RectangleShape
                                )
                                .clickable { viewModel.onEvent(CameraEvent.TogglePoseSuggestion) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "AI",
                                color = if (uiState.isPoseSuggestionActive) Color.Magenta else Color.DarkGray,
                                fontSize = 10.sp,
                                style = Y2KTypography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (uiState.isPoseSuggestionActive) "ON" else "OFF",
                                color = Color.White,
                                style = Y2KTypography.bodyMedium
                            )
                        }
                    }
                }
            }

            // --- TOP OVERLAY BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                if (!isConnectedToRemote) {
                    val isFlashOn = uiState.flashMode == ImageCapture.FLASH_MODE_ON
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(45.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(2.dp, Color.White, RectangleShape)
                            .clickable(enabled = !isRecording) { viewModel.onEvent(CameraEvent.ToggleFlashLocal) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeControlBtn(
                            if (uiState.timerSeconds == 0) "TMR:OFF" else "TMR:${uiState.timerSeconds}s",
                            Color.White,
                            !isRecording
                        ) { viewModel.onEvent(CameraEvent.ToggleTimerLocal) }
                        ModeControlBtn(
                            if (is169) "16:9" else "4:3",
                            Color.Cyan,
                            !isRecording
                        ) { viewModel.onEvent(CameraEvent.ToggleAspectRatio) }
                        ModeControlBtn(
                            if (uiState.isVideoMode) "VID" else "PIC",
                            if (uiState.isVideoMode) Color.Red else Color.Cyan,
                            !isRecording
                        ) { viewModel.onEvent(CameraEvent.ToggleVideoModeLocal) }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(45.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(2.dp, Color.Red, RectangleShape)
                        .clickable(enabled = !isRecording) { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.Red)
                }
            }

            if (!isConnectedToRemote && !isRecording && uiState.lensFacing == CameraSelector.LENS_FACING_BACK) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (is169) 130.dp else 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(2.dp, Color.White, RectangleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val uwLabel =
                        if (dynamicMinZoom == 0.6f) ".6" else if (dynamicMinZoom == 0.5f) ".5" else String.format(
                            Locale.US,
                            "%.1f",
                            dynamicMinZoom
                        ).replace("0.", ".")
                    val zooms = listOf(dynamicMinZoom, 1f, 2f)
                    zooms.forEach { z ->
                        val isSelected =
                            (z < 1f && uiState.isUltrawideActive) || (z >= 1f && uiState.zoomRatio == z && !uiState.isUltrawideActive)
                        val label = if (z < 1f) uwLabel else "${z.toInt()}x"

                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            style = Y2KTypography.bodyMedium,
                            modifier = Modifier
                                .background(if (isSelected) Color.Cyan else Color.Transparent)
                                .clickable {
                                    viewModel.onEvent(CameraEvent.SetZoomLocal(z))
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        val bottomBarBg = if (is169) Color.Black.copy(alpha = 0.5f) else Color(0xFF1A1A1A)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(bottomBarBg)
                .border(if (is169) 0.dp else 2.dp, Color.DarkGray, RectangleShape)
        ) {
            if (isConnectedToRemote) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("P2P: LINKED", color = Color.Green, style = Y2KTypography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "> REMOTE CTRL ACTIVE <",
                        color = Color.Magenta,
                        style = Y2KTypography.bodyLarge,
                        modifier = Modifier.y2kBlinkEffect(500)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                ) {

                    if (!isRecording) {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            AlbumThumbnail(uri = latestPhotoUri) {
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
                            .clickable(
                                interactionSource = shutterInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.onEvent(CameraEvent.TakePhotoLocal)
                            }
                            .background(
                                when {
                                    isRecording -> Color.Red; uiState.isVideoMode -> Color.DarkGray; else -> Color.Magenta
                                }
                            )
                            .border(4.dp, Color.White, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.White, RectangleShape)
                            )
                        } else if (uiState.isVideoMode) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.Red, RectangleShape)
                            )
                        }
                    }

                    if (!isRecording) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .border(2.dp, Color.White, RectangleShape)
                                .clickable {
                                    if (uiState.isUltrawideActive) viewModel.onEvent(
                                        CameraEvent.SetZoomLocal(
                                            1f
                                        )
                                    )
                                    viewModel.onEvent(CameraEvent.FlipCameraLocal)
                                }
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
fun AlbumThumbnail(uri: Uri?, onClick: () -> Unit) {
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

fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    onPhotoSaved: () -> Unit,
    onCaptureComplete: () -> Unit
) {
    val name =
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Pictures/VibeCheck"
        )
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions, ContextCompat.getMainExecutor(context),
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
fun ModeControlBtn(txt: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
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