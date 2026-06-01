package com.example.vibecheck_dev.presentation.purikura

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class GridType(val rows: Int, val cols: Int, val label: String) {
    STRIP_1X3(3, 1, "1x3 STRIP"),
    STRIP_1X4(4, 1, "1x4 STRIP"),
    GRID_2X2(2, 2, "2x2 GRID"),
    GRID_2X3(3, 2, "2x3 GRID")
}

enum class FrameStyle(val label: String) {
    SOLID("SOLID_COLOR"),
    GRID("NEON_GRID"),
    STRIPES("HAZARD_STRIPES"),
    DOTS("RETRO_DOTS")
}

data class PurikuraUiState(
    val selectedGrid: GridType = GridType.STRIP_1X3,
    val selectedStyle: FrameStyle = FrameStyle.SOLID,
    val photos: Map<Int, Uri> = emptyMap(),
    val frameColor: Color = Color.White,
    val textColor: Color = Color.Cyan, // Tambahan Warna Teks
    val isPanelVisible: Boolean = true, // Tambahan Toggle Panel
    val isSaving: Boolean = false,

    // State untuk UI Action & Printer
    val showActionDialog: Boolean = false,
    val showPrinterDialog: Boolean = false,
    val isScanningPrinters: Boolean = false,
    val simulatedPrinters: List<String> = emptyList()
)