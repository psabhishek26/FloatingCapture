package com.psabhishek26.floatingcapture.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.psabhishek26.floatingcapture.domain.model.AppSettings

@Composable
fun MainScreen(
    settings: AppSettings,
    isServiceEnabled: Boolean,
    onEnableService: () -> Unit,
    onSettingsChanged: (AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Floating Capture",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configure your capture options",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        ServiceStatusCard(
            isEnabled = isServiceEnabled,
            onEnableClick = onEnableService
        )

        Spacer(modifier = Modifier.height(24.dp))

        FloatingButtonCard(
            isVisible = settings.floatingButtonVisible,
            isServiceEnabled = isServiceEnabled,
            onVisibilityChanged = { visible ->
                onSettingsChanged(settings.copy(floatingButtonVisible = visible))
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCard(
            settings = settings,
            onSettingsChanged = onSettingsChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        InfoCard(settings = settings)
    }
}

@Composable
private fun ServiceStatusCard(
    isEnabled: Boolean,
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Service Active" else "Service Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isEnabled) {
                        "Accessibility service is running"
                    } else {
                        "Enable accessibility service to start"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isEnabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = onEnableClick) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun FloatingButtonCard(
    isVisible: Boolean,
    isServiceEnabled: Boolean,
    onVisibilityChanged: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show Floating Button",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isVisible) {
                        "Drag to close zone to hide"
                    } else {
                        "Turn on to show floating button"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isVisible,
                onCheckedChange = onVisibilityChanged,
                enabled = isServiceEnabled
            )
        }
    }
}

@Composable
private fun SettingsCard(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Capture Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OptionRow(
                icon = Icons.Default.CameraAlt,
                title = "Screenshot",
                description = "Capture screen images",
                checked = settings.screenshotEnabled,
                onCheckedChange = { enabled ->
                    if (enabled || settings.recordingEnabled) {
                        onSettingsChanged(settings.copy(screenshotEnabled = enabled))
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OptionRow(
                icon = Icons.Default.Videocam,
                title = "Screen Recording",
                description = "Record screen videos with audio",
                checked = settings.recordingEnabled,
                onCheckedChange = { enabled ->
                    if (enabled || settings.screenshotEnabled) {
                        onSettingsChanged(settings.copy(recordingEnabled = enabled))
                    }
                }
            )
        }
    }
}

@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InfoCard(settings: AppSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val behaviorText = when {
                !settings.floatingButtonVisible -> {
                    "The floating button is hidden. Enable it above to use capture features."
                }
                settings.singleActionMode -> {
                    "With only one option enabled, tapping the floating button will directly ${
                        if (settings.screenshotEnabled) "take a screenshot" else "start/stop recording"
                    }."
                }
                else -> {
                    "Tap the floating button to expand options. Hold and drag to the close zone to hide the button."
                }
            }

            Text(
                text = behaviorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}