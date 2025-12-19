package com.psabhishek26.floatingcapture.domain.model

data class AppSettings(
    val screenshotEnabled: Boolean = true,
    val recordingEnabled: Boolean = true,
    val floatingButtonVisible: Boolean = true
) {
    val singleActionMode: Boolean
        get() = (screenshotEnabled xor recordingEnabled)

    val singleAction: CaptureType?
        get() = when {
            screenshotEnabled && !recordingEnabled -> CaptureType.SCREENSHOT
            !screenshotEnabled && recordingEnabled -> CaptureType.SCREEN_RECORD
            else -> null
        }
}