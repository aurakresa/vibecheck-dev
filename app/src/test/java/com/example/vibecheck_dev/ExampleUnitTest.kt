package com.example.vibecheck_dev

import com.example.vibecheck_dev.domain.util.ColorMatrixUtil
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Kita pakai Robolectric supaya bisa ngetest class bawaan Android di Ubuntu tanpa buka emulator
@RunWith(RobolectricTestRunner::class)
class ColorMatrixUtilTest {

    @Test
    fun `matriks warna untuk Y2K manly but bright tersetting dengan benar`() {
        // 1. ARRANGE: Siapkan parameter input (misal preset buat filter)
        val brightness = 1.2f // Sedikit terang (bright)
        val contrast = 1.3f   // Kontras tegas (manly)
        val saturation = 0.9f
        val warmth = 0.5f     // Kasih sedikit tone hangat

        // 2. ACT: Panggil fungsi dari ColorMatrixUtil
        val matrix = ColorMatrixUtil.createAndroidColorMatrix(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth
        )

        // 3. ASSERT: Verifikasi hasilnya
        val matrixArray = matrix.array

        // Kita ngecek matrix.array-nya gak boleh kosong
        assertNotNull("Matriks warna tidak boleh null", matrixArray)

        // Cek apakah parameter warmth berfungsi menambah offset warna merah.
        // Di formula matriks lu, warmth positif bikin nilai di index ke-4 (Red offset) jadi positif.
        assertTrue("Offset warna merah harus bertambah karena efek warmth positif", matrixArray[4] > 0f)

        // Dan efek warmth di formula lu harus ngurangin offset biru (index ke-14)
        assertTrue("Offset warna biru harus berkurang karena efek warmth positif", matrixArray[14] < 0f)
    }
}