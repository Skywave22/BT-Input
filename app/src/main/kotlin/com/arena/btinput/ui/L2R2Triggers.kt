package com.arena.btinput.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * L2 / R2 analog shoulder triggers.
 * Returns value 0-255 (suitable for gamepad analog triggers).
 */
@Composable
fun L2R2Trigger(
    label: String,
    width: Dp = 68.dp,
    height: Dp = 42.dp,
    color: Color = Color(0xFF455A64),
    onValueChanged: (value: Int) -> Unit,
    onRelease: () -> Unit,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf(0f) } // 0 to 1

    Box(
        modifier = modifier
            .size(width, height)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()

                        if (change != null && change.pressed) {
                            val pos = change.position
                            val newValue = (pos.y / size.height).coerceIn(0f, 1f)
                            value = newValue
                            onValueChanged((value * 255).toInt())
                        } else {
                            if (value > 0f) {
                                value = 0f
                                onRelease()
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background
            drawRoundRect(
                color = color.copy(alpha = alpha * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                topLeft = Offset.Zero,
                size = size
            )

            // Fill level
            val fillHeight = size.height * value
            drawRoundRect(
                color = Color(0xFF81C784).copy(alpha = alpha),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                topLeft = Offset(0f, size.height - fillHeight),
                size = androidx.compose.ui.geometry.Size(size.width, fillHeight)
            )

            // Label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 14.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(label, size.width / 2, size.height / 2 + 5.dp.toPx(), paint)
            }
        }
    }
}