package com.psabhishek26.floatingcapture.domain.usecase

import com.psabhishek26.floatingcapture.domain.model.CaptureResult
import com.psabhishek26.floatingcapture.domain.repository.MediaCaptureRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TakeScreenshotUseCase @Inject constructor(
    private val repository: MediaCaptureRepository
) {
    suspend operator fun invoke(): Flow<CaptureResult> {
        return repository.captureScreenshot()
    }
}