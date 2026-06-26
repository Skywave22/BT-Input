package com.arena.btinput.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

enum class StickCurve {
    LINEAR,
    AGGRESSIVE,
    EXPONENTIAL
}

/**
 * High-performance Analog Stick with deadzone + output curves (Step 7).
 */
@Composable
fun AnalogStick(
    size: Dp = 140.dp,
    knobSize: Dp = 52.dp,
    onValueChanged: (x: Int, y: Int) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF555555),
    knobColor: Color = Color(0xFFEEEEEE),
    alpha: Float = 1f,
    deadzone: Float = 0.12f,
    curve: StickCurve = StickCurve.AGGRESSIVE
) {
    var knobPosition by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    val outerRadius = size.toPx() / 2f
    val innerRadius = knobSize.toPx() / 2f
    val maxTravel = outerRadius - innerRadius - 6f

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(deadzone, curve) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()

                        if (change != null) {
                            if (change.pressed) {
                                isPressed = true
                                val center = Offset(outerRadius, outerRadius)
                                val touchPos = change.position

                                var dx = touchPos.x - center.x
                                var dy = touchPos.y - center.y
                                val distance = sqrt(dx * dx + dy * dy)

                                if (distance > maxTravel) {
                                    val scale = maxTravel / distance
                                    dx *= scale
                                    dy *= scale
                                }

                                knobPosition = Offset(dx, dy)

                                // === DEADZONE + CURVE (Step 7) ===
                                val rawX = (dx / maxTravel)
                                val rawY = (dy / maxTravel)

                                val outX = applyDeadzoneAndCurve(rawX, deadzone, curve)
                                val outY = applyDeadzoneAndCurve(rawY, deadzone, curve)

                                onValueChanged(
                                    (outX * 127).toInt().coerceIn(-127, 127),
                                    (outY * 127).toInt().coerceIn(-127, 127)
                                )
                            } else {
                                if (isPressed) {
                                    isPressed = false
                                    knobPosition = Offset.Zero
                                    onRelease()
                                }
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(outerRadius, outerRadius)

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = outerRadius - 2.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f * alpha),
                radius = outerRadius - 4.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Visual deadzone indicator
            if (deadzone > 0.01f) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f * alpha),
                    radius = outerRadius * deadzone,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            val knobCenter = center + knobPosition
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f * alpha),
                radius = innerRadius + 3.dp.toPx(),
                center = knobCenter.copy(y = knobCenter.y + 4.dp.toPx())
            )
            drawCircle(
                color = knobColor.copy(alpha = alpha),
                radius = innerRadius,
                center = knobCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.6f * alpha),
                radius = innerRadius * 0.65f,
                center = knobCenter.copy(y = knobCenter.y - innerRadius * 0.25f)
            )
        }
    }
}

private fun applyDeadzoneAndCurve(raw: Float, deadzone: Float, curve: StickCurve): Float {
    val absRaw = abs(raw)
    if (absRaw < deadzone) return 0f

    val normalized = (absRaw - deadzone) / (1f - deadzone)

    val curved = when (curve) {
        StickCurve.LINEAR -> normalized
        StickCurve.AGGRESSIVE -> normalized.pow(0.55f)
        StickCurve.EXPONENTIAL -> normalized.pow(1.8f)
    }

    return curved * sign(raw)
}