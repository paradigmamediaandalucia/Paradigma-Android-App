package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.viewmodel.DownloadStatus
import kotlin.math.roundToInt

/**
 * Floating overlay for download progress, to be shown above the player.
 * @param status The current download status, or null if no download in progress.
 * @param modifier Modifier for positioning (should be used with Box for absolute positioning).
 */
@Composable
fun FloatingDownloadProgressOverlay(
    status: DownloadStatus?,
    modifier: Modifier = Modifier
) {
    if (status == null) return
    val clampedProgress = status.progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color(0xFFFFEB3B), // Yellow
                style = Stroke(width = 10f)
            )
            drawArc(
                color = Color.Black,
                startAngle = -90f,
                sweepAngle = clampedProgress * 360f,
                useCenter = false,
                style = Stroke(width = 10f)
            )
        }
        if (status.isComplete) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Descarga finalizada",
                tint = Color.Black,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Text(
                text = "${(clampedProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )
        }
    }
}
