package com.arena.btinput.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arena.btinput.data.*

/**
 * Custom Layout Editor + Player
 * - Toggle Edit Mode
 * - Drag + Resize controls
 * - Add new controls
 * - Map actions via dialog
 */
@Composable
fun CustomEditor(
    controls: List<EditableControl>,
    isEditMode: Boolean,
    onControlsChanged: (List<EditableControl>) -> Unit,
    onActionTriggered: (control: EditableControl, isPress: Boolean) -> Unit,
    onExitEditor: () -> Unit,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingControl by remember { mutableStateOf<EditableControl?>(null) }

    Box(modifier = modifier.fillMaxSize()) {

        // Render all controls
        controls.forEach { control ->
            DraggableControl(
                control = control,
                isEditMode = isEditMode,
                onUpdate = { updated ->
                    val newList = controls.map { if (it.id == updated.id) updated else it }
                    onControlsChanged(newList)
                }
            ) { ctrl, isPressed ->
                RenderControl(
                    control = ctrl,
                    isPressed = isPressed,
                    isEditMode = isEditMode,
                    alpha = alpha,
                    onClick = {
                        if (!isEditMode) {
                            onActionTriggered(ctrl, true)
                        } else {
                            editingControl = ctrl
                        }
                    },
                    onRelease = {
                        if (!isEditMode) onActionTriggered(ctrl, false)
                    }
                )
            }
        }

        // Editor UI
        if (isEditMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .background(Color(0xAA000000))
                    .padding(8.dp)
            ) {
                Text("EDIT MODE — Drag & Resize", color = Color.Cyan, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Control")
                    }
                    Button(onClick = onExitEditor) {
                        Text("Done")
                    }
                }
            }
        } else {
            // Play mode hint
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .background(Color(0xAA222222))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Custom Layout — Background is Trackpad", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Add Control Dialog
        if (showAddDialog) {
            AddControlDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { type ->
                    val newControl = EditableControl(
                        type = type,
                        x = 120f,
                        y = 300f,
                        label = when (type) {
                            ControlType.ANALOG_STICK -> "Stick"
                            ControlType.DPAD -> "DPad"
                            else -> "Btn"
                        }
                    )
                    onControlsChanged(controls + newControl)
                    showAddDialog = false
                }
            )
        }

        // Mapping Dialog
        editingControl?.let { ctrl ->
            MappingDialog(
                control = ctrl,
                onDismiss = { editingControl = null },
                onSave = { newAction ->
                    val updated = ctrl.copy(action = newAction)
                    val newList = controls.map { if (it.id == ctrl.id) updated else it }
                    onControlsChanged(newList)
                    editingControl = null
                },
                onDelete = {
                    val newList = controls.filter { it.id != ctrl.id }
                    onControlsChanged(newList)
                    editingControl = null
                }
            )
        }
    }
}

@Composable
private fun RenderControl(
    control: EditableControl,
    isPressed: Boolean,
    isEditMode: Boolean,
    alpha: Float = 1f,
    onClick: () -> Unit,
    onRelease: () -> Unit
) {
    when (control.type) {
        ControlType.ROUND_BUTTON, ControlType.RECT_BUTTON -> {
            val shape = if (control.type == ControlType.ROUND_BUTTON) CircleShape else RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isPressed) Color(0xFF1565C0) else Color(0xFF1976D2),
                        shape
                    )
                    .clickable {
                        onClick()
                        // Auto release for buttons
                        if (!isEditMode) {
                            kotlinx.coroutines.GlobalScope.launch {
                                kotlinx.coroutines.delay(80)
                                onRelease()
                            }
                        }
                    }
                    .border(1.dp, Color.White.copy(0.6f), shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    control.toDisplayLabel(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        ControlType.ANALOG_STICK -> {
            AnalogStick(
                size = (control.width).dp,
                onValueChanged = { x, y ->
                    // In play mode this would send stick values
                },
                onRelease = onRelease,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlType.DPAD -> {
            DPad(
                size = (control.width).dp,
                onDirection = { _, _ -> },
                onRelease = onRelease,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlType.SLIDER -> {
            // Simple visual slider
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF424242), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(control.toDisplayLabel(), color = Color.White)
            }
        }
    }
}

@Composable
private fun AddControlDialog(
    onDismiss: () -> Unit,
    onAdd: (ControlType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Control") },
        text = {
            Column {
                ControlType.values().forEach { type ->
                    TextButton(
                        onClick = { onAdd(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(type.name.replace("_", " "))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MappingDialog(
    control: EditableControl,
    onDismiss: () -> Unit,
    onSave: (ControlAction) -> Unit,
    onDelete: () -> Unit
) {
    var selectedType by remember { mutableStateOf(control.action?.type ?: ActionType.GAMEPAD) }
    var value by remember { mutableStateOf(control.action?.value ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map ${control.type.name}") },
        text = {
            Column {
                Text("Action Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value (e.g. W, LEFT, 1, TRIANGLE)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "Examples:\nKeyboard: W, SPACE, LEFT_SHIFT\nMouse: LEFT, RIGHT, MIDDLE\nGamepad: TRIANGLE, L1, BUTTON_5",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Delete")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (value.isNotBlank()) {
                        onSave(ControlAction(selectedType, value))
                    } else {
                        onDismiss()
                    }
                }) {
                    Text("Save Mapping")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}