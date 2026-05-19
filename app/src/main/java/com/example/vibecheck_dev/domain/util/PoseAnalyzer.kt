package com.example.vibecheck_dev.domain.util

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

// Struktur Data untuk Garis Tulang (Real-time)
data class PoseBone(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

class PoseAnalyzer(
    private val isFrontCamera: Boolean, // Buat nyesuaiin efek cermin (mirror) kamera depan
    private val onBonesUpdated: (List<PoseBone>) -> Unit, // Ngirim garis kerangka real-time ke UI
    private val onUserConfused: (userContext: String) -> Unit // Deteksi kalau user mati gaya
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector = PoseDetection.getClient(options)

    private var idleStartTime = System.currentTimeMillis()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    processRealTimePose(pose, image.width.toFloat(), image.height.toFloat())
                }
                .addOnFailureListener { e ->
                    Log.e("POSE_ANALYZER", "ML Kit Error: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processRealTimePose(pose: Pose, imageWidth: Float, imageHeight: Float) {
        // 1. CEK USER MATI GAYA (BINGUNG)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (leftWrist != null && rightWrist != null && leftHip != null && rightHip != null) {
            val isHandsDown = leftWrist.position.y > leftHip.position.y && rightWrist.position.y > rightHip.position.y
            val currentTime = System.currentTimeMillis()

            if (isHandsDown) {
                if (currentTime - idleStartTime > 4000) { // Diam 4 detik
                    onUserConfused("HANDS_DOWN")
                    idleStartTime = currentTime
                }
            } else {
                idleStartTime = currentTime
            }
        }

        // 2. BIKIN GARIS KERANGKA (STICKMAN) YANG NEMPLOK DI BADAN REAL-TIME
        val bones = mutableListOf<PoseBone>()

        // Fungsi bantuan untuk narik garis antar 2 sendi
        fun addBone(p1: Int, p2: Int) {
            val l1 = pose.getPoseLandmark(p1)
            val l2 = pose.getPoseLandmark(p2)

            // Kalau kedua sendi kelihatan di kamera
            if (l1 != null && l2 != null && l1.inFrameLikelihood > 0.3f && l2.inFrameLikelihood > 0.3f) {
                // Konversi koordinat ke rasio (0.0 sampai 1.0) biar gampang digambar di layar HP
                val x1 = if (isFrontCamera) 1f - (l1.position.x / imageWidth) else l1.position.x / imageWidth
                val y1 = l1.position.y / imageHeight
                val x2 = if (isFrontCamera) 1f - (l2.position.x / imageWidth) else l2.position.x / imageWidth
                val y2 = l2.position.y / imageHeight
                bones.add(PoseBone(x1, y1, x2, y2))
            }
        }

        // Gambar Badan
        addBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        addBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        addBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        addBone(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)

        // Gambar Tangan
        addBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        addBone(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        addBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        addBone(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Lempar data garis ke UI!
        onBonesUpdated(bones)
    }
}