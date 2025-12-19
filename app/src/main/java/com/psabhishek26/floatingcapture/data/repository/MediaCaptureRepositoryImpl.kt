package com.psabhishek26.floatingcapture.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import com.psabhishek26.floatingcapture.domain.model.CaptureResult
import com.psabhishek26.floatingcapture.domain.model.CaptureType
import com.psabhishek26.floatingcapture.domain.repository.MediaCaptureRepository
import com.psabhishek26.floatingcapture.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MediaCaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository
) : MediaCaptureRepository {

    private companion object {
        private var mediaProjection: MediaProjection? = null
        private const val SCREENSHOT_TIMEOUT_MS = 1000L
        private const val FRAME_DELAY_MS = 100L
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_BIT_RATE = 8 * 1024 * 1024
    }

    private val mutex = Mutex()
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val projectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    private val captureThread = HandlerThread("ScreenCaptureThread").apply { start() }
    private val captureHandler = Handler(captureThread.looper)

    private var mediaRecorder: MediaRecorder? = null
    private var recordingDisplay: VirtualDisplay? = null
    private var isRecording = false
    private var currentRecordingUri: Uri? = null

    override fun isReady(): Boolean = mediaProjection != null

    override suspend fun initializeCapture(resultCode: Int, data: Intent): Result<Unit> =
        withContext(Dispatchers.Main) {
            runCatching {
                mediaProjection?.stop()
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)
                    ?: throw IllegalStateException("Failed to obtain MediaProjection")

                mediaProjection?.registerCallback(
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            mediaProjection = null
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
                Unit
            }
        }

    override suspend fun captureScreenshot(): Flow<CaptureResult> = flow {
        mutex.withLock {
            emit(CaptureResult.InProgress)

            val projection = mediaProjection
            if (projection == null) {
                emit(CaptureResult.Error("Service not initialized"))
                return@withLock
            }

            var imageReader: ImageReader? = null
            var virtualDisplay: VirtualDisplay? = null

            try {
                val metrics = getDisplayMetrics()
                imageReader = createImageReader(metrics)
                virtualDisplay = createVirtualDisplay(projection, metrics, imageReader)

                delay(FRAME_DELAY_MS)

                val image = acquireImage(imageReader)
                if (image != null) {
                    processAndSaveImage(image)?.let { result ->
                        emit(result)
                    } ?: emit(CaptureResult.Error("Failed to save screenshot"))
                } else {
                    emit(CaptureResult.Error("Capture timed out"))
                }
            } catch (e: Exception) {
                emit(CaptureResult.Error("Capture error: ${e.message}"))
            } finally {
                virtualDisplay?.release()
                imageReader?.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun startRecording(): Flow<CaptureResult> = flow {
        mutex.withLock {
            if (isRecording) return@withLock

            val projection = mediaProjection
            if (projection == null) {
                emit(CaptureResult.Error("Service not initialized"))
                return@withLock
            }

            emit(CaptureResult.InProgress)

            try {
                val metrics = getDisplayMetrics()
                val entry = storageRepository.createRecordingEntry().getOrThrow()
                currentRecordingUri = entry.uri

                mediaRecorder = createMediaRecorder(metrics, entry.pfd.fileDescriptor)
                recordingDisplay = createRecordingDisplay(projection, metrics)

                mediaRecorder?.start()
                isRecording = true

                emit(CaptureResult.Success(entry.uri, CaptureType.SCREEN_RECORD))
            } catch (e: Exception) {
                cleanupRecording()
                emit(CaptureResult.Error("Recording failed: ${e.message}"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun stopRecording(): Flow<CaptureResult> = flow {
        mutex.withLock {
            if (!isRecording) return@withLock

            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {
            }

            cleanupRecording()

            currentRecordingUri?.let { uri ->
                storageRepository.finalizeRecording(uri)
                emit(CaptureResult.Success(uri, CaptureType.SCREEN_RECORD))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun cleanup() {
        mediaProjection?.stop()
        mediaProjection = null
        cleanupRecording()
        captureThread.quitSafely()
    }

    private fun createImageReader(metrics: DisplayMetrics): ImageReader =
        ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            3
        )

    private fun createVirtualDisplay(
        projection: MediaProjection,
        metrics: DisplayMetrics,
        imageReader: ImageReader
    ): VirtualDisplay = projection.createVirtualDisplay(
        "Screenshot",
        metrics.widthPixels,
        metrics.heightPixels,
        metrics.densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        imageReader.surface,
        null,
        captureHandler
    ) ?: throw IllegalStateException("Failed to create virtual display")

    private suspend fun acquireImage(imageReader: ImageReader): Image? {
        return imageReader.acquireLatestImage() ?: withTimeoutOrNull(SCREENSHOT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                imageReader.setOnImageAvailableListener({ reader ->
                    try {
                        val img = reader.acquireLatestImage()
                        if (img != null && cont.isActive) {
                            cont.resume(img)
                            reader.setOnImageAvailableListener(null, null)
                        }
                    } catch (_: Exception) {
                    }
                }, captureHandler)

                cont.invokeOnCancellation {
                    imageReader.setOnImageAvailableListener(null, null)
                }
            }
        }
    }

    private suspend fun processAndSaveImage(image: Image): CaptureResult? {
        val bitmap = convertImageToBitmap(image)
        image.close()

        return storageRepository.saveScreenshot(bitmap).fold(
            onSuccess = { uri ->
                bitmap.recycle()
                CaptureResult.Success(uri, CaptureType.SCREENSHOT)
            },
            onFailure = {
                bitmap.recycle()
                null
            }
        )
    }

    private fun createMediaRecorder(
        metrics: DisplayMetrics,
        fd: java.io.FileDescriptor
    ): MediaRecorder =
        MediaRecorder(context).apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(metrics.widthPixels, metrics.heightPixels)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            setVideoEncodingBitRate(VIDEO_BIT_RATE)
            setOutputFile(fd)
            prepare()
        }

    private fun createRecordingDisplay(
        projection: MediaProjection,
        metrics: DisplayMetrics
    ): VirtualDisplay = projection.createVirtualDisplay(
        "Recording",
        metrics.widthPixels,
        metrics.heightPixels,
        metrics.densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mediaRecorder!!.surface,
        null,
        null
    ) ?: throw IllegalStateException("Failed to create recording display")

    private fun cleanupRecording() {
        runCatching { recordingDisplay?.release() }
        runCatching { mediaRecorder?.release() }
        recordingDisplay = null
        mediaRecorder = null
        isRecording = false
    }

    private fun convertImageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)

        return if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height).also {
                bitmap.recycle()
            }
        }
    }

    private fun getDisplayMetrics(): DisplayMetrics = DisplayMetrics().apply {
        val bounds = windowManager.currentWindowMetrics.bounds
        widthPixels = bounds.width()
        heightPixels = bounds.height()
        densityDpi = context.resources.configuration.densityDpi
    }
}