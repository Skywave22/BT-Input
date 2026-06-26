package com.arena.btinput.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Overlay controls that sit on top of the background Trackpad.
 * Touches on these controls are fully consumed (no leak to trackpad).
 *
 * This is a simple PPSSPP-style layout for Step 3.
 */
@Composable
fun ControllerOverlay(
    onLeftStick: (x: Int, y: Int) -> Unit,
    onLeftStickRelease: () -> Unit,
    onRightStick: (x: Int, y: Int) -> Unit,
    onRightStickRelease: () -> Unit,
    onDPad: (x: Int, y: Int) -> Unit,
    onDPadRelease: () -> Unit,
    onButtonPress: (button: String) -> Unit,
    onButtonRelease: (button: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // === LEFT SIDE: D-Pad + Left Analog ===
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // D-Pad
            DPad(
                size = 118.dp,
                onDirection = onDPad,
                onRelease = onDPadRelease
            )

            Spacer(Modifier.height(18.dp))

            // Left Analog Stick
            AnalogStick(
                size = 128.dp,
                onValueChanged = onLeftStick,
                onRelease = onLeftStickRelease,
                color = Color(0xFF2F2F2F),
                knobColor = Color(0xFFCCCCCC)
            )
        }

        // === RIGHT SIDE: Action buttons + Right Analog ===
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action buttons (PPSSPP style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Triangle (top)
                GameButton(
                    label = "△",
                    size = 58.dp,
                    color = Color(0xFF4A90E2),
                    onPress = { onButtonPress("TRIANGLE") },
                    onRelease = { onButtonRelease("TRIANGLE") }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Square (left)
                GameButton(
                    label = "□",
                    size = 58.dp,
                    color = Color(0xFF9C27B0),
                    onPress = { onButtonPress("SQUARE") },
                    onRelease = { onButtonRelease("SQUARE") }
                )

                // Circle (right)
                GameButton(
                    label = "○",
                    size = 58.dp,
                    color = Color(0xFFE53935),
                    onPress = { onButtonPress("CIRCLE") },
                    onRelease = { onButtonRelease("CIRCLE") }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cross (bottom)
                GameButton(
                    label = "✕",
                    size = 58.dp,
                    color = Color(0xFF43A047),
                    onPress = { onButtonPress("CROSS") },
                    onRelease = { onButtonRelease("CROSS") }
                )
            }

            Spacer(Modifier.height(22.dp))

            // Right Analog Stick
            AnalogStick(
                size = 128.dp,
                onValueChanged = onRightStick,
                onRelease = onRightStickRelease,
                color = Color(0xFF2F2F2F),
                knobColor = Color(0xFFCCCCCC)
            )
        }

        // === SHOULDER BUTTONS (Top) ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, top = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GameButton(
                label = "L1",
                size = 52.dp,
                color = Color(0xFF607D8B),
                onPress = { onButtonPress("L1") },
                onRelease = { onButtonRelease("L1") }
            )

            GameButton(
                label = "R1",
                size = 52.dp,
                color = Color(0xFF607D8B),
                onPress = { onButtonPress("R1") },
                onRelease = { onButtonRelease("R1") }
            )
        }
    }
}