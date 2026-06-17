package com.example.vibecheck_dev.domain.util

import com.example.vibecheck_dev.domain.model.DetectedScene
import com.example.vibecheck_dev.domain.model.TargetPose
import androidx.compose.ui.geometry.Offset

class PoseRecommender {

    // Fungsi ini dipanggil setelah YOLO selesai menganalisis layar
    fun getRecommendations(scene: DetectedScene, personCount: Int, screenWidth: Float, screenHeight: Float): List<TargetPose> {
        val poses = mutableListOf<TargetPose>()

        when (personCount) {
            1 -> {
                if (scene == DetectedScene.NATURE) {
                    // Pose 1 Orang di Alam: Tangan merentang ke atas (bebas)
                    poses.add(
                        TargetPose(
                            leftShoulder = Offset(screenWidth * 0.35f, screenHeight * 0.4f),
                            rightShoulder = Offset(screenWidth * 0.65f, screenHeight * 0.4f),
                            leftElbow = Offset(screenWidth * 0.2f, screenHeight * 0.25f),
                            rightElbow = Offset(screenWidth * 0.8f, screenHeight * 0.25f),
                            leftWrist = Offset(screenWidth * 0.1f, screenHeight * 0.1f),
                            rightWrist = Offset(screenWidth * 0.9f, screenHeight * 0.1f),
                            leftHip = Offset(screenWidth * 0.4f, screenHeight * 0.65f),
                            rightHip = Offset(screenWidth * 0.6f, screenHeight * 0.65f)
                        )
                    )
                } else {
                    // Pose Default (Mall/Studio): Tangan di pinggang Y2K Style
                    poses.add(
                        TargetPose(
                            leftShoulder = Offset(screenWidth * 0.25f, screenHeight * 0.35f),
                            rightShoulder = Offset(screenWidth * 0.75f, screenHeight * 0.35f),
                            leftElbow = Offset(screenWidth * 0.15f, screenHeight * 0.5f),
                            rightElbow = Offset(screenWidth * 0.85f, screenHeight * 0.5f),
                            leftWrist = Offset(screenWidth * 0.25f, screenHeight * 0.65f),
                            rightWrist = Offset(screenWidth * 0.75f, screenHeight * 0.65f),
                            leftHip = Offset(screenWidth * 0.35f, screenHeight * 0.65f),
                            rightHip = Offset(screenWidth * 0.65f, screenHeight * 0.65f)
                        )
                    )
                }
            }
            2 -> {
                // TODO: Hitungan untuk 2 orang (Bikin berdampingan Kiri dan Kanan)
                // Nanti kita tambahkan logic pemotongan layar di sini
            }
        }

        return poses
    }
}