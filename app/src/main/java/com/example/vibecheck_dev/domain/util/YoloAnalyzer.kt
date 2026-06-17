package com.example.vibecheck_dev.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.vibecheck_dev.domain.model.DetectedScene
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class YoloAnalyzer(context: Context) {

    private var interpreter: Interpreter? = null

    // Kita pakai resolusi 320x320 biar inferensi cepat & enteng
    private val inputSize = 640

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    init {
        try {
            val tfliteModel = FileUtil.loadMappedFile(context, "yolov8n_int8.tflite")
            val options = Interpreter.Options().apply { numThreads = 2 } // Pake 2 core HP aja
            interpreter = Interpreter(tfliteModel, options)
            Log.d("YOLO", "Model berhasil dimuat!")
        } catch (e: Exception) {
            Log.e("YOLO", "Gagal muat model: ${e.message}")
        }
    }

    fun analyzeFrame(bitmap: Bitmap): SceneResult {
        if (interpreter == null) return SceneResult(DetectedScene.UNKNOWN, 1)

        var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32) // Pakai Float32 walau int8 model (TFLite Support auto-convert)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Wadah output YOLOv8 Nano ukuran 640x640: [1, 84, 8400]
        val outputBuffer = Array(1) { Array(84) { FloatArray(8400) } }

        try {
            interpreter?.run(tensorImage.buffer, outputBuffer)
        } catch (e: Exception) {
            Log.e("YOLO", "Error run: ${e.message}")
            return SceneResult(DetectedScene.UNKNOWN, 1)
        }

        // --- PROSES BEDAH MATRIX (PARSING) ---
        var personCount = 0
        var natureObjects = 0 // Pohon, tanaman, burung, dll
        var mallObjects = 0   // Kursi, TV, buku, laptop, dll

        // Looping ke 8400 tebakan
        for (i in 0 until 8400) {
            var maxConfidence = 0f
            var maxClassId = -1

            // Indeks 0-3 itu koordinat kotak. Class ID mulai dari indeks 4 sampai 83
            for (c in 4..83) {
                val confidence = outputBuffer[0][c][i]
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    maxClassId = c - 4 // Normalisasi jadi 0 - 79
                }
            }

            // Kalau AI yakin di atas 50% sama barang itu
            if (maxConfidence > 0.50f) {
                when (maxClassId) {
                    0 -> personCount++ // 0 adalah ID untuk "Person"
                    // Alam: Burung (14), Kucing (15), Anjing (16), Kuda (17), Sapi (19), Tanaman Pot (58)
                    14, 15, 16, 17, 19, 58 -> natureObjects++
                    // Mall/Indoor: Kursi (56), Sofa (57), Kasur (59), TV (62), Laptop (63), Mouse (64), Remote (65), Buku (73), Jam (74)
                    56, 57, 59, 62, 63, 64, 65, 73, 74 -> mallObjects++
                }
            }
        }

        // NMS Sederhana (Mencegah AI ngitung 1 orang yang sama berkali-kali)
        // Karena kita ngga pakai matrix NMS full, kita bagi aja hasilnya buat kasarannya
        val estimatedPersons = if (personCount > 0) (personCount / 3).coerceAtLeast(1) else 1

        val scene = when {
            natureObjects > mallObjects -> DetectedScene.NATURE
            mallObjects > natureObjects -> DetectedScene.MALL
            else -> DetectedScene.UNKNOWN // Kalau kosong, anggap Unknown
        }

        Log.d("YOLO", "Orang: $estimatedPersons | Alam: $natureObjects | Mall: $mallObjects -> VIBE: $scene")
        return SceneResult(scene, estimatedPersons)
    }

    fun close() {
        interpreter?.close()
    }
}

// Bawa class ini ke luar biar bisa diakses global
data class SceneResult(val scene: DetectedScene, val personCount: Int)