package com.arena.btinput.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun DPad(
    size: Dp = 130.dp,
    onDirection: (x: Int, y: Int) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF3A3A3A),
    highlightColor: Color = Color(0xFF6B6B6B),
    alpha: Float = 1f
) {
    var pressedDirection by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed }

                        if (change != null) {
                            val center = Offset(size.toPx() / 2, size.toPx() / 2)
                            val touch = change.position

                            val dx = touch.x - center.x
                            val dy = touch.y - center.y
                            val distance = sqrt(dx * dx + dy * dy)
                            val radius = size.toPx() / 2 * 0.78f

                            if (distance > 8f) {
                                val angle = atan2(dy, dx) * (180 / PI)
                                val normX: Int
                                val normY: Int

                                when {
                                    angle in -22.5..22.5 -> { normX = 1; normY = 0 }
                                    angle in 22.5..67.5 -> { normX = 1; normY = 1 }
                                    angle in 67.5..112.5 -> { normX = 0; normY = 1 }
                                    angle in 112.5..157.5 -> { normX = -1; normY = 1 }
                                    angle in 157.5..202.5 || angle < -157.5 -> { normX = -1; normY = 0 }
                                    angle in -157.5..-112.5 -> { normX = -1; normY = -1 }
                                    angle in -112.5..-67.5 -> { normX = 0; normY = -1 }
                                    angle in -67.5..-22.5 -> { normX = 1; normY = -1 }
                                    else -> { normX = 0; normY = 0 }
                                }

                                pressedDirection = Offset(normX.toFloat(), normY.toFloat())
                                onDirection(normX, normY)
                            }
                        } else {
                            if (pressedDirection != null) {
                                pressedDirection = null
                                onRelease()
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = size.toPx() / 2
            val cy = size.toPx() / 2
            val armLength = size.toPx() * 0.38f
            val armWidth = size.toPx() * 0.22f
            val centerSize = size.toPx() * 0.28f

            val isPressed = pressedDirection != null

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = size.toPx() / 2 - 4.dp.toPx(),
                center = Offset(cx, cy)
            )

            val dirs = listOf(
                Offset(1f, 0f), Offset(-1f, 0f),
                Offset(0f, 1f), Offset(0f, -1f)
            )

            dirs.forEach { dir ->
                val isActive = pressedDirection?.let {
                    (dir.x != 0f && it.x == dir.x) || (dir.y != 0f && it.y == dir.y)
                } ?: false

                val armColor = if (isActive) highlightColor else Color(0xFF2A2A2A)

                val startX = cx + dir.x * centerSize / 2
                val startY = cy + dir.y * centerSize / 2
                val endX = cx + dir.x * armLength
                val endY = cy + dir.y * armLength

                drawRoundRect(
                    color = armColor.copy(alpha = alpha),
                    topLeft = Offset(
                        min(startX, endX) - armWidth / 2,
                        min(startY, endY) - armWidth / 2
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        abs(endX - startX) + armWidth,
                        abs(endY - startY) + armWidth
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(armWidth / 2)
                )
            }

            drawCircle(
                color = (if (isPressed) highlightColor else Color(0xFF444444)).copy(alpha = alpha),
                radius = centerSize,
                center = Offset(cx, cy)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.25f * alpha),
                radius = size.toPx() / 2 - 4.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}