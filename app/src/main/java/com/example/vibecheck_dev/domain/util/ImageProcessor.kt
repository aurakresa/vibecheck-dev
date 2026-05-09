package com.example.vibecheck_dev.domain.util

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageProcessor {

    // FITUR 1: Digicam / Retro Vibe Mode
    fun applyDigicamFilter(original: Bitmap): Bitmap {
        val config = original.config ?: Bitmap.Config.ARGB_8888
        val result = createBitmap(original.width, original.height, config)
        val canvas = Canvas(result)

        // 1. Bikin Efek Warm / CCD Sensor Lawas pakai ColorMatrix
        val colorMatrix = ColorMatrix().apply {
            setSaturation(1.3f) // Boost warna dikit ala kamera saku
            // Matrix untuk nambahin Red & Yellow, ngurangin Blue
            val warmScale = floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,  // R
                0f, 1.0f, 0f, 0f, 10f,  // G
                0f, 0f, 0.8f, 0f, -10f, // B
                0f, 0f, 0f, 1f, 0f      // A
            )
            postConcat(ColorMatrix(warmScale))
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        canvas.drawBitmap(original, 0f, 0f, paint)

        // 2. Tambahin Date Stamp Oranye di Pojok Kanan Bawah
        val textPaint = Paint().apply {
            color = Color.parseColor("#FF9800") // Oranye Y2K
            textSize = original.height * 0.05f
            typeface = Typeface.MONOSPACE
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        val dateString = SimpleDateFormat("yyyy' 'MM' 'dd", Locale.US).format(Date())
        canvas.drawText(dateString, original.width * 0.65f, original.height * 0.95f, textPaint)

        return result
    }

    // FITUR 2: Photobooth / Photo Strip Generator
    fun createPhotoStrip(bitmaps: List<Bitmap>, frameColor: Int = Color.BLACK): Bitmap {
        if (bitmaps.isEmpty()) return createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val width = bitmaps[0].width
        val height = bitmaps[0].height
        val padding = 40 // Ketebalan frame

        val stripWidth = width + (padding * 2)
        val stripHeight = (height * bitmaps.size) + (padding * (bitmaps.size + 1))

        val result = createBitmap(stripWidth, stripHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(frameColor) // Warna frame (Bisa diganti Pink/Magenta nanti)

        for (i in bitmaps.indices) {
            val top = padding + (i * (height + padding))
            canvas.drawBitmap(bitmaps[i], padding.toFloat(), top.toFloat(), null)
        }
        return result
    }
}