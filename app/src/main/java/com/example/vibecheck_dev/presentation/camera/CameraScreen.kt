package com.example.vibecheck_dev.presentation.camera

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

// Fungsi bantuan untuk mencari Activity dari Context di dalam Compose
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun CameraScreen(viewModel: CameraViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()

    val connectionInfo by viewModel.connectionInfo.collectAsState()

    // AMBIL SATU STATE UTAMA
    val uiState by viewModel.uiState.collectAsState()

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(640, 480))
            .build()
    }

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }
    val isFront = uiState.lensFacing == CameraSelector.LENS_FACING_FRONT

    val screenFlashAlpha = remember { Animatable(0f) }
    val shutterBlinkAlpha = remember { Animatable(0f) }

    var countdownDisplay by remember { mutableStateOf(0) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    DisposableEffect(Unit) {
        viewModel.onEvent(CameraEvent.StartHosting)
        onDispose {
            viewModel.onEvent(CameraEvent.StopHosting)
            backgroundExecutor.shutdown()
        }
    }

    LaunchedEffect(uiState.flashMode) { imageCapture.flashMode = uiState.flashMode }

    LaunchedEffect(uiState.zoomRatio) {
        if (uiState.zoomRatio > 0f) {
            cameraControl?.setZoomRatio(uiState.zoomRatio)
        }
    }

    LaunchedEffect(uiState.lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder().requireLensFacing(uiState.lensFacing).build()

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
                )
                cameraControl = camera.cameraControl

                imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                    try {
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val bitmap = imageProxy.toBitmap()
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 30, stream)
                        val byteArray = stream.toByteArray()

                        // Kirim gambar via Event
                        viewModel.onEvent(CameraEvent.SendVideoFrame(byteArray, rotation, isFront))
                    } catch (e: Exception) {
                        Log.e("CAMERA_LOG", "Gagal memproses frame: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    var zState = camera.cameraInfo.zoomState.value
                    while (zState == null) {
                        delay(100)
                        zState = camera.cameraInfo.zoomState.value
                    }
                    val minZoom = zState.minZoomRatio
                    val maxZoom = zState.maxZoomRatio
                    // Kirim spek via Event
                    viewModel.onEvent(CameraEvent.UpdateHardwareSpecs(minZoom, maxZoom))
                }
            } catch (exc: Exception) {
                Log.e("CAMERA_LOG", "Gagal membuka kamera", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // LOGIKA PENJEPRETAN SCREEN FLASH
    LaunchedEffect(Unit) {
        viewModel.takePhotoTrigger.collect {
            if (uiState.timerSeconds > 0) {
                countdownDisplay = uiState.timerSeconds
                while (countdownDisplay > 0) {
                    delay(1000)
                    countdownDisplay--
                }
            }

            if (uiState.lensFacing == CameraSelector.LENS_FACING_FRONT && uiState.flashMode == ImageCapture.FLASH_MODE_ON) {
                val originalBrightness = activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity?.window?.let { window ->
                    val layoutParams = window.attributes
                    layoutParams.screenBrightness = 1.0f
                    window.attributes = layoutParams
                }
                screenFlashAlpha.snapTo(1f)
                delay(800)
                takePhoto(imageCapture, context) {
                    launch {
                        screenFlashAlpha.animateTo(0f)
                        activity?.window?.let { window ->
                            val layoutParams = window.attributes
                            layoutParams.screenBrightness = originalBrightness
                            window.attributes = layoutParams
                        }
                    }
                }
            } else {
                launch {
                    shutterBlinkAlpha.snapTo(1f)
                    delay(50)
                    shutterBlinkAlpha.animateTo(0f)
                }
                takePhoto(imageCapture, context) {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = shutterBlinkAlpha.value)))
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = screenFlashAlpha.value)))

        if (countdownDisplay > 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = countdownDisplay.toString(),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color.Red))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HOST_MODE", style = MaterialTheme.typography.titleLarge, color = Color.Red)
                }
                val isConnected = connectionInfo?.groupFormed == true
                val statusColor = if (isConnected) Color.Green else Color.Yellow
                Text(
                    text = if (isConnected) "REMOTE: CONNECTED" else "WAITING_REMOTE...",
                    style = MaterialTheme.typography.bodyMedium, color = statusColor,
                    modifier = Modifier.border(2.dp, statusColor).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun takePhoto(imageCapture: ImageCapture, context: Context, onCaptureComplete: () -> Unit) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VibeCheck")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
    ).build()

    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CAMERA_LOG", "Gagal menyimpan foto", exc)
                onCaptureComplete()
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Foto tersimpan!", Toast.LENGTH_SHORT).show()
                onCaptureComplete()
            }
        }
    )
}