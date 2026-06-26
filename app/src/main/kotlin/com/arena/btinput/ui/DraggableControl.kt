package com.arena.btinput.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arena.btinput.data.ControlType
import com.arena.btinput.data.EditableControl

/**
 * Draggable + Resizable wrapper for editor mode.
 * In play mode, just renders the control without editing handles.
 */
@Composable
fun DraggableControl(
    control: EditableControl,
    isEditMode: Boolean,
    onUpdate: (EditableControl) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (control: EditableControl, isPressed: Boolean) -> Unit
) {
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }

    var currentX by remember { mutableStateOf(control.x) }
    var currentY by remember { mutableStateOf(control.y) }
    var currentW by remember { mutableStateOf(control.width) }
    var currentH by remember { mutableStateOf(control.height) }

    // Sync external changes
    LaunchedEffect(control.x, control.y, control.width, control.height) {
        currentX = control.x
        currentY = control.y
        currentW = control.width
        currentH = control.height
    }

    Box(
        modifier = modifier
            .offset(x = currentX.dp, y = currentY.dp)
            .size(width = currentW.dp, height = currentH.dp)
            .then(
                if (isEditMode) {
                    Modifier
                        .border(2.dp, Color(0xFF00BCD4), RoundedCornerShape(6.dp))
                        .background(Color(0x2200BCD4))
                } else Modifier
            )
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    // Normal play mode - just detect taps for functionality
                    detectDragGestures(
                        onDragStart = { isPressed = true },
                        onDrag = { _, _ -> },
                        onDragEnd = { isPressed = false }
                    )
                    return@pointerInput
                }

                // Edit mode drag (move)
                detectDragGestures { change, dragAmount ->
                    val dx = dragAmount.x / density.density
                    val dy = dragAmount.y / density.density
                    currentX = (currentX + dx).coerceIn(0f, 320f)
                    currentY = (currentY + dy).coerceIn(60f, 580f)

                    onUpdate(control.copy(x = currentX, y = currentY))
                    change.consume()
                }
            }
    ) {
        // Render the actual control appearance
        content(control, isPressed)

        // Resize handle (only in edit mode)
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .background(Color(0xFF00BCD4))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            val dw = dragAmount.x / density.density
                            val dh = dragAmount.y / density.density

                            currentW = (currentW + dw).coerceIn(40f, 160f)
                            currentH = (currentH + dh).coerceIn(40f, 160f)

                            onUpdate(
                                control.copy(
                                    x = currentX,
                                    y = currentY,
                                    width = currentW,
                                    height = currentH
                                )
                            )
                            change.consume()
                        }
                    }
            ) {
                Text("↘", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}