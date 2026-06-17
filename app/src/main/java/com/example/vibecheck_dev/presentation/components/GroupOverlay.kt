package com.example.vibecheck_dev.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun GroupOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val margin = 40f

        // Bikin bingkai kamera digital solid ala Y2K
        drawRect(
            color = Color.Cyan,
            topLeft = Offset(margin, margin),
            size = Size(w - margin * 2, h - margin * 2),
            style = Stroke(width = 10f)
        )

        // Sudut-sudut tebal (Crosshairs)
        val cornerLength = 80f
        val thickStroke = 24f

        // Kiri Atas
        drawLine(Color.Magenta, Offset(margin, margin), Offset(margin + cornerLength, margin), thickStroke)
        drawLine(Color.Magenta, Offset(margin, margin), Offset(margin, margin + cornerLength), thickStroke)
        // Kanan Atas
        drawLine(Color.Magenta, Offset(w - margin, margin), Offset(w - margin - cornerLength, margin), thickStroke)
        drawLine(Color.Magenta, Offset(w - margin, margin), Offset(w - margin, margin + cornerLength), thickStroke)
        // Kiri Bawah
        drawLine(Color.Magenta, Offset(margin, h - margin), Offset(margin + cornerLength, h - margin), thickStroke)
        drawLine(Color.Magenta, Offset(margin, h - margin), Offset(margin, h - margin - cornerLength), thickStroke)
        // Kanan Bawah
        drawLine(Color.Magenta, Offset(w - margin, h - margin), Offset(w - margin - cornerLength, h - margin), thickStroke)
        drawLine(Color.Magenta, Offset(w - margin, h - margin), Offset(w - margin, h - margin - cornerLength), thickStroke)
    }
}