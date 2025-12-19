package com.psabhishek26.floatingcapture.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.psabhishek26.floatingcapture.domain.repository.RecordingEntry
import com.psabhishek26.floatingcapture.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageRepository {

    private companion object {
        const val SCREENSHOT_PREFIX = "Screenshot_"
        const val RECORDING_PREFIX = "Recording_"
        const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val PNG_EXTENSION = ".png"
        const val MP4_EXTENSION = ".mp4"
        const val PNG_MIME = "image/png"
        const val MP4_MIME = "video/mp4"
    }

    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)

    override suspend fun saveScreenshot(bitmap: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val filename = "$SCREENSHOT_PREFIX${dateFormat.format(Date())}$PNG_EXTENSION"
            val resolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, PNG_MIME)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Screenshots"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: throw IOException("Failed to open output stream")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            uri
        }
    }

    override suspend fun createRecordingEntry(): Result<RecordingEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val filename = "$RECORDING_PREFIX${dateFormat.format(Date())}$MP4_EXTENSION"
                val resolver = context.contentResolver

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, MP4_MIME)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/ScreenRecordings"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Failed to create video entry")

                val pfd = resolver.openFileDescriptor(uri, "w")
                    ?: throw IOException("Failed to open file descriptor")

                RecordingEntry(uri, pfd)
            }
        }

    override suspend fun finalizeRecording(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
            Unit
        }
    }
}