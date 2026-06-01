package com.example.vibecheck_dev.domain.model

data class Y2KPreset(
    val name: String,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f
)

val defaultPresets = listOf(
    Y2KPreset("NORMAL", 1f, 1f, 1f, 0f),
    Y2KPreset("CYBERSHOT", 1.05f, 1.15f, 1.3f, 0.2f),
    Y2KPreset("LUMIX CCD", 0.95f, 1.1f, 0.8f, -0.15f),
    Y2KPreset("IXUS", 1.08f, 1.2f, 1.15f, 0.1f),
    Y2KPreset("FUJIFILM", 1.0f, 1.05f, 0.85f, -0.1f),
    Y2KPreset("KODAK GOLD", 1.1f, 1.05f, 1.2f, 0.4f),
    Y2KPreset("MATRIX", 0.9f, 1.3f, 0.5f, -0.5f)
)