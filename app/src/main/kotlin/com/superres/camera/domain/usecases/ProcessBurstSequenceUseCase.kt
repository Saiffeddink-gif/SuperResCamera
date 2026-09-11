package com.superres.camera.domain.usecases

import com.superres.camera.data.models.BurstSequence
import com.superres.camera.data.models.ProcessingConfig
import com.superres.camera.domain.processors.BayerProcessor
import com.superres.camera.domain.processors.SuperResolutionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
حالة الاستخدام: معالجة تسلسل Burst كامل
من RAW إلى صورة Super-Resolution نهائية
 */
class ProcessBurstSequenceUseCase(
    private val bayerProcessor: BayerProcessor = BayerProcessor(),
    private val superResEngine: SuperResolutionEngine = SuperResolutionEngine()
) {

    suspend fun execute(
        burstSequence: BurstSequence,
        config: ProcessingConfig
    ): IntArray = withContext(Dispatchers.Default) {
        // 1. تحويل جميع إطارات RAW إلى RGB
        val rgbFrames = burstSequence.frames.map { frame ->
            bayerProcessor.reconstructRGBFromBayer(
                frame.data,
                frame.width,
                frame.height,
                frame.pixelStride,
                frame.rowPadding
            )
        }

        // 2. دمج الإطارات بتقنية Super-Resolution
        val resultPixels = superResEngine.mergeBurstFrames(
            frames = rgbFrames,
            originalWidth = burstSequence.frames[0].width,
            originalHeight = burstSequence.frames[0].height,
            zoomLevel = config.superResZoomLevel
        )

        resultPixels
    }
}
