package com.arena.btinput.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ControllerLayout {
    PPSSPP,
    GTA,
    FPS,
    CUSTOM
}

@Composable
fun PrebuiltLayout(
    layout: ControllerLayout,
    onLeftStick: (x: Int, y: Int) -> Unit,
    onLeftStickRelease: () -> Unit,
    onRightStick: (x: Int, y: Int) -> Unit,
    onRightStickRelease: () -> Unit,
    onDPad: (x: Int, y: Int) -> Unit,
    onDPadRelease: () -> Unit,
    onGamepadButtonPress: (String) -> Unit,
    onGamepadButtonRelease: (String) -> Unit,
    onKeyPress: (modifiers: Byte, keyCode: Byte) -> Unit,
    onKeyRelease: () -> Unit,
    onL2Changed: (Int) -> Unit = {},
    onR2Changed: (Int) -> Unit = {},
    onL2Release: () -> Unit = {},
    onR2Release: () -> Unit = {},
    alpha: Float = 1f,
    stickDeadzone: Float = 0.12f,
    stickCurve: StickCurve = StickCurve.AGGRESSIVE,
    modifier: Modifier = Modifier
) {
    when (layout) {
        ControllerLayout.PPSSPP -> PPSSPPStyleLayout(
            onLeftStick, onLeftStickRelease, onRightStick, onRightStickRelease,
            onDPad, onDPadRelease, onGamepadButtonPress, onGamepadButtonRelease, modifier
        )
        ControllerLayout.GTA -> GTAStyleLayout(
            onLeftStick, onLeftStickRelease, onRightStick, onRightStickRelease,
            onGamepadButtonPress, onGamepadButtonRelease, onKeyPress, onKeyRelease, modifier
        )
        ControllerLayout.FPS -> FPSStyleLayout(
            onLeftStick, onLeftStickRelease, onRightStick, onRightStickRelease,
            onKeyPress, onKeyRelease, modifier
        )
        ControllerLayout.CUSTOM -> {
            // Custom is handled separately in MainActivity via CustomEditor
            Box(modifier = modifier)
        }
    }
}

/* PPSSPP */
@Composable
private fun PPSSPPStyleLayout(
    onLeftStick: (x: Int, y: Int) -> Unit, onLeftStickRelease: () -> Unit,
    onRightStick: (x: Int, y: Int) -> Unit, onRightStickRelease: () -> Unit,
    onDPad: (x: Int, y: Int) -> Unit, onDPadRelease: () -> Unit,
    onButtonPress: (String) -> Unit, onButtonRelease: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 32.dp)) {
            DPad(size = 112.dp, onDirection = onDPad, onRelease = onDPadRelease)
            Spacer(Modifier.height(14.dp))
            AnalogStick(
                size = 122.dp,
                onValueChanged = onLeftStick,
                onRelease = onLeftStickRelease,
                color = Color(0xFF2A2A2A),
                alpha = alpha,
                deadzone = stickDeadzone,
                curve = stickCurve
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("△", 52.dp, Color(0xFF2196F3), onPress = { onButtonPress("TRIANGLE") }, onRelease = { onButtonRelease("TRIANGLE") }, alpha = alpha)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("□", 52.dp, Color(0xFF9C27B0), onPress = { onButtonPress("SQUARE") }, onRelease = { onButtonRelease("SQUARE") }, alpha = alpha)
                GameButton("○", 52.dp, Color(0xFFE53935), onPress = { onButtonPress("CIRCLE") }, onRelease = { onButtonRelease("CIRCLE") }, alpha = alpha)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("✕", 52.dp, Color(0xFF43A047), onPress = { onButtonPress("CROSS") }, onRelease = { onButtonRelease("CROSS") }, alpha = alpha)
            }
            Spacer(Modifier.height(18.dp))
            AnalogStick(size = 122.dp, onValueChanged = onRightStick, onRelease = onRightStickRelease, color = Color(0xFF2A2A2A))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, top = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            GameButton("L1", 46.dp, Color(0xFF546E7A), onPress = { onButtonPress("L1") }, onRelease = { onButtonRelease("L1") }, alpha = alpha)
            GameButton("R1", 46.dp, Color(0xFF546E7A), onPress = { onButtonPress("R1") }, onRelease = { onButtonRelease("R1") }, alpha = alpha)
        }

        // L2 / R2 Triggers (real analog)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, top = 110.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            L2R2Trigger("L2", width = 58.dp, height = 36.dp, onValueChanged = onL2Changed, onRelease = onL2Release, alpha = alpha)
            L2R2Trigger("R2", width = 58.dp, height = 36.dp, onValueChanged = onR2Changed, onRelease = onR2Release, alpha = alpha)
        }
    }
}

/* GTA */
@Composable
private fun GTAStyleLayout(
    onLeftStick: (x: Int, y: Int) -> Unit, onLeftStickRelease: () -> Unit,
    onRightStick: (x: Int, y: Int) -> Unit, onRightStickRelease: () -> Unit,
    onButtonPress: (String) -> Unit, onButtonRelease: (String) -> Unit,
    onKeyPress: (modifiers: Byte, keyCode: Byte) -> Unit, onKeyRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 28.dp)) {
            AnalogStick(size = 130.dp, onValueChanged = onLeftStick, onRelease = onLeftStickRelease, color = Color(0xFF1E3A5F))
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnalogStick(size = 118.dp, onValueChanged = onRightStick, onRelease = onRightStickRelease, color = Color(0xFF1E3A5F))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("A", 46.dp, Color(0xFF4CAF50), onPress = { onButtonPress("A") }, onRelease = { onButtonRelease("A") })
                GameButton("B", 46.dp, Color(0xFFE53935), onPress = { onButtonPress("B") }, onRelease = { onButtonRelease("B") })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("X", 46.dp, Color(0xFF2196F3), onPress = { onButtonPress("X") }, onRelease = { onButtonRelease("X") })
                GameButton("Y", 46.dp, Color(0xFFFFC107), onPress = { onButtonPress("Y") }, onRelease = { onButtonRelease("Y") })
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, top = 52.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            GameButton("L1", 42.dp, Color(0xFF455A64), onPress = { onButtonPress("L1") }, onRelease = { onButtonRelease("L1") })
            GameButton("R1", 42.dp, Color(0xFF455A64), onPress = { onButtonPress("R1") }, onRelease = { onButtonRelease("R1") })
        }
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(top = 110.dp, end = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameButton("E", 38.dp, Color(0xFF795548), onPress = { onKeyPress(0, 8) }, onRelease = onKeyRelease)
            Spacer(Modifier.height(6.dp))
            GameButton("W", 38.dp, Color(0xFF607D8B), onPress = { onKeyPress(0, 26) }, onRelease = onKeyRelease)
        }
    }
}

/* FPS */
@Composable
private fun FPSStyleLayout(
    onLeftStick: (x: Int, y: Int) -> Unit, onLeftStickRelease: () -> Unit,
    onRightStick: (x: Int, y: Int) -> Unit, onRightStickRelease: () -> Unit,
    onKeyPress: (modifiers: Byte, keyCode: Byte) -> Unit, onKeyRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameButton("W", 44.dp, Color(0xFF37474F), onPress = { onKeyPress(0, 26) }, onRelease = onKeyRelease)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GameButton("A", 44.dp, Color(0xFF37474F), onPress = { onKeyPress(0, 4) }, onRelease = onKeyRelease)
                GameButton("S", 44.dp, Color(0xFF37474F), onPress = { onKeyPress(0, 22) }, onRelease = onKeyRelease)
                GameButton("D", 44.dp, Color(0xFF37474F), onPress = { onKeyPress(0, 7) }, onRelease = onKeyRelease)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Shift", 38.dp, Color(0xFF455A64), onPress = { onKeyPress(0x02, 0) }, onRelease = onKeyRelease)
                GameButton("Spc", 38.dp, Color(0xFF455A64), onPress = { onKeyPress(0, 44) }, onRelease = onKeyRelease)
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 30.dp), horizontalAlignment = Alignment.End) {
            AnalogStick(size = 108.dp, onValueChanged = onRightStick, onRelease = onRightStickRelease, color = Color(0xFF263238))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GameButton("LMB", 40.dp, Color(0xFFD32F2F), onPress = { onKeyPress(0, 0xE0.toByte()) }, onRelease = onKeyRelease)
                GameButton("RMB", 40.dp, Color(0xFF388E3C), onPress = { onKeyPress(0, 0xE1.toByte()) }, onRelease = onKeyRelease)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GameButton("R", 36.dp, Color(0xFF5D4037), onPress = { onKeyPress(0, 21) }, onRelease = onKeyRelease)
                GameButton("F", 36.dp, Color(0xFF5D4037), onPress = { onKeyPress(0, 9) }, onRelease = onKeyRelease)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, top = 50.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            GameButton("Tab", 34.dp, Color(0xFF455A64), onPress = { onKeyPress(0, 43) }, onRelease = onKeyRelease)
            GameButton("Esc", 34.dp, Color(0xFF455A64), onPress = { onKeyPress(0, 41) }, onRelease = onKeyRelease)
        }
    }
}