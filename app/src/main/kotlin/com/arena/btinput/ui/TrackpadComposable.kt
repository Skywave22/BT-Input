package com.arena.btinput.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * High-performance background Trackpad with **non-linear acceleration** (Step 7).
 * Slow movements = precise aiming. Fast flicks = large movement.
 */
@Composable
fun TrackpadComposable(
    onMouseMove: (dx: Int, dy: Int) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onScroll: (delta: Int) -> Unit,
    acceleration: Float = 1.8f,
    modifier: Modifier = Modifier
) {
    var lastPosition by remember { mutableStateOf<Offset?>(null) }
    var activePointerCount by remember { mutableIntStateOf(0) }
    var lastPointerCount by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var scrollAccum by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .pointerInput(acceleration) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    activePointerCount = 1
                    lastPosition = down.position
                    isDragging = false
                    scrollAccum = 0f

                    val downId = down.id

                    var event = awaitPointerEvent()
                    while (event.type != PointerEventType.Release) {
                        val changes = event.changes
                        activePointerCount = changes.count { it.pressed }

                        if (activePointerCount >= 2) {
                            val pointers = changes.filter { it.pressed }.map { it.position }
                            if (pointers.size >= 2) {
                                val avgY = pointers.map { it.y }.average().toFloat()
                                val dy = avgY - (lastPosition?.y ?: avgY)
                                scrollAccum += dy * 0.8f
                                val scrollDelta = (scrollAccum / 18f).toInt()
                                if (abs(scrollDelta) >= 1) {
                                    onScroll(scrollDelta.coerceIn(-20, 20))
                                    scrollAccum -= scrollDelta * 18f
                                }
                                lastPosition = Offset(pointers.map { it.x }.average().toFloat(), avgY)
                            }
                            isDragging = true
                        } else if (activePointerCount == 1) {
                            val change = changes.firstOrNull { it.id == downId && it.pressed }
                            if (change != null) {
                                val current = change.position
                                val prev = lastPosition ?: current

                                var dx = current.x - prev.x
                                var dy = current.y - prev.y

                                // === ACCELERATION CURVE ===
                                val speed = sqrt(dx * dx + dy * dy)
                                val factor = if (speed < 4f) 1f else (speed / 8f).pow(acceleration * 0.55f)

                                dx = (dx * factor).toInt().coerceIn(-120, 120)
                                dy = (dy * factor).toInt().coerceIn(-120, 120)

                                if (dx != 0 || dy != 0) {
                                    onMouseMove(dx, dy)
                                    isDragging = true
                                }
                                lastPosition = current
                            }
                        }

                        lastPointerCount = activePointerCount
                        event = awaitPointerEvent()
                    }

                    val upTime = System.currentTimeMillis()
                    val wasDragging = isDragging

                    if (!wasDragging && activePointerCount <= 1) {
                        val duration = upTime - (lastTapTime.takeIf { it > 0 } ?: (upTime - 300))
                        if (duration < 280) onLeftClick()
                    } else if (activePointerCount == 0 && lastPointerCount >= 2 && !wasDragging) {
                        onRightClick()
                    }

                    lastPosition = null
                    activePointerCount = 0
                    isDragging = false
                    scrollAccum = 0f
                    if (!wasDragging) lastTapTime = upTime
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { lastPosition = it; isDragging = true },
                    onDrag = { change, dragAmount ->
                        if (activePointerCount <= 1) {
                            var dx = dragAmount.x * 1.2f
                            var dy = dragAmount.y * 1.2f
                            val speed = sqrt(dx * dx + dy * dy)
                            val factor = if (speed < 4f) 1f else (speed / 8f).pow(acceleration * 0.55f)
                            dx = (dx * factor).toInt().coerceIn(-127, 127)
                            dy = (dy * factor).toInt().coerceIn(-127, 127)
                            if (dx != 0 || dy != 0) onMouseMove(dx, dy)
                            lastPosition = change.position
                        }
                    },
                    onDragEnd = { isDragging = false; lastPosition = null }
                )
            }
    ) {}
}