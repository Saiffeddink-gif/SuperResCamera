package com.superres.camera.domain.processors

import kotlin.math.*

/**
 * محرك Super-Resolution - دمج عدة صور Burst بدقة تحت البكسلية
 * لزيادة الدقة إلى 2x أو 3x أو 4x بناءً على عدد الصور المتاحة
 */
class SuperResolutionEngine(
    private val alignmentEngine: SubPixelAlignmentEngine = SubPixelAlignmentEngine()
) {

    /**
     * دمج صور Burst متعددة الإزاحة لإنتاج صورة بدقة أعلى
     * @param frames قائمة الإطارات (يجب أن تكون بنفس الحجم)
     * @param zoomLevel مستوى التكبير (2, 3, أو 4)
     */
    fun mergeBurstFrames(
        frames: List<IntArray>,
        originalWidth: Int,
        originalHeight: Int,
        zoomLevel: Int = 2
    ): IntArray {
        if (frames.isEmpty()) return IntArray(0)
        if (frames.size == 1) return scaleUpImage(frames[0], originalWidth, originalHeight, zoomLevel)

        // محاذاة جميع الإطارات بالنسبة للإطار الأول
        val alignments = alignFrames(frames, originalWidth, originalHeight)

        // إنشاء صورة بدقة أعلى
        val upscaledWidth = originalWidth * zoomLevel
        val upscaledHeight = originalHeight * zoomLevel
        val resultPixels = IntArray(upscaledWidth * upscaledHeight) { 0 }
        val pixelCounts = IntArray(upscaledWidth * upscaledHeight) { 0 }

        // تجميع البيانات من كل إطار بناءً على إزاحته
        for ((frameIdx, frame) in frames.withIndex()) {
            val (offsetX, offsetY) = alignments[frameIdx]

            for (y in 0 until originalHeight) {
                for (x in 0 until originalWidth) {
                    val srcIdx = y * originalWidth + x
                    if (srcIdx < frame.size) {
                        // حساب الموقع في الصورة المكبرة
                        val upscaledX = (x * zoomLevel + (offsetX * zoomLevel).toInt()).coerceIn(0, upscaledWidth - 1)
                        val upscaledY = (y * zoomLevel + (offsetY * zoomLevel).toInt()).coerceIn(0, upscaledHeight - 1)
                        val dstIdx = upscaledY * upscaledWidth + upscaledX

                        resultPixels[dstIdx] += frame[srcIdx]
                        pixelCounts[dstIdx]++
                    }
                }
            }
        }

        // حساب المتوسط الحسابي لكل بكسل
        for (i in resultPixels.indices) {
            if (pixelCounts[i] > 0) {
                resultPixels[i] /= pixelCounts[i]
            }
        }

        // interpolation للبكسلات الفارغة
        return interpolateMissingPixels(resultPixels, upscaledWidth, upscaledHeight, pixelCounts)
    }

    /**
     * محاذاة جميع الإطارات بالنسبة للإطار الأول (Reference frame)
     */
    private fun alignFrames(
        frames: List<IntArray>,
        width: Int,
        height: Int
    ): List<Pair<Float, Float>> {
        val alignments = mutableListOf<Pair<Float, Float>>()
        alignments.add(Pair(0f, 0f)) // الإطار الأول ليس له إزاحة

        for (i in 1 until frames.size) {
            val offset = alignmentEngine.calculateSubPixelOffset(
                frames[0], frames[i], width, height
            )
            alignments.add(offset)
        }

        return alignments
    }

    /**
     * تكبير الصورة باستخدام Lanczos interpolation
     */
    private fun scaleUpImage(
        pixels: IntArray,
        width: Int,
        height: Int,
        zoomLevel: Int
    ): IntArray {
        val newWidth = width * zoomLevel
        val newHeight = height * zoomLevel
        val result = IntArray(newWidth * newHeight)

        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                val srcX = x / zoomLevel.toFloat()
                val srcY = y / zoomLevel.toFloat()
                result[y * newWidth + x] = lanczosInterpolate(
                    pixels, width, height, srcX, srcY
                )
            }
        }

        return result
    }

    /**
     * Lanczos Interpolation لجودة أفضل في التكبير
     */
    private fun lanczosInterpolate(
        pixels: IntArray,
        width: Int,
        height: Int,
        x: Float,
        y: Float,
        radius: Int = 3
    ): Int {
        val ix = x.toInt()
        val iy = y.toInt()
        val dx = x - ix
        val dy = y - iy

        var sum = 0.0
        var weightSum = 0.0

        for (jy in -radius until radius) {
            for (jx in -radius until radius) {
                val px = ix + jx
                val py = iy + jy

                if (px in 0 until width && py in 0 until height) {
                    val weight = lanczosKernel(dx - jx, radius) *
                            lanczosKernel(dy - jy, radius)
                    val pixelValue = pixels[py * width + px]
                    sum += pixelValue * weight
                    weightSum += weight
                }
            }
        }

        return if (weightSum > 0) (sum / weightSum).toInt().coerceIn(0, 255) else 0
    }

    private fun lanczosKernel(x: Float, radius: Int): Double {
        val absX = abs(x)
        return when {
            absX >= radius -> 0.0
            absX < 1e-6 -> 1.0
            else -> {
                val piX = PI * x
                val piXOverRadius = PI * x / radius
                (radius * sin(piX) * sin(piXOverRadius)) / (piX * piXOverRadius)
            }
        }
    }

    /**
     * ملء البكسلات الفارغة بـ interpolation من الجيران
     */
    private fun interpolateMissingPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        counts: IntArray
    ): IntArray {
        for (i in pixels.indices) {
            if (counts[i] == 0) {
                val x = i % width
                val y = i / width
                val neighbors = mutableListOf<Int>()

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val ni = ny * width + nx
                            if (counts[ni] > 0) neighbors.add(pixels[ni])
                        }
                    }
                }

                if (neighbors.isNotEmpty()) {
                    pixels[i] = neighbors.average().toInt()
                }
            }
        }
        return pixels
    }
}
