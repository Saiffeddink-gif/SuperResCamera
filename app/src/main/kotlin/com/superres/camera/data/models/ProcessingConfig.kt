package com.superres.camera.data.models

data class ProcessingConfig(
    val burstCount: Int = 3,                    // عدد الصور في الـ burst
    val superResZoomLevel: Int = 2,             // مستوى التكبير (2x, 3x, 4x)
    val enableSubPixelAlignment: Boolean = true,// محاذاة تحت البكسل
    val bayerInterpolationMethod: String = "direct_channel_construction", // بناء القنوات مباشرة من RAW
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val outputQuality: Int = 95
)

enum class OutputFormat {
    RAW,    // RAW DNG format
    JPEG,   // JPEG
    HEIC,   // HEIC/HEIF
    PNG,    // PNG
    WEBP    // WebP
}
