package com.scan3d.camera.domain.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.scan3d.camera.data.models.BurstFrame
import com.scan3d.camera.data.models.BurstSequence
import java.nio.ByteBuffer

/**
 * مدير التقاط صور RAW Burst متعددة العدسات والتعريضات
 * يدعم العدسات التالية:
 * - MACRO: ID 54
 * - PRIMARY: ID 0, 40
 * - ULTRA_WIDE: ID 2
 */
class RawBurstCaptureManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val _burstSequence = MutableLiveData<BurstSequence?>()
    val burstSequence: LiveData<BurstSequence?> = _burstSequence

    private val frames = mutableListOf<BurstFrame>()
    private var captureCount = 0
    private var targetCaptureCount = 3
    private var exposureCompensations = listOf<Float>()
    private var currentLensId = 0

    enum class LensType(val id: String) {
        MACRO("54"),
        PRIMARY_0("0"),
        PRIMARY_40("40"),
        ULTRA_WIDE("2")
    }

    init {
        startBackgroundThread()
    }

    /**
     * ابدأ خيط المعالجة الخلفي
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    /**
     * الحصول على قائمة الكاميرات المتاحة مع معرفات العدسات
     */
    fun getAvailableLenses(): Map<LensType, String> {
        val lenses = mutableMapOf<LensType, String>()
        
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val lens = characteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION)
            
            when (cameraId) {
                LensType.MACRO.id -> lenses[LensType.MACRO] = cameraId
                LensType.PRIMARY_0.id -> lenses[LensType.PRIMARY_0] = cameraId
                LensType.PRIMARY_40.id -> lenses[LensType.PRIMARY_40] = cameraId
                LensType.ULTRA_WIDE.id -> lenses[LensType.ULTRA_WIDE] = cameraId
            }
        }
        
        return lenses
    }

    /**
     * التحقق من دعم RAW
     */
    fun supportsRaw(cameraId: String): Boolean {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        return capabilities?.any { it == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW } ?: false
    }

    /**
     * ابدأ التقاط Burst بعدسة محددة وتعريضات مختلفة
     */
    fun startBurstCapture(
        lensType: LensType,
        burstCount: Int,
        exposureValues: List<Float> = listOf(-1.0f, 0.0f, 1.0f)
    ) {
        val lenses = getAvailableLenses()
        val cameraId = lenses[lensType] ?: return
        
        targetCaptureCount = burstCount
        exposureCompensations = exposureValues.take(burstCount)
        frames.clear()
        captureCount = 0
        currentLensId = cameraId.toIntOrNull() ?: 0

        backgroundHandler?.post {
            openCamera(cameraId)
        }
    }

    /**
     * فتح الكاميرا
     */
    private fun openCamera(cameraId: String) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val rawSize = getOptimalRawSize(characteristics)

            imageReader = ImageReader.newInstance(
                rawSize.width, rawSize.height, ImageFormat.RAW_SENSOR, targetCaptureCount
            ).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }

            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * الحصول على أفضل حجم لالتقاط RAW
     */
    private fun getOptimalRawSize(characteristics: CameraCharacteristics): Size {
        val rawSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.RAW_SENSOR)
            ?.sortedByDescending { it.width * it.height }
            ?: emptyList()
        return rawSizes.firstOrNull() ?: Size(4096, 3072)
    }

    /**
     * Callback حالة الكاميرا
     */
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    /**
     * إنشاء جلسة الالتقاط
     */
    private fun createCaptureSession() {
        try {
            val surfaces = listOf(imageReader?.surface).filterNotNull()
            cameraDevice?.createCaptureSession(
                surfaces,
                captureSessionStateCallback,
                backgroundHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Callback جلسة الالتقاط
     */
    private val captureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            captureNextFrame(0)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {}
    }

    /**
     * التقاط الإطار التالي بتعريض محدد
     */
    private fun captureNextFrame(frameIndex: Int) {
        if (frameIndex >= targetCaptureCount) {
            finalizeBurstCapture()
            return
        }

        try {
            val captureRequest = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            )?.apply {
                addTarget(imageReader!!.surface)
                val exposureValue = exposureCompensations.getOrElse(frameIndex) { 0f }
                val exposureTime = calculateExposureTime(exposureValue)
                set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime)
                set(CaptureRequest.SENSOR_SENSITIVITY, 100 + (frameIndex * 50))
                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            }?.build()

            captureRequest?.let {
                captureSession?.capture(it, captureCallback(frameIndex), backgroundHandler)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * حساب وقت التعريض
     */
    private fun calculateExposureTime(exposureCompensation: Float): Long {
        val baseTime = 33_000_000L
        val multiplier = 2.0.pow(exposureCompensation).toFloat()
        return (baseTime * multiplier).toLong().coerceIn(1_000_000L, 300_000_000L)
    }

    /**
     * Callback الالتقاط
     */
    private fun captureCallback(frameIndex: Int) = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            captureCount++
            captureNextFrame(frameIndex + 1)
        }
    }

    /**
     * Listener الصور
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        processRawImage(image)
        image.close()
    }

    /**
     * معالجة صورة RAW
     */
    private fun processRawImage(image: Image) {
        if (image.format != ImageFormat.RAW_SENSOR) return

        val plane = image.planes[0]
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val frame = BurstFrame(
            timestamp = image.timestamp,
            exposureTime = 33_000_000L,
            iso = 100,
            focusDistance = 0f,
            lensId = currentLensId,
            width = image.width,
            height = image.height,
            pixelStride = plane.pixelStride,
            rowPadding = plane.rowPadding,
            data = ByteBuffer.wrap(data)
        )

        frames.add(frame)
    }

    /**
     * إنهاء التقاط Burst
     */
    private fun finalizeBurstCapture() {
        val sequence = BurstSequence(
            frames = frames.toList(),
            exposureCompensations = exposureCompensations,
            lensType = when (currentLensId) {
                54 -> LensType.MACRO
                2 -> LensType.ULTRA_WIDE
                40 -> LensType.PRIMARY_40
                else -> LensType.PRIMARY_0
            }
        )
        _burstSequence.postValue(sequence)
        releaseCamera()
    }

    /**
     * إغلاق الكاميرا
     */
    fun releaseCamera() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()

        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}

private fun Double.pow(exponent: Float): Double = kotlin.math.pow(this, exponent.toDouble())
