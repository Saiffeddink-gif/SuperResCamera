package com.superres.camera.domain.processors

import android.graphics.ImageFormat
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * معالج Bayer المتقدم - بناء قنوات الألوان مباشرة من بيانات RAW
 * بدلاً من استخدام demosaicing أو interpolation تقليدي
 */
class BayerProcessor {

    /**
     * بناء RGB من Bayer RAW مباشرة باستخدام معلومات البكسلات المحيطة
     * RGGB Bayer Pattern:
     * R  G
     * G  B
     */
    fun reconstructRGBFromBayer(
        bayerData: ByteBuffer,
        width: Int,
        height: Int,
        pixelStride: Int,
        rowPadding: Int
    ): IntArray {
        val pixels = IntArray(width * height * 3) // RGB buffer
        val data = ByteArray(bayerData.remaining())
        bayerData.get(data)
        bayerData.rewind()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val (r, g, b) = extractRGBAtPosition(data, x, y, width, height, rowPadding)
                val idx = (y * width + x) * 3
                pixels[idx] = r
                pixels[idx + 1] = g
                pixels[idx + 2] = b
            }
        }
        return pixels
    }

    /**
     * استخراج قيم RGB من موقع معين في Bayer pattern
     * باستخدام interpolation من البكسلات المحيطة
     */
    private fun extractRGBAtPosition(
        data: ByteArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rowPadding: Int
    ): Triple<Int, Int, Int> {
        val isRedRow = y % 2 == 0
        val isRedCol = x % 2 == 0

        val bytesPerPixel = 2 // 10-bit packed as 2 bytes
        val stride = (width + rowPadding) * bytesPerPixel

        val r = when {
            isRedRow && isRedCol -> getValue(data, x, y, stride)
            isRedRow && !isRedCol -> interpolateHorizontal(data, x, y, stride)
            !isRedRow && isRedCol -> interpolateVertical(data, x, y, stride)
            else -> interpolateDiagonal(data, x, y, stride)
        }

        val g = when {
            (isRedRow && !isRedCol) || (!isRedRow && isRedCol) -> getValue(data, x, y, stride)
            else -> interpolateGreen(data, x, y, stride)
        }

        val b = when {
            !isRedRow && !isRedCol -> getValue(data, x, y, stride)
            !isRedRow && isRedCol -> interpolateHorizontal(data, x, y, stride)
            isRedRow && !isRedCol -> interpolateVertical(data, x, y, stride)
            else -> interpolateDiagonal(data, x, y, stride)
        }

        return Triple(
            r.coerceIn(0, 255),
            g.coerceIn(0, 255),
            b.coerceIn(0, 255)
        )
    }

    private fun getValue(data: ByteArray, x: Int, y: Int, stride: Int): Int {
        val offset = y * stride + x * 2
        return if (offset + 1 < data.size) {
            ((data[offset + 1].toInt() and 0xFF) shl 2) or 
            ((data[offset].toInt() and 0xC0) shr 6)
        } else 0
    }

    private fun interpolateHorizontal(data: ByteArray, x: Int, y: Int, stride: Int): Int {
        val left = if (x > 0) getValue(data, x - 1, y, stride) else 0
        val right = getValue(data, x + 1, y, stride)
        return (left + right) / 2
    }

    private fun interpolateVertical(data: ByteArray, x: Int, y: Int, stride: Int): Int {
        val top = if (y > 0) getValue(data, x, y - 1, stride) else 0
        val bottom = getValue(data, x, y + 1, stride)
        return (top + bottom) / 2
    }

    private fun interpolateDiagonal(data: ByteArray, x: Int, y: Int, stride: Int): Int {
        val tl = if (x > 0 && y > 0) getValue(data, x - 1, y - 1, stride) else 0
        val tr = if (y > 0) getValue(data, x + 1, y - 1, stride) else 0
        val bl = if (x > 0) getValue(data, x - 1, y + 1, stride) else 0
        val br = getValue(data, x + 1, y + 1, stride)
        return (tl + tr + bl + br) / 4
    }

    private fun interpolateGreen(data: ByteArray, x: Int, y: Int, stride: Int): Int {
        val neighbors = mutableListOf<Int>()
        if (x > 0) neighbors.add(getValue(data, x - 1, y, stride))
        if (x < stride / 2 - 1) neighbors.add(getValue(data, x + 1, y, stride))
        if (y > 0) neighbors.add(getValue(data, x, y - 1, stride))
        if (y < stride - 1) neighbors.add(getValue(data, x, y + 1, stride))
        return if (neighbors.isNotEmpty()) neighbors.average().toInt() else 0
    }
}
