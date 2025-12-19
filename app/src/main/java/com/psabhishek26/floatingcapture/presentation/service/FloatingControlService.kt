package com.psabhishek26.floatingcapture.presentation.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.psabhishek26.floatingcapture.R
import com.psabhishek26.floatingcapture.domain.model.AppSettings
import com.psabhishek26.floatingcapture.domain.model.CaptureResult
import com.psabhishek26.floatingcapture.domain.model.CaptureType
import com.psabhishek26.floatingcapture.domain.repository.MediaCaptureRepository
import com.psabhishek26.floatingcapture.domain.repository.SettingsRepository
import com.psabhishek26.floatingcapture.domain.usecase.StartRecordingUseCase
import com.psabhishek26.floatingcapture.domain.usecase.StopRecordingUseCase
import com.psabhishek26.floatingcapture.presentation.activity.MainActivity
import com.psabhishek26.floatingcapture.presentation.ui.CloseZoneOverlay
import com.psabhishek26.floatingcapture.presentation.ui.FloatingButtonContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class FloatingControlService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    @Inject lateinit var startRecording: StartRecordingUseCase
    @Inject lateinit var stopRecording: StopRecordingUseCase
    @Inject lateinit var repository: MediaCaptureRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var closeZoneView: ComposeView? = null
    private lateinit var floatingParams: WindowManager.LayoutParams
    private lateinit var vibrator: Vibrator

    private val _uiState = MutableStateFlow(FloatingUiState())
    private val _closeZoneState = MutableStateFlow(CloseZoneState())

    data class FloatingUiState(
        val isExpanded: Boolean = false,
        val isRecording: Boolean = false,
        val recordingDuration: Long = 0L,
        val isDragging: Boolean = false
    )

    data class CloseZoneState(
        val isVisible: Boolean = false,
        val isHovering: Boolean = false
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        startForegroundWithNotification()
        performRestore(null)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        vibrator = getSystemService(Vibrator::class.java)
        windowManager = getSystemService(WindowManager::class.java)

        setupFloatingParams()
        setupCloseZoneWindow()
        observeSettings()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun startForegroundWithNotification() {
        val channelId = "floating_capture_service"
        val manager = getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Floating Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Floating Capture")
            .setContentText("Ready to capture")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        try {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFloatingParams() {
        floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = INITIAL_X
            y = INITIAL_Y
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                if (settings.floatingButtonVisible) {
                    showFloatingButton()
                } else {
                    hideFloatingButton()
                }
            }
        }
    }

    private fun showFloatingButton() {
        if (floatingView != null) return

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingControlService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControlService)

            setContent {
                val uiState by _uiState.collectAsState()
                val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

                FloatingButtonContent(
                    state = uiState,
                    settings = settings,
                    onTap = { handleTap(settings) },
                    onExpand = { _uiState.update { it.copy(isExpanded = !it.isExpanded) } },
                    onScreenshot = { handleScreenshot() },
                    onRecordStart = { handleRecordStart() },
                    onRecordStop = { handleRecordStop() },
                    onDragStart = { handleDragStart() },
                    onDrag = { dx, dy -> handleDrag(dx, dy) },
                    onDragEnd = { handleDragEnd() }
                )
            }
        }

        windowManager?.addView(floatingView, floatingParams)
    }

    private fun hideFloatingButton() {
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null

        _uiState.update { it.copy(isExpanded = false, isDragging = false) }
        _closeZoneState.update { CloseZoneState() }
    }

    private fun setupCloseZoneWindow() {
        val closeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        closeZoneView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingControlService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControlService)

            setContent {
                val closeState by _closeZoneState.collectAsState()
                CloseZoneOverlay(state = closeState)
            }
        }

        windowManager?.addView(closeZoneView, closeParams)
    }

    private fun handleTap(settings: AppSettings) {
        when {
            _uiState.value.isRecording -> handleRecordStop()
            settings.singleActionMode -> {
                when (settings.singleAction) {
                    CaptureType.SCREENSHOT -> handleScreenshot()
                    CaptureType.SCREEN_RECORD -> handleRecordStart()
                    else -> _uiState.update { it.copy(isExpanded = !it.isExpanded) }
                }
            }
            else -> _uiState.update { it.copy(isExpanded = !it.isExpanded) }
        }
    }

    private fun handleDragStart() {
        _uiState.update { it.copy(isDragging = true, isExpanded = false) }
        _closeZoneState.update { it.copy(isVisible = true) }
    }

    private fun handleDrag(dx: Float, dy: Float) {
        floatingParams.x += dx.toInt()
        floatingParams.y += dy.toInt()
        windowManager?.updateViewLayout(floatingView, floatingParams)

        val screenHeight = resources.displayMetrics.heightPixels
        val isInCloseZone = floatingParams.y > screenHeight - CLOSE_ZONE_HEIGHT

        if (isInCloseZone != _closeZoneState.value.isHovering) {
            _closeZoneState.update { it.copy(isHovering = isInCloseZone) }
            if (isInCloseZone) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            }
        }
    }

    private fun handleDragEnd() {
        _uiState.update { it.copy(isDragging = false) }

        if (_closeZoneState.value.isHovering) {
            _closeZoneState.update { CloseZoneState() }
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

            scope.launch {
                settingsRepository.updateSettings(
                    settingsRepository.getSettingsSync().copy(floatingButtonVisible = false)
                )
            }
        } else {
            _closeZoneState.update { it.copy(isVisible = false) }
        }
    }

    private fun handleScreenshot() {
        scope.launch {
            _uiState.update { it.copy(isExpanded = false) }
            floatingParams.alpha = 0f
            windowManager?.updateViewLayout(floatingView, floatingParams)

            delay(SCREENSHOT_DELAY_MS)

            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            hapticFeedback()

            delay(SCREENSHOT_RESTORE_DELAY_MS)

            floatingParams.alpha = 1f
            windowManager?.updateViewLayout(floatingView, floatingParams)
        }
    }

    private fun handleRecordStart() {
        if (!repository.isReady()) {
            Toast.makeText(this, "Setup required for recording...", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(MainActivity.EXTRA_REQUEST_PROJECTION, true)
                }
            )
            return
        }

        scope.launch {
            _uiState.update { it.copy(isExpanded = false) }

            startRecording().collect { result ->
                when (result) {
                    is CaptureResult.Success -> {
                        _uiState.update { it.copy(isRecording = true) }
                        hapticFeedback()
                        startRecordingTimer()
                    }
                    is CaptureResult.Error -> {
                        Toast.makeText(
                            this@FloatingControlService,
                            "Recording error: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleRecordStop() {
        scope.launch {
            stopRecording().collect { result ->
                when (result) {
                    is CaptureResult.Success -> {
                        _uiState.update { it.copy(isRecording = false, recordingDuration = 0L) }
                        hapticFeedback()
                        Toast.makeText(
                            this@FloatingControlService,
                            "Recording saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startRecordingTimer() {
        scope.launch {
            val startTime = SystemClock.elapsedRealtime()
            while (_uiState.value.isRecording) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                _uiState.update { it.copy(recordingDuration = elapsed) }
                delay(TIMER_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun hapticFeedback() {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    private fun performRestore(state: android.os.Bundle?) {
        savedStateRegistryController.performRestore(state)
    }

    override fun onDestroy() {
        repository.cleanup()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        floatingView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        closeZoneView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val NOTIFICATION_ID = 101
        const val INITIAL_X = 50
        const val INITIAL_Y = 200
        const val CLOSE_ZONE_HEIGHT = 200
        const val SCREENSHOT_DELAY_MS = 100L
        const val SCREENSHOT_RESTORE_DELAY_MS = 800L
        const val TIMER_UPDATE_INTERVAL_MS = 1000L
    }
}