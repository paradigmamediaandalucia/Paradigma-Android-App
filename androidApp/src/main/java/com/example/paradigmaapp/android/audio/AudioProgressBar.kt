package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Dibuja una barra de progreso personalizada y manejable, con un círculo siempre visible.
 *
 * @param isLiveStream Si es `true`, la barra no se dibuja.
 * @param progress El progreso real de la reproducción (para la parte coloreada de la barra).
 * @param dragPosition La posición del círculo de arrastre (0.0 a 1.0).
 * @param onDragChange Callback que se invoca continuamente mientras el usuario arrastra.
 * @param onDragEnd Callback que se invoca cuando el usuario suelta el círculo.
 */
@Composable
fun AudioProgressBar(
    isLiveStream: Boolean,
    progress: Float,
    dragPosition: Float,
    onDragChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLiveStream) {
        return
    }

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbRadiusPx = with(LocalDensity.current) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp) // Altura suficiente para que el gesto de arrastre sea cómodo y evitar cortes del pulgar
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val availableWidth = (size.width - 2 * thumbRadiusPx).coerceAtLeast(1f)
                        val newPosition = ((offset.x - thumbRadiusPx) / availableWidth).coerceIn(0f, 1f)
                        onDragChange(newPosition)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val availableWidth = (size.width - 2 * thumbRadiusPx).coerceAtLeast(1f)
                        val newPosition = ((change.position.x - thumbRadiusPx) / availableWidth).coerceIn(0f, 1f)
                        onDragChange(newPosition)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        // Usamos un Canvas para dibujar la barra y el círculo de forma personalizada.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barHeight = 4.dp.toPx()
            val trackStart = thumbRadiusPx
            val trackEnd = size.width - thumbRadiusPx
            val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)

            // Dibuja la barra de fondo
            drawLine(
                color = surfaceVariantColor.copy(alpha = 0.3f),
                start = Offset(trackStart, center.y),
                end = Offset(trackEnd, center.y),
                strokeWidth = barHeight
            )
            // Dibuja la parte de la barra que ya se ha reproducido
            val progressX = trackStart + trackWidth * progress.coerceIn(0f, 1f)
            drawLine(
                color = secondaryColor,
                start = Offset(trackStart, center.y),
                end = Offset(progressX, center.y),
                strokeWidth = barHeight
            )

            // Dibuja el círculo en la posición de arrastre (o en la de reproducción si no se arrastra).
            val circleX = trackStart + trackWidth * dragPosition.coerceIn(0f, 1f)
            drawCircle(color = onPrimaryColor, radius = thumbRadiusPx, center = Offset(circleX, center.y))
        }
    }
}
