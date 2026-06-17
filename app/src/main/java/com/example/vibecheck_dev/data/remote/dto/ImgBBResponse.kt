package com.example.vibecheck_dev.data.remote.dto

data class ImgBBResponse(
    val data: ImgBBData?,
    val success: Boolean,
    val status: Int
)

data class ImgBBData(
    val url: String,
    val display_url: String? // 🔴 Ini yang kemaren bikin error karena lu belum tambahin
)