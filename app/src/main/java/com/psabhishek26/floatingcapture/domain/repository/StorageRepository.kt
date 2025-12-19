package com.psabhishek26.floatingcapture.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor

data class RecordingEntry(
    val uri: Uri,
    val pfd: ParcelFileDescriptor
)

interface StorageRepository {
    suspend fun saveScreenshot(bitmap: Bitmap): Result<Uri>
    suspend fun createRecordingEntry(): Result<RecordingEntry>
    suspend fun finalizeRecording(uri: Uri): Result<Unit>
}