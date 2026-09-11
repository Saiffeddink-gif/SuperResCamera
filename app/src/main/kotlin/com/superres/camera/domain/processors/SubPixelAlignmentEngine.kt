package com.superres.camera.domain.processors

import kotlin.math.*

/**
 * محرك المحاذاة تحت البكسلية (Sub-pixel Alignment)
 * يقوم بحساب إزاحات دقيقة بين الإطارات باستخدام cross-correlation
 */
class SubPixelAlignmentEngine {

    /**
     * حساب إزاحة تحت البكسلية بين صورتين باستخدام Phase Correlation
     */
    fun calculateSubPixelOffset(
        referenceFrame: IntArray,
        targetFrame: IntArray,
        width: Int,
        height: Int,
        searchRadius: Int = 32
    ): Pair<Float, Float> {
        var maxCorrelation = 0f
        var bestOffsetX = 0f
        var bestOffsetY = 0f

        // البحث عن أفضل إزاحة في النطاق المحدد
        for (dy in -searchRadius..searchRadius) {
            for (dx in -searchRadius..searchRadius) {
                val correlation = calculateCorrelation(
                    referenceFrame, targetFrame, width, height, dx, dy
                )
                if (correlation > maxCorrelation) {
                    maxCorrelation = correlation
                    bestOffsetX = dx.toFloat()
                    bestOffsetY = dy.toFloat()
                }
            }
        }

        // تحسين الدقة للحصول على إزاحة تحت البكسلية
        return refineSubPixelOffset(
            referenceFrame, targetFrame, width, height,
            bestOffsetX.toInt(), bestOffsetY.toInt()
        )
    }

    /**
     * حساب معامل الترابط (Correlation) بين صورتين مع إزاحة معينة
     */
    private fun calculateCorrelation(
        ref: IntArray,
        target: IntArray,
        width: Int,
        height: Int,
        offsetX: Int,
        offsetY: Int
    ): Float {
        var sum = 0.0
        var count = 0

        for (y in maxOf(0, -offsetY) until minOf(height, height - offsetY)) {
            for (x in maxOf(0, -offsetX) until minOf(width, width - offsetX)) {
                val refIdx = y * width + x
                val targetIdx = (y + offsetY) * width + (x + offsetX)

                if (refIdx < ref.size && targetIdx < target.size) {
                    sum += (ref[refIdx] - target[targetIdx]).toDouble().pow(2.0)
                    count++
                }
            }
        }

        return if (count > 0) (count / (sum + 1e-6)).toFloat() else 0f
    }

    /**
     * تحسين الدقة باستخدام fitting المكافئ (Parabolic fitting)
     */
    private fun refineSubPixelOffset(
        ref: IntArray,
        target: IntArray,
        width: Int,
        height: Int,
        intOffsetX: Int,
        intOffsetY: Int
    ): Pair<Float, Float> {
        val c00 = calculateCorrelation(ref, target, width, height, intOffsetX - 1, intOffsetY - 1)
        val c10 = calculateCorrelation(ref, target, width, height, intOffsetX, intOffsetY - 1)
        val c20 = calculateCorrelation(ref, target, width, height, intOffsetX + 1, intOffsetY - 1)

        val c01 = calculateCorrelation(ref, target, width, height, intOffsetX - 1, intOffsetY)
        val c11 = calculateCorrelation(ref, target, width, height, intOffsetX, intOffsetY)
        val c21 = calculateCorrelation(ref, target, width, height, intOffsetX + 1, intOffsetY)

        val c02 = calculateCorrelation(ref, target, width, height, intOffsetX - 1, intOffsetY + 1)
        val c12 = calculateCorrelation(ref, target, width, height, intOffsetX, intOffsetY + 1)
        val c22 = calculateCorrelation(ref, target, width, height, intOffsetX + 1, intOffsetY + 1)

        // Parabolic fitting
        val dx = 0.5f * (c20 - c00 + c21 - c01 + c22 - c02) / (2 * c11 - c10 - c12 + 1e-6f)
        val dy = 0.5f * (c02 - c00 + c12 - c10 + c22 - c20) / (2 * c11 - c01 - c21 + 1e-6f)

        return Pair(
            intOffsetX + dx.coerceIn(-0.5f, 0.5f),
            intOffsetY + dy.coerceIn(-0.5f, 0.5f)
        )
    }
}
