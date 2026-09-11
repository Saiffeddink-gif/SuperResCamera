package com.superres.camera.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import android.os.Environment
import androidx.media3.common.util.UnstableApi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 معالج تصدير الصور بصيغ مختلفة
 */
class ImageExportRepository(private val context: Context) {

    suspend fun exportImage(
        pixels: IntArray,
        width: Int,
        height: Int,
        format: ExportFormat,
        quality: Int = 95
    ): File {
        val fileName = generateFileName(format)
        val outputDir = getOutputDirectory()
        val outputFile = File(outputDir, fileName)

        return when (format) {
            ExportFormat.JPEG -> exportAsJpeg(pixels, width, height, outputFile, quality)
            ExportFormat.PNG -> exportAsPng(pixels, width, height, outputFile)
            ExportFormat.WEBP -> exportAsWebP(pixels, width, height, outputFile, quality)
            ExportFormat.HEIC -> exportAsHeic(pixels, width, height, outputFile, quality)
            ExportFormat.RAW -> exportAsRaw(pixels, width, height, outputFile)
        }
    }

    private fun exportAsJpeg(
        pixels: IntArray,
        width: Int,
        height: Int,
        file: File,
        quality: Int
    ): File {
        val bitmap = createBitmapFromPixels(pixels, width, height)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }
        return file
    }

    private fun exportAsPng(
        pixels: IntArray,
        width: Int,
        height: Int,
        file: File
    ): File {
        val bitmap = createBitmapFromPixels(pixels, width, height)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }

    private fun exportAsWebP(
        pixels: IntArray,
        width: Int,
        height: Int,
        file: File,
        quality: Int
    ): File {
        val bitmap = createBitmapFromPixels(pixels, width, height)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality, fos)
        }
        return file
    }

    private fun exportAsHeic(
        pixels: IntArray,
        width: Int,
        height: Int,
        file: File,
        quality: Int
    ): File {
        // Note: HEIC requires external library or Android 10+ MediaCodec
        // For now, we'll save as JPEG alternative
        return exportAsJpeg(pixels, width, height, file, quality)
    }

    private fun exportAsRaw(
        pixels: IntArray,
        width: Int,
        height: Int,
        file: File
    ): File {
        FileOutputStream(file).use { fos ->
            fos.write(pixels.flatMap { listOf(
                (it and 0xFF).toByte(),
                ((it shr 8) and 0xFF).toByte(),
                ((it shr 16) and 0xFF).toByte(),
                ((it shr 24) and 0xFF).toByte()
            ) }.toByteArray())
        }
        return file
    }

    private fun createBitmapFromPixels(
        pixels: IntArray,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun getOutputDirectory(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "SuperResCamera").apply { mkdirs() }
        }
        return mediaDir ?: context.filesDir
    }

    private fun generateFileName(format: ExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = when (format) {
            ExportFormat.JPEG -> "jpg"
            ExportFormat.PNG -> "png"
            ExportFormat.WEBP -> "webp"
            ExportFormat.HEIC -> "heic"
            ExportFormat.RAW -> "raw"
        }
        return "superres_${timestamp}.$extension"
    }
}

enum class ExportFormat {
    JPEG, PNG, WEBP, HEIC, RAW
}
