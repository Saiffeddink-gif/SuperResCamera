package com.superres.camera.data.models

import android.graphics.ImageFormat
import java.nio.ByteBuffer

data class BurstFrame(
    val timestamp: Long,
    val exposureTime: Long,
    val iso: Int,
    val focusDistance: Float,
    val bayerFormat: Int = ImageFormat.RAW_SENSOR,
    val width: Int,
    val height: Int,
    val pixelStride: Int,
    val rowPadding: Int,
    val data: ByteBuffer,
    val colorFilterArray: ByteArray = byteArrayOf(0, 1, 1, 2) // RGGB Bayer pattern
)

data class BurstSequence(
    val frames: List<BurstFrame>,
    val exposureCompensations: List<Float>,
    val alignmentOffsets: List<Pair<Float, Float>> = emptyList()
)
