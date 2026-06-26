package com.arena.btinput.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * High-performance Game Button using Canvas.
 * Zero latency rendering.
 * Consumes all touch events so they don't leak to background trackpad.
 */
@Composable
fun GameButton(
    label: String,
    size: Dp = 72.dp,
    color: Color = Color(0xFF4A90E2),
    pressedColor: Color = Color(0xFF2C6FC3),
    onPress: () -> Unit,
    onRelease: () -> Unit,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.changes.any { it.pressed } && !isPressed -> {
                                isPressed = true
                                onPress()
                            }
                            event.changes.none { it.pressed } && isPressed -> {
                                isPressed = false
                                onRelease()
                            }
                        }
                        // Consume all events so background trackpad doesn't receive them
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = size.toPx() / 2f * 0.92f
            val center = Offset(size.toPx() / 2, size.toPx() / 2)

            // Shadow / outer ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = radius + 3.dp.toPx(),
                center = center.copy(y = center.y + 3.dp.toPx())
            )

            // Main button
            drawCircle(
                color = (if (isPressed) pressedColor else color).copy(alpha = alpha),
                radius = radius
            )

            // Inner highlight / bevel
            drawCircle(
                color = Color.White.copy(alpha = if (isPressed) 0.15f else 0.35f),
                radius = radius * 0.82f,
                center = center.copy(y = center.y - radius * 0.15f)
            )

            // Border
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Label text (simple)
            val textSize = min(size.toPx() * 0.28f, 22.dp.toPx())
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = textSize
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                drawText(
                    label,
                    center.x,
                    center.y + textSize * 0.35f,
                    paint
                )
            }
        }
    }
}