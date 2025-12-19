package com.psabhishek26.floatingcapture.domain.repository

import android.content.Intent
import com.psabhishek26.floatingcapture.domain.model.CaptureResult
import kotlinx.coroutines.flow.Flow

interface MediaCaptureRepository {
    suspend fun initializeCapture(resultCode: Int, data: Intent): Result<Unit>
    suspend fun captureScreenshot(): Flow<CaptureResult>
    suspend fun startRecording(): Flow<CaptureResult>
    suspend fun stopRecording(): Flow<CaptureResult>
    fun isReady(): Boolean
    fun cleanup()
}