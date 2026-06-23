package com.example.vibecheck_dev.domain.util

import android.graphics.ColorMatrix

object ColorMatrixUtil {
    fun createAndroidColorMatrix(brightness: Float, contrast: Float, saturation: Float, warmth: Float): ColorMatrix {
        val matrix = ColorMatrix()

        matrix.setSaturation(saturation)

        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f + (brightness - 1f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(contrastMatrix)

        if (warmth != 0f) {
            val w = warmth * 50f
            val warmthMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, w,
                0f, 1f, 0f, 0f, w * 0.5f,
                0f, 0f, 1f, 0f, -w,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(warmthMatrix)
        }
        return matrix
    }
}