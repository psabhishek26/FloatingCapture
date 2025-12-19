package com.psabhishek26.floatingcapture.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psabhishek26.floatingcapture.presentation.service.FloatingControlService

@Composable
fun CloseZoneOverlay(state: FloatingControlService.CloseZoneState) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            CloseZone(isHovering = state.isHovering)
        }
    }
}

@Composable
private fun BoxScope.CloseZone(isHovering: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isHovering) 1.3f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isHovering) 1f else 0.7f,
        animationSpec = tween(150),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 48.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isHovering) Color.Red else Color.Red.copy(alpha = 0.7f),
                            if (isHovering) Color.Red.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.4f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = isHovering,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "Release to close",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}