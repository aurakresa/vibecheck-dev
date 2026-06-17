package com.example.vibecheck_dev.domain.util

import android.graphics.PointF
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.pow
import kotlin.math.sqrt

class IdlePoseTracker {
    private var lastMidpoint: PointF? = null
    // Kita pantau 30 frame terakhir (sekitar 1 detik)
    private val movementBuffer = FloatArray(30)
    private var bufferIndex = 0
    private var isBufferFull = false

    // Batas toleransi pergerakan pixel (Bisa lu tweak nanti pas di-run)
    private val IDLE_THRESHOLD = 80f

    fun processFrame(pose: Pose): Boolean {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftShoulder != null && rightShoulder != null) {
            // Cari titik tengah (dada atas)
            val midX = (leftShoulder.position.x + rightShoulder.position.x) / 2
            val midY = (leftShoulder.position.y + rightShoulder.position.y) / 2
            val currentMidpoint = PointF(midX, midY)

            lastMidpoint?.let { last ->
                // Hitung Euclidean Distance (Jarak pergerakan dari frame sebelumnya)
                val distance = sqrt((currentMidpoint.x - last.x).pow(2) + (currentMidpoint.y - last.y).pow(2))

                movementBuffer[bufferIndex] = distance
                bufferIndex++

                if (bufferIndex >= movementBuffer.size) {
                    bufferIndex = 0
                    isBufferFull = true
                }
            }
            lastMidpoint = currentMidpoint

            // Cek apakah buffer sudah penuh dan total pergerakan sangat kecil (Diam)
            if (isBufferFull) {
                val totalMovement = movementBuffer.sum()
                if (totalMovement < IDLE_THRESHOLD) {
                    return true // USER TERDETEKSI DIAM / KAKU!
                }
            }
        }
        return false
    }

    // Reset tracker saat pop-up muncul biar nggak trigger terus-terusan
    fun reset() {
        isBufferFull = false
        bufferIndex = 0
        movementBuffer.fill(0f)
        lastMidpoint = null
    }
}