package com.psabhishek26.floatingcapture.presentation.activity

import android.content.ComponentName
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.psabhishek26.floatingcapture.domain.model.AppSettings
import com.psabhishek26.floatingcapture.domain.repository.MediaCaptureRepository
import com.psabhishek26.floatingcapture.domain.repository.SettingsRepository
import com.psabhishek26.floatingcapture.presentation.service.FloatingControlService
import com.psabhishek26.floatingcapture.presentation.ui.MainScreen
import com.psabhishek26.floatingcapture.ui.theme.FloatingCaptureTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MediaCaptureRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var isServiceEnabled by mutableStateOf(false)

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            lifecycleScope.launch {
                repository.initializeCapture(result.resultCode, result.data!!)
                Toast.makeText(this@MainActivity, "Ready! Use the floating button.", Toast.LENGTH_LONG).show()
                moveTaskToBack(true)
            }
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (intent.getBooleanExtra(EXTRA_REQUEST_PROJECTION, false)) {
            requestProjectionPermission()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                isServiceEnabled = isAccessibilityServiceEnabled()
            }
        }

        setContent {
            FloatingCaptureTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

                    MainScreen(
                        settings = settings,
                        isServiceEnabled = isServiceEnabled,
                        onEnableService = { openAccessibilitySettings() },
                        onSettingsChanged = { newSettings ->
                            lifecycleScope.launch {
                                settingsRepository.updateSettings(newSettings)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_REQUEST_PROJECTION, false)) {
            requestProjectionPermission()
        }
    }

    private fun openAccessibilitySettings() {
        Toast.makeText(
            this,
            "Please enable Floating Capture in Accessibility Settings",
            Toast.LENGTH_LONG
        ).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestProjectionPermission() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, FloatingControlService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent == expectedComponentName) return true
        }
        return false
    }

    companion object {
        const val EXTRA_REQUEST_PROJECTION = "ACTION_REQUEST_PROJECTION"
    }
}