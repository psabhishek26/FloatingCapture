package com.psabhishek26.floatingcapture.presentation.activity

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

private const val TAG = "ScreenCaptureActivity"
private const val REQUEST_MEDIA_PROJECTION = 1

class ScreenCaptureActivity : Activity() {

    private lateinit var requestType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestType = intent.getStringExtra(EXTRA_REQUEST_TYPE) ?: REQUEST_TYPE_SCREENSHOT

        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()

        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "Permission granted, sending broadcast")

                val intent = Intent(ACTION_SCREEN_CAPTURE_RESULT).apply {
                    putExtra(EXTRA_RESULT_CODE, resultCode)
                    putExtra(EXTRA_RESULT_DATA, data)
                    putExtra(EXTRA_REQUEST_TYPE, requestType)
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            } else {
                Log.e(TAG, "Permission denied or no data")
            }
        }
        finish()
    }

    companion object {
        const val ACTION_SCREEN_CAPTURE_RESULT =
            "com.psabhishek26.floatingcapture.SCREEN_CAPTURE_RESULT"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_REQUEST_TYPE = "request_type"
        const val REQUEST_TYPE_SCREENSHOT = "screenshot"
        const val REQUEST_TYPE_RECORDING = "recording"
    }
}