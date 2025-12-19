package com.psabhishek26.floatingcapture.domain.model

import android.net.Uri

sealed class CaptureResult {
    data class Success(val uri: Uri, val type: CaptureType) : CaptureResult()
    data class Error(val message: String, val exception: Throwable? = null) : CaptureResult()
    object InProgress : CaptureResult()
}