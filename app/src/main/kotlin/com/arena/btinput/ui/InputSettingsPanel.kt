package com.arena.btinput.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InputSettingsPanel(
    trackpadAcceleration: Float,
    onTrackpadAccelerationChange: (Float) -> Unit,
    stickDeadzone: Float,
    onStickDeadzoneChange: (Float) -> Unit,
    stickCurve: StickCurve,
    onStickCurveChange: (StickCurve) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE222222))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Input Tuning", style = MaterialTheme.typography.titleMedium, color = Color.White)

            Spacer(Modifier.height(12.dp))

            Text("Trackpad Acceleration: ${"%.1f".format(trackpadAcceleration)}", color = Color.White)
            Slider(
                value = trackpadAcceleration,
                onValueChange = onTrackpadAccelerationChange,
                valueRange = 1f..3.5f,
                steps = 5
            )
            Text("1.0 = Linear (precise)  •  2.0+ = Strong flick acceleration", color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(16.dp))

            Text("Analog Deadzone: ${"%.0f".format(stickDeadzone * 100)}%", color = Color.White)
            Slider(
                value = stickDeadzone,
                onValueChange = onStickDeadzoneChange,
                valueRange = 0f..0.35f
            )

            Spacer(Modifier.height(12.dp))

            Text("Analog Output Curve", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StickCurve.values().forEach { c ->
                    FilterChip(
                        selected = stickCurve == c,
                        onClick = { onStickCurveChange(c) },
                        label = { Text(c.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close Settings")
            }
        }
    }
}