package com.example.vibecheck_dev.presentation.purikura

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color

sealed class PurikuraEvent {
    data class ChangeGrid(val gridType: GridType) : PurikuraEvent()
    data class ChangeFrameStyle(val style: FrameStyle) : PurikuraEvent()
    data class AddPhoto(val index: Int, val uri: Uri) : PurikuraEvent()
    data class ChangeFrameColor(val color: Color) : PurikuraEvent()
    data class ChangeTextColor(val color: Color) : PurikuraEvent() // Event Ganti Warna Teks
    object TogglePanel : PurikuraEvent() // Event Hide/Show Panel
    object ClearAll : PurikuraEvent()

    data class ToggleActionDialog(val show: Boolean) : PurikuraEvent()

    data class SavePurikura(val context: Context, val onResult: (Boolean, String) -> Unit) : PurikuraEvent()
    data class PrintPurikura(val context: Context) : PurikuraEvent()
}