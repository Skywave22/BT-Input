package com.arena.btinput

import android.Manifest
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arena.btinput.data.*
import com.arena.btinput.service.HidBluetoothService
import com.arena.btinput.ui.*
import com.arena.btinput.utils.GyroManager
import com.arena.btinput.utils.MacroRecorder
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private var hidService: HidBluetoothService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            hidService = (service as HidBluetoothService.LocalBinder).getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
        }
    }

    private val requiredPermissions = mutableListOf<String>().apply {
        add(Manifest.permission.BLUETOOTH)
        add(Manifest.permission.BLUETOOTH_ADMIN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startAndBindHidService()
        else Toast.makeText(this, "Bluetooth permissions required.", Toast.LENGTH_LONG).show()
    }

    // State
    private var connectionState by mutableStateOf(HidBluetoothService.STATE_DISCONNECTED)
    private var connectedDeviceName by mutableStateOf<String?>(null)
    private var currentLayout by mutableStateOf(ControllerLayout.PPSSPP)
    private var isEditMode by mutableStateOf(false)
    private var customControls by mutableStateOf<List<EditableControl>>(emptyList())

    // Step 7 Tuning
    private var controlOpacity by mutableStateOf(1f)
    private var trackpadAcceleration by mutableStateOf(1.8f)
    private var stickDeadzone by mutableStateOf(0.12f)
    private var stickCurve by mutableStateOf(StickCurve.AGGRESSIVE)
    private var showInputSettings by mutableStateOf(false)

    // PiP
    private var isInPip by mutableStateOf(false)

    // Step 6
    private var gyroEnabled by mutableStateOf(false)
    private var gyroSensitivity by mutableStateOf(7f)
    private var isRecordingMacro by mutableStateOf(false)
    private var lastMacro by mutableStateOf<List<com.arena.btinput.utils.MacroAction>>(emptyList())

    private var currentGamepadButtons = 0
    private var leftTrigger = 0
    private var rightTrigger = 0

    private val prefs by lazy { getSharedPreferences("btinput_custom", Context.MODE_PRIVATE) }
    private lateinit var gyroManager: GyroManager
    private val macroRecorder = MacroRecorder()
    private lateinit var profileManager: ProfileManager

    private val gyroJob = Job()
    private val gyroScope = CoroutineScope(Dispatchers.Main + gyroJob)

    private var showProfilesDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gyroManager = GyroManager(this)
        profileManager = ProfileManager(this)
        loadCustomProfile()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen(
                        currentLayout = currentLayout,
                        onLayoutChange = { currentLayout = it },
                        isEditMode = isEditMode,
                        customControls = customControls,
                        onCustomControlsChange = { customControls = it },
                        connectionState = connectionState,
                        connectedDeviceName = connectedDeviceName,
                        onStartService = { checkAndRequestPermissions() },
                        onStopService = { stopHidService() },
                        onEnableBluetooth = { enableBluetooth() },
                        onEnterEditMode = { isEditMode = true },
                        onExitEditMode = {
                            isEditMode = false
                            saveCustomProfile(customControls)
                        },
                        controlOpacity = controlOpacity,
                        onOpacityChange = { controlOpacity = it },
                        onShowProfiles = { showProfilesDialog = true },
                        onShowInputSettings = { showInputSettings = true },
                        // Step 7 Tuning values
                        trackpadAcceleration = trackpadAcceleration,
                        stickDeadzone = stickDeadzone,
                        stickCurve = stickCurve,
                        // Trackpad
                        onMouseMove = { dx, dy -> sendMouseMove(dx, dy) },
                        onLeftClick = { performLeftClick() },
                        onRightClick = { performRightClick() },
                        onScroll = { delta -> sendScroll(delta) },
                        // Layouts
                        onLeftStick = { x, y -> sendLeftStick(x, y) },
                        onLeftStickRelease = { sendLeftStick(0, 0) },
                        onRightStick = { x, y -> sendRightStick(x, y) },
                        onRightStickRelease = { sendRightStick(0, 0) },
                        onDPad = { x, y -> sendDPad(x, y) },
                        onDPadRelease = { sendDPad(0, 0) },
                        onGamepadButtonPress = { btn -> pressGamepadButton(btn) },
                        onGamepadButtonRelease = { btn -> releaseGamepadButton(btn) },
                        onKeyPress = { mods, key -> sendKeyboard(mods, key) },
                        onKeyRelease = { releaseKeyboard() },
                        onCustomAction = { ctrl, isPress -> handleCustomAction(ctrl, isPress) },
                        onL2Changed = { v -> leftTrigger = v; sendTriggerUpdate() },
                        onR2Changed = { v -> rightTrigger = v; sendTriggerUpdate() },
                        onL2Release = { leftTrigger = 0; sendTriggerUpdate() },
                        onR2Release = { rightTrigger = 0; sendTriggerUpdate() },
                        // Step 6
                        gyroEnabled = gyroEnabled,
                        onGyroToggle = { enabled ->
                            gyroEnabled = enabled
                            gyroManager.setEnabled(enabled)
                            if (enabled) startGyroLoop()
                        },
                        gyroSensitivity = gyroSensitivity,
                        onGyroSensitivityChange = { gyroSensitivity = it },
                        isRecordingMacro = isRecordingMacro,
                        onToggleMacroRecord = { toggleMacroRecording() },
                        onPlayMacro = { playMacro() },
                        lastMacroSize = lastMacro.size,
                        // PiP
                        onEnterPip = { enterPipMode() },
                        isInPip = isInPip
                    )
                }
            }
        }

        if (hasAllPermissions()) startAndBindHidService()
    }

    private fun hasAllPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missing.isNotEmpty()) permissionLauncher.launch(missing) else startAndBindHidService()
    }

    private fun startAndBindHidService() {
        val intent = Intent(this, HidBluetoothService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopHidService() {
        if (isBound) unbindService(serviceConnection)
        stopService(Intent(this, HidBluetoothService::class.java))
        hidService = null
        isBound = false
        connectionState = HidBluetoothService.STATE_DISCONNECTED
    }

    private fun enableBluetooth() {
        val bt = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (bt != null && !bt.isEnabled) startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    // ==================== STEP 7: PiP ====================
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPip = isInPictureInPictureMode
        // The HID service keeps running because it is a foreground service
    }

    // ==================== HID REPORTS ====================

    private fun sendMouseMove(dx: Int, dy: Int) {
        hidService?.sendMouseReport(dx.coerceIn(-127, 127).toByte(), dy.coerceIn(-127, 127).toByte(), 0)
    }

    private fun performLeftClick() {
        val svc = hidService ?: return
        svc.sendMouseReport(0, 0, 0x01)
        android.os.Handler(mainLooper).postDelayed({ svc.sendMouseReport(0, 0, 0) }, 35)
    }

    private fun performRightClick() {
        val svc = hidService ?: return
        svc.sendMouseReport(0, 0, 0x02)
        android.os.Handler(mainLooper).postDelayed({ svc.sendMouseReport(0, 0, 0) }, 35)
    }

    private fun sendScroll(delta: Int) {
        hidService?.sendMouseReport(0, 0, 0, delta.coerceIn(-10, 10).toByte())
    }

    private fun sendLeftStick(x: Int, y: Int) {
        hidService?.sendGamepadReport(currentGamepadButtons, x.toByte(), y.toByte(), rightZ = leftTrigger.toByte(), rightRz = rightTrigger.toByte())
    }

    private fun sendRightStick(x: Int, y: Int) {
        hidService?.sendGamepadReport(currentGamepadButtons, 0, 0, x.toByte(), y.toByte(), 8, leftTrigger.toByte(), rightTrigger.toByte())
    }

    private fun sendDPad(x: Int, y: Int) {
        val hat = when {
            x == 1 && y == 0 -> 0; x == 1 && y == 1 -> 1; x == 0 && y == 1 -> 2; x == -1 && y == 1 -> 3
            x == -1 && y == 0 -> 4; x == -1 && y == -1 -> 5; x == 0 && y == -1 -> 6; x == 1 && y == -1 -> 7
            else -> 8
        }
        hidService?.sendGamepadReport(currentGamepadButtons, 0, 0, 0, 0, hat.toByte(), leftTrigger.toByte(), rightTrigger.toByte())
    }

    private fun sendTriggerUpdate() {
        hidService?.sendGamepadReport(currentGamepadButtons, 0, 0, 0, 0, 8, leftTrigger.toByte(), rightTrigger.toByte())
    }

    private fun pressGamepadButton(btn: String) {
        val bit = when (btn.uppercase()) {
            "CROSS", "A" -> 0x0001; "CIRCLE", "B" -> 0x0002; "SQUARE", "X" -> 0x0004; "TRIANGLE", "Y" -> 0x0008
            "L1" -> 0x0010; "R1" -> 0x0020; "L2" -> 0x0040; "R2" -> 0x0080
            else -> 0x0001
        }
        currentGamepadButtons = currentGamepadButtons or bit
        sendFullGamepadUpdate()
        vibrate()
    }

    private fun releaseGamepadButton(btn: String) {
        val bit = when (btn.uppercase()) {
            "CROSS", "A" -> 0x0001; "CIRCLE", "B" -> 0x0002; "SQUARE", "X" -> 0x0004; "TRIANGLE", "Y" -> 0x0008
            "L1" -> 0x0010; "R1" -> 0x0020; "L2" -> 0x0040; "R2" -> 0x0080
            else -> 0x0001
        }
        currentGamepadButtons = currentGamepadButtons and bit.inv()
        sendFullGamepadUpdate()
    }

    private fun sendFullGamepadUpdate() {
        hidService?.sendGamepadReport(currentGamepadButtons, 0, 0, 0, 0, 8, leftTrigger.toByte(), rightTrigger.toByte())
    }

    private fun sendKeyboard(modifiers: Byte, key: Byte) {
        hidService?.sendKeyboardReport(modifiers, key)
    }

    private fun releaseKeyboard() {
        hidService?.sendKeyboardReport(0)
    }

    // ==================== GYRO ====================

    private fun startGyroLoop() {
        gyroScope.launch {
            while (gyroEnabled) {
                val (dx, dy) = gyroManager.getMouseDeltas(gyroSensitivity)
                if (dx != 0 || dy != 0) {
                    sendMouseMove(dx, dy)
                }
                delay(16)
            }
        }
    }

    // ==================== MACROS ====================

    private fun toggleMacroRecording() {
        if (!isRecordingMacro) {
            macroRecorder.startRecording()
            isRecordingMacro = true
            Toast.makeText(this, "Macro recording started", Toast.LENGTH_SHORT).show()
        } else {
            lastMacro = macroRecorder.stopRecording()
            isRecordingMacro = false
            Toast.makeText(this, "Macro recorded (${lastMacro.size} actions)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMacro() {
        val macro = if (lastMacro.isNotEmpty()) lastMacro else macroRecorder.getLastMacro()
        if (macro.isEmpty()) {
            Toast.makeText(this, "No macro recorded", Toast.LENGTH_SHORT).show()
            return
        }
        hidService?.let {
            macroRecorder.play(it, macro)
            vibrate()
        }
    }

    // ==================== CUSTOM ACTIONS ====================

    private fun handleCustomAction(control: EditableControl, isPress: Boolean) {
        val action = control.action ?: return
        val svc = hidService ?: return

        if (isRecordingMacro) {
            when (action.type) {
                ActionType.KEYBOARD -> {
                    val key = parseKeyCode(action.value)
                    macroRecorder.recordKeyboard(action.modifiers, key)
                }
                ActionType.MOUSE -> {
                    val btn = when (action.value.uppercase()) {
                        "LEFT" -> 1; "RIGHT" -> 2; "MIDDLE" -> 4; else -> 1
                    }
                    macroRecorder.recordMouse(0, 0, btn.toByte())
                }
                ActionType.GAMEPAD -> {
                    val bit = when (action.value.uppercase()) {
                        "TRIANGLE", "Y" -> 0x0008; "CIRCLE", "B" -> 0x0002
                        "SQUARE", "X" -> 0x0004; "CROSS", "A" -> 0x0001
                        "L1" -> 0x0010; "R1" -> 0x0020; else -> 0x0001
                    }
                    macroRecorder.recordGamepad(bit)
                }
            }
        }

        when (action.type) {
            ActionType.KEYBOARD -> {
                val keyCode = parseKeyCode(action.value)
                if (isPress) svc.sendKeyboardReport(action.modifiers, keyCode)
                else svc.sendKeyboardReport(0)
            }
            ActionType.MOUSE -> {
                val button = when (action.value.uppercase()) {
                    "LEFT" -> 0x01; "RIGHT" -> 0x02; "MIDDLE" -> 0x04; else -> 0x01
                }
                if (isPress) svc.sendMouseReport(0, 0, button.toByte())
                else svc.sendMouseReport(0, 0, 0)
            }
            ActionType.GAMEPAD -> {
                val bit = when (action.value.uppercase()) {
                    "TRIANGLE", "Y" -> 0x0008; "CIRCLE", "B" -> 0x0002
                    "SQUARE", "X" -> 0x0004; "CROSS", "A" -> 0x0001
                    "L1" -> 0x0010; "R1" -> 0x0020; "L2" -> 0x0040; "R2" -> 0x0080
                    else -> 0x0001
                }
                if (isPress) currentGamepadButtons = currentGamepadButtons or bit
                else currentGamepadButtons = currentGamepadButtons and bit.inv()
                svc.sendGamepadReport(currentGamepadButtons, 0, 0, 0, 0, 8, leftTrigger.toByte(), rightTrigger.toByte())
            }
        }
        if (isPress) vibrate()
    }

    private fun parseKeyCode(name: String): Byte {
        return when (name.uppercase()) {
            "W" -> 26; "A" -> 4; "S" -> 22; "D" -> 7
            "SPACE", "SPC" -> 44; "SHIFT" -> 0x02; "TAB" -> 43; "ESC" -> 41
            "R" -> 21; "F" -> 9; "E" -> 8; "LEFT" -> 0x50.toByte(); "RIGHT" -> 0x4F.toByte()
            "ENTER" -> 40; "BACK" -> 42; "HOME" -> 0x02
            else -> 0
        }.toByte()
    }

    private fun vibrate() {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vib.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    // ==================== CUSTOM PROFILE ====================

    private fun saveCustomProfile(controls: List<EditableControl>) {
        val json = controls.joinToString("|||") { it.toJson() }
        prefs.edit().putString("custom_profile", json).apply()
    }

    private fun loadCustomProfile() {
        val json = prefs.getString("custom_profile", "") ?: ""
        if (json.isNotBlank()) customControls = parseControls(json)
    }

    override fun onDestroy() {
        super.onDestroy()
        gyroManager.setEnabled(false)
        gyroJob.cancel()
        if (isBound) unbindService(serviceConnection)
    }
}

// ==================== UI ====================

@Composable
fun MainAppScreen(
    currentLayout: ControllerLayout,
    onLayoutChange: (ControllerLayout) -> Unit,
    isEditMode: Boolean,
    customControls: List<EditableControl>,
    onCustomControlsChange: (List<EditableControl>) -> Unit,
    connectionState: Int,
    connectedDeviceName: String?,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    controlOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    onShowProfiles: () -> Unit,
    onShowInputSettings: () -> Unit,
    trackpadAcceleration: Float,
    stickDeadzone: Float,
    stickCurve: StickCurve,
    // Trackpad
    onMouseMove: (dx: Int, dy: Int) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onScroll: (delta: Int) -> Unit,
    // Layouts
    onLeftStick: (x: Int, y: Int) -> Unit,
    onLeftStickRelease: () -> Unit,
    onRightStick: (x: Int, y: Int) -> Unit,
    onRightStickRelease: () -> Unit,
    onDPad: (x: Int, y: Int) -> Unit,
    onDPadRelease: () -> Unit,
    onGamepadButtonPress: (String) -> Unit,
    onGamepadButtonRelease: (String) -> Unit,
    onKeyPress: (modifiers: Byte, key: Byte) -> Unit,
    onKeyRelease: () -> Unit,
    onCustomAction: (EditableControl, Boolean) -> Unit,
    onL2Changed: (Int) -> Unit,
    onR2Changed: (Int) -> Unit,
    onL2Release: () -> Unit,
    onR2Release: () -> Unit,
    gyroEnabled: Boolean,
    onGyroToggle: (Boolean) -> Unit,
    gyroSensitivity: Float,
    onGyroSensitivityChange: (Float) -> Unit,
    isRecordingMacro: Boolean,
    onToggleMacroRecord: () -> Unit,
    onPlayMacro: () -> Unit,
    lastMacroSize: Int,
    // PiP
    onEnterPip: () -> Unit,
    isInPip: Boolean
) {
    val isConnected = connectionState == HidBluetoothService.STATE_CONNECTED

    Box(modifier = Modifier.fillMaxSize()) {

        TrackpadComposable(
            onMouseMove = onMouseMove,
            onLeftClick = onLeftClick,
            onRightClick = onRightClick,
            onScroll = onScroll,
            acceleration = trackpadAcceleration,
            modifier = Modifier.fillMaxSize()
        )

        when {
            currentLayout == ControllerLayout.CUSTOM && !isEditMode -> {
                CustomEditor(
                    controls = customControls,
                    isEditMode = false,
                    onControlsChanged = onCustomControlsChange,
                    onActionTriggered = onCustomAction,
                    onExitEditor = {},
                    alpha = controlOpacity
                )
            }
            currentLayout == ControllerLayout.CUSTOM && isEditMode -> {
                CustomEditor(
                    controls = customControls,
                    isEditMode = true,
                    onControlsChanged = onCustomControlsChange,
                    onActionTriggered = onCustomAction,
                    onExitEditor = onExitEditMode,
                    alpha = controlOpacity
                )
            }
            else -> {
                PrebuiltLayout(
                    layout = currentLayout,
                    onLeftStick = onLeftStick,
                    onLeftStickRelease = onLeftStickRelease,
                    onRightStick = onRightStick,
                    onRightStickRelease = onRightStickRelease,
                    onDPad = onDPad,
                    onDPadRelease = onDPadRelease,
                    onGamepadButtonPress = onGamepadButtonPress,
                    onGamepadButtonRelease = onGamepadButtonRelease,
                    onKeyPress = onKeyPress,
                    onKeyRelease = onKeyRelease,
                    onL2Changed = onL2Changed,
                    onR2Changed = onR2Changed,
                    onL2Release = onL2Release,
                    onR2Release = onR2Release,
                    modifier = Modifier.fillMaxSize(),
                    alpha = controlOpacity,
                    stickDeadzone = stickDeadzone,
                    stickCurve = stickCurve
                )
            }
        }

        // TOP BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BT Input — Pro Gamer", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (isConnected) "CONNECTED • ${connectedDeviceName ?: "PC"}" else "DISCONNECTED — Pair on PC",
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFFC107),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onEnterPip, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("PiP", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                    if (!isConnected) {
                        Button(onClick = onStartService, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("START", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    } else {
                        OutlinedButton(onClick = onStopService, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("STOP", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    }
                    TextButton(onClick = onEnableBluetooth) {
                        Text("BT", color = Color.White, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LayoutChip("PPSSPP", currentLayout == ControllerLayout.PPSSPP) { onLayoutChange(ControllerLayout.PPSSPP) }
                LayoutChip("GTA", currentLayout == ControllerLayout.GTA) { onLayoutChange(ControllerLayout.GTA) }
                LayoutChip("FPS", currentLayout == ControllerLayout.FPS) { onLayoutChange(ControllerLayout.FPS) }
                LayoutChip("CUSTOM", currentLayout == ControllerLayout.CUSTOM) { onLayoutChange(ControllerLayout.CUSTOM) }

                Spacer(Modifier.weight(1f))

                if (currentLayout == ControllerLayout.CUSTOM) {
                    if (isEditMode) {
                        Button(onClick = onExitEditMode, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                            Text("SAVE", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    } else {
                        OutlinedButton(onClick = onEnterEditMode) { Text("EDIT", fontSize = MaterialTheme.typography.labelSmall.fontSize) }
                        OutlinedButton(onClick = onShowProfiles) { Text("PROFILES", fontSize = MaterialTheme.typography.labelSmall.fontSize) }
                    }
                }

                OutlinedButton(onClick = onShowInputSettings) {
                    Text("TUNING", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }

            // Step 6 + 7 row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opacity", color = Color.White, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                Slider(value = controlOpacity, onValueChange = onOpacityChange, valueRange = 0.3f..1f, modifier = Modifier.width(70.dp).height(18.dp))

                Spacer(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gyro", color = Color.White, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    Switch(checked = gyroEnabled, onCheckedChange = onGyroToggle, modifier = Modifier.scale(0.7f))
                }

                Button(
                    onClick = onToggleMacroRecord,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecordingMacro) Color(0xFFD32F2F) else Color(0xFF455A64)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text(if (isRecordingMacro) "■ STOP" else "● REC", fontSize = MaterialTheme.typography.labelSmall.fontSize) }

                if (lastMacroSize > 0 || isRecordingMacro) {
                    Button(onClick = onPlayMacro, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("▶ PLAY", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                }
            }
        }

        if (showInputSettings) {
            InputSettingsPanel(
                trackpadAcceleration = trackpadAcceleration,
                onTrackpadAccelerationChange = { trackpadAcceleration = it },
                stickDeadzone = stickDeadzone,
                onStickDeadzoneChange = { stickDeadzone = it },
                stickCurve = stickCurve,
                onStickCurveChange = { stickCurve = it },
                onClose = { showInputSettings = false },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp)
            )
        }

        if (!isConnected && !isInPip) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 140.dp), contentAlignment = Alignment.Center) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xDD1F1F1F))) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Real HID + Acceleration + Deadzones + PiP", color = Color.White)
                        Text("Pair 'BT Input Controller' on PC", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showProfilesDialog) {
        ProfilesDialog(
            profileManager = ProfileManager(LocalContext.current),
            currentControls = customControls,
            onLoad = { loaded -> onCustomControlsChange(loaded) },
            onDismiss = { showProfilesDialog = false }
        )
    }
}

@Composable
private fun LayoutChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, color = if (selected) Color.Black else Color.White, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4CAF50),
            containerColor = Color(0xFF333333)
        ),
        modifier = Modifier.height(26.dp)
    )
}