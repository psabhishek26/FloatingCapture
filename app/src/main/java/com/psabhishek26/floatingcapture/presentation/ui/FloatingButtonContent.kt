package com.psabhishek26.floatingcapture.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psabhishek26.floatingcapture.R
import com.psabhishek26.floatingcapture.domain.model.AppSettings
import com.psabhishek26.floatingcapture.presentation.service.FloatingControlService

@Composable
fun FloatingButtonContent(
    state: FloatingControlService.FloatingUiState,
    settings: AppSettings,
    onTap: () -> Unit,
    onExpand: () -> Unit,
    onScreenshot: () -> Unit,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = state.isExpanded && !state.isDragging,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            ExpandedMenu(
                settings = settings,
                isRecording = state.isRecording,
                onScreenshot = onScreenshot,
                onRecordStart = onRecordStart,
                onRecordStop = onRecordStop
            )
        }

        FloatingButton(
            state = state,
            settings = settings,
            onTap = onTap
        )
    }
}

@Composable
private fun ExpandedMenu(
    settings: AppSettings,
    isRecording: Boolean,
    onScreenshot: () -> Unit,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (settings.screenshotEnabled) {
            IconButton(onClick = onScreenshot) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Screenshot")
            }
        }

        if (settings.recordingEnabled) {
            IconButton(onClick = if (isRecording) onRecordStop else onRecordStart) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = if (isRecording) Color.Red else LocalContentColor.current
                )
            }
        }
    }
}

@Composable
private fun FloatingButton(
    state: FloatingControlService.FloatingUiState,
    settings: AppSettings,
    onTap: () -> Unit
) {
    FloatingActionButton(
        onClick = onTap,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        containerColor = when {
            state.isRecording -> Color.Red
            state.isDragging -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }
    ) {
        when {
            state.isRecording -> {
                Text(
                    text = formatDuration(state.recordingDuration),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }

            state.isExpanded -> {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close menu",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            else -> {
                Icon(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = "Menu",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 1000) / 60
    return if (minutes > 0) "${minutes}m" else "${seconds}s"
}