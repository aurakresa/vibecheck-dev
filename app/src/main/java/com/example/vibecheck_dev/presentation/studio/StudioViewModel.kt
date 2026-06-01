package com.example.vibecheck_dev.presentation.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibecheck_dev.domain.model.Y2KPreset
import com.example.vibecheck_dev.domain.util.ColorMatrixUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StudioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: StudioEvent) {
        when (event) {
            is StudioEvent.LoadImage -> loadImage(event.uri, event.context)
            is StudioEvent.ChangeTab -> _uiState.update { it.copy(selectedTab = event.index) }
            is StudioEvent.UpdateBrightness -> _uiState.update { it.copy(currentBrightness = event.value) }
            is StudioEvent.UpdateContrast -> _uiState.update { it.copy(currentContrast = event.value) }
            is StudioEvent.UpdateSaturation -> _uiState.update { it.copy(currentSaturation = event.value) }
            is StudioEvent.UpdateWarmth -> _uiState.update { it.copy(currentWarmth = event.value) }
            is StudioEvent.ApplyPreset -> _uiState.update {
                it.copy(
                    currentBrightness = event.preset.brightness,
                    currentContrast = event.preset.contrast,
                    currentSaturation = event.preset.saturation,
                    currentWarmth = event.preset.warmth
                )
            }
            is StudioEvent.ToggleSaveDialog -> _uiState.update { it.copy(showSaveDialog = event.show) }
            is StudioEvent.UpdateNewPresetName -> _uiState.update { it.copy(newPresetName = event.name) }
            is StudioEvent.SaveCustomPreset -> saveCustomPreset()
            is StudioEvent.SaveImage -> saveEditedImage(event.context, event.onResult)
        }
    }

    private fun loadImage(uri: android.net.Uri, context: Context) {
        _uiState.update { it.copy(selectedImageUri = uri) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val safeBmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            loadedAndroidBitmap = safeBmp,
                            loadedImageBitmap = safeBmp.asImageBitmap()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("STUDIO_VM", "Gagal meload gambar dari galeri", e)
            }
        }
    }

    private fun saveCustomPreset() {
        val state = _uiState.value
        if (state.newPresetName.isNotBlank()) {
            val newPreset = Y2KPreset(
                name = state.newPresetName.uppercase(Locale.US),
                brightness = state.currentBrightness,
                contrast = state.currentContrast,
                saturation = state.currentSaturation,
                warmth = state.currentWarmth
            )
            _uiState.update {
                it.copy(
                    savedCustomPresets = it.savedCustomPresets + newPreset,
                    newPresetName = "",
                    showSaveDialog = false
                )
            }
        }
    }

    private fun saveEditedImage(context: Context, onComplete: (Boolean) -> Unit) {
        val state = _uiState.value
        val originalBitmap = state.loadedAndroidBitmap
        if (originalBitmap == null) {
            onComplete(false)
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val matrix = ColorMatrixUtil.createAndroidColorMatrix(
                    state.currentBrightness, state.currentContrast,
                    state.currentSaturation, state.currentWarmth
                )
                val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)
                val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
                canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

                val name = "VibeCheck_Edit_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VibeCheck")
                    }
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                var success = false
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    success = true
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSaving = false) }
                    onComplete(success)
                }
            } catch (e: Exception) {
                Log.e("STUDIO_VM", "Gagal menyimpan foto hasil edit", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSaving = false) }
                    onComplete(false)
                }
            }
        }
    }
}