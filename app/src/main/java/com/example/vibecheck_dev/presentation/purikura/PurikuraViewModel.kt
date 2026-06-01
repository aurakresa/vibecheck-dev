package com.example.vibecheck_dev.presentation.purikura

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.print.PrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PurikuraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PurikuraUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: PurikuraEvent) {
        when (event) {
            is PurikuraEvent.ChangeGrid -> _uiState.update { it.copy(selectedGrid = event.gridType) }
            is PurikuraEvent.ChangeFrameStyle -> _uiState.update { it.copy(selectedStyle = event.style) }
            is PurikuraEvent.AddPhoto -> {
                val updatedPhotos = _uiState.value.photos.toMutableMap()
                updatedPhotos[event.index] = event.uri
                _uiState.update { it.copy(photos = updatedPhotos) }
            }
            is PurikuraEvent.ChangeFrameColor -> _uiState.update { it.copy(frameColor = event.color) }
            is PurikuraEvent.ChangeTextColor -> _uiState.update { it.copy(textColor = event.color) }
            is PurikuraEvent.TogglePanel -> _uiState.update { it.copy(isPanelVisible = !it.isPanelVisible) }
            is PurikuraEvent.ClearAll -> _uiState.update { it.copy(photos = emptyMap()) }
            is PurikuraEvent.ToggleActionDialog -> _uiState.update { it.copy(showActionDialog = event.show) }

            is PurikuraEvent.SavePurikura -> executeSave(event.context, event.onResult)
            is PurikuraEvent.PrintPurikura -> executePrint(event.context)
        }
    }

    // --- ENGINE PEMBUAT GAMBAR KANVAS Y2K ---
    private fun generatePurikuraBitmap(context: Context): Bitmap? {
        val state = _uiState.value
        if (state.photos.isEmpty()) return null

        try {
            val grid = state.selectedGrid
            val padding = 40
            val topHeaderHeight = 180 // Ruang atas buat teks VIBECHECK
            val bottomFooterHeight = 220 // Ruang bawah buat ikon Y2K
            val cellSize = 800

            // Kalkulasi Total Lebar & Tinggi Kanvas Baru
            val totalWidth = grid.cols * cellSize + (grid.cols + 1) * padding
            val totalHeight = grid.rows * cellSize + (grid.rows + 1) * padding + topHeaderHeight + bottomFooterHeight

            val resultBmp = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBmp)

            val bgColor = state.frameColor.toArgb()
            val textPixelColor = state.textColor.toArgb()
            val contrastColor = if (state.frameColor == androidx.compose.ui.graphics.Color.Black)
                android.graphics.Color.WHITE else android.graphics.Color.BLACK

            // 1. Gambar Background Solid
            canvas.drawColor(bgColor)

            // 2. Gambar Motif Estetik & Ornamen Y2K
            val patternPaint = Paint().apply {
                color = contrastColor
                alpha = 40
                strokeWidth = 10f
                isAntiAlias = true
            }

            val fontResId = context.resources.getIdentifier("press_start", "font", context.packageName)
            val customTypeface = if (fontResId != 0) {
                ResourcesCompat.getFont(context, fontResId) ?: Typeface.MONOSPACE
            } else {
                Typeface.DEFAULT_BOLD
            }

            val textPaint = Paint().apply {
                color = textPixelColor
                textSize = 90f
                typeface = customTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val iconPaint = Paint(textPaint).apply {
                textSize = 150f
            }

            // TULISAN HEADER PIXEL ART (DI ATAS)
            canvas.drawText("VIBECHECK", totalWidth / 2f, 130f, textPaint)

            // ORNAMEN Y2K BERBEDA & BESAR (PINDAH KE BAWAH / FOOTER)
            val iconY = totalHeight - 70f // Koordinat Y ditarik ke paling bawah
            when (state.selectedStyle) {
                FrameStyle.GRID -> {
                    for (i in 0..totalWidth step 80) canvas.drawLine(i.toFloat(), 0f, i.toFloat(), totalHeight.toFloat(), patternPaint)
                    for (i in 0..totalHeight step 80) canvas.drawLine(0f, i.toFloat(), totalWidth.toFloat(), i.toFloat(), patternPaint)
                    canvas.drawText("👾", 120f, iconY, iconPaint)
                    canvas.drawText("🕹️", totalWidth - 120f, iconY, iconPaint)
                }
                FrameStyle.STRIPES -> {
                    patternPaint.strokeWidth = 30f
                    for (i in -totalHeight..totalWidth step 100) {
                        canvas.drawLine(i.toFloat(), 0f, (i + totalHeight).toFloat(), totalHeight.toFloat(), patternPaint)
                    }
                    canvas.drawText("⚠", 120f, iconY, iconPaint)
                    canvas.drawText("🛑", totalWidth - 120f, iconY, iconPaint)
                }
                FrameStyle.DOTS -> {
                    for (x in 20..totalWidth step 60) {
                        for (y in 20..totalHeight step 60) {
                            canvas.drawCircle(x.toFloat(), y.toFloat(), 8f, patternPaint)
                        }
                    }
                    canvas.drawText("✿", 120f, iconY, iconPaint)
                    canvas.drawText("✌", totalWidth - 120f, iconY, iconPaint)
                }
                FrameStyle.SOLID -> {
                    canvas.drawText("★", 120f, iconY, iconPaint)
                    canvas.drawText("📼", totalWidth - 120f, iconY, iconPaint)
                }
            }

            // 3. Proses Slot Foto
            val photoPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            val emptyPaint = Paint().apply { color = android.graphics.Color.DKGRAY }
            val borderPaint = Paint().apply {
                color = contrastColor
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }

            for (r in 0 until grid.rows) {
                for (c in 0 until grid.cols) {
                    val index = r * grid.cols + c
                    val left = padding + c * (cellSize + padding)
                    val top = topHeaderHeight + padding + r * (cellSize + padding) // Top didorong sejauh tinggi header

                    val uri = state.photos[index]
                    if (uri != null) {
                        val originalBmp = loadBitmap(context, uri)
                        val squareBmp = getSquareCrop(originalBmp, cellSize)
                        canvas.drawBitmap(squareBmp, left.toFloat(), top.toFloat(), photoPaint)
                    } else {
                        canvas.drawRect(left.toFloat(), top.toFloat(), (left + cellSize).toFloat(), (top + cellSize).toFloat(), emptyPaint)
                    }
                    canvas.drawRect(left.toFloat(), top.toFloat(), (left + cellSize).toFloat(), (top + cellSize).toFloat(), borderPaint)
                }
            }
            return resultBmp
        } catch (e: Exception) {
            Log.e("PURIKURA_VM", "Gagal generate bitmap", e)
            return null
        }
    }

    private fun executeSave(context: Context, onResult: (Boolean, String) -> Unit) {
        _uiState.update { it.copy(isSaving = true, showActionDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = generatePurikuraBitmap(context)
            if (bmp == null) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSaving = false) }
                    onResult(false, "Gagal merender gambar")
                }
                return@launch
            }

            val name = "PURIKURA_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VibeCheck")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            try {
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.flush()
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isSaving = false) }
                        onResult(true, "Berhasil simpan ke Galeri!")
                    }
                } else {
                    throw Exception("MediaStore URI Null (Mungkin permission storage belum diizinkan)")
                }
            } catch (e: Exception) {
                Log.e("PURIKURA_SAVE", "Save Error", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSaving = false) }
                    onResult(false, e.localizedMessage ?: "Gagal menyimpan foto")
                }
            }
        }
    }

    private fun executePrint(context: Context) {
        _uiState.update { it.copy(isSaving = true, showActionDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = generatePurikuraBitmap(context)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isSaving = false) }
                if (bmp != null) {
                    val printHelper = PrintHelper(context)
                    printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
                    printHelper.printBitmap("VibeCheck_Purikura_${System.currentTimeMillis()}", bmp)
                }
            }
        }
    }

    private fun loadBitmap(context: Context, uri: android.net.Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                // FIX OOM & HARDWARE BITMAP ISSUE
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    private fun getSquareCrop(bitmap: Bitmap, targetSize: Int): Bitmap {
        val minEdge = Math.min(bitmap.width, bitmap.height)
        val dx = (bitmap.width - minEdge) / 2
        val dy = (bitmap.height - minEdge) / 2
        val cropped = Bitmap.createBitmap(bitmap, dx, dy, minEdge, minEdge)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }
}