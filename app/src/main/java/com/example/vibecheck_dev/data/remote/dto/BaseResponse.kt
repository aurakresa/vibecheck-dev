package com.example.vibecheck_dev.data.remote.dto

data class BaseResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val error: String?
)