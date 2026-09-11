package com.scan3d.camera.data.models

import android.graphics.ImageFormat
import java.nio.ByteBuffer

data class BurstFrame(
    val timestamp: Long,
    val exposureTime: Long,
    val iso: Int,
    val focusDistance: Float,
    val lensId: Int = 0,
    val bayerFormat: Int = ImageFormat.RAW_SENSOR,
    val width: Int,
    val height: Int,
    val pixelStride: Int,
    val rowPadding: Int,
    val data: ByteBuffer,
    val colorFilterArray: ByteArray = byteArrayOf(0, 1, 1, 2) // RGGB Bayer pattern
)

enum class LensType {
    MACRO,          // ID 54 - عدسة ماكرو
    PRIMARY_0,      // ID 0 - عدسة أساسية
    PRIMARY_40,     // ID 40 - عدسة أساسية
    ULTRA_WIDE      // ID 2 - عدسة واسعة جداً
}

data class BurstSequence(
    val frames: List<BurstFrame>,
    val exposureCompensations: List<Float> = emptyList(),
    val alignmentOffsets: List<Pair<Float, Float>> = emptyList(),
    val lensType: LensType = LensType.PRIMARY_0
)
