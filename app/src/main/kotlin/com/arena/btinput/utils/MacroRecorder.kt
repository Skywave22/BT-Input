package com.arena.btinput.utils

import com.arena.btinput.service.HidBluetoothService
import kotlinx.coroutines.*

data class MacroAction(
    val delayMs: Long,
    val type: String,
    val data: String
)

class MacroRecorder {

    private val recordedActions = mutableListOf<MacroAction>()
    private var isRecording = false
    private var recordStartTime = 0L

    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startRecording() {
        recordedActions.clear()
        isRecording = true
        recordStartTime = System.currentTimeMillis()
    }

    fun stopRecording(): List<MacroAction> {
        isRecording = false
        return recordedActions.toList()
    }

    fun isRecording(): Boolean = isRecording

    fun recordMouse(dx: Int, dy: Int, buttons: Byte) {
        if (!isRecording) return
        val delay = System.currentTimeMillis() - recordStartTime
        recordedActions.add(MacroAction(delay, "MOUSE", "$dx,$dy,$buttons"))
    }

    fun recordKeyboard(modifiers: Byte, key: Byte) {
        if (!isRecording) return
        val delay = System.currentTimeMillis() - recordStartTime
        recordedActions.add(MacroAction(delay, "KEY", "$modifiers,$key"))
    }

    fun recordGamepad(buttons: Int) {
        if (!isRecording) return
        val delay = System.currentTimeMillis() - recordStartTime
        recordedActions.add(MacroAction(delay, "GAMEPAD", "$buttons"))
    }

    fun play(service: HidBluetoothService?, macro: List<MacroAction>) {
        if (service == null || macro.isEmpty()) return

        playbackJob?.cancel()
        playbackJob = scope.launch {
            var lastTime = 0L
            for (action in macro) {
                val wait = (action.delayMs - lastTime).coerceAtLeast(0)
                if (wait > 0) delay(wait)

                when (action.type) {
                    "MOUSE" -> {
                        val p = action.data.split(",")
                        service.sendMouseReport(
                            p.getOrNull(0)?.toByteOrNull() ?: 0,
                            p.getOrNull(1)?.toByteOrNull() ?: 0,
                            p.getOrNull(2)?.toByteOrNull() ?: 0
                        )
                    }
                    "KEY" -> {
                        val p = action.data.split(",")
                        service.sendKeyboardReport(
                            p.getOrNull(0)?.toByteOrNull() ?: 0,
                            p.getOrNull(1)?.toByteOrNull() ?: 0
                        )
                    }
                    "GAMEPAD" -> {
                        val btns = action.data.toIntOrNull() ?: 0
                        service.sendGamepadReport(btns, 0, 0, 0, 0, 8)
                    }
                }
                lastTime = action.delayMs
            }
            service.sendReleaseAll()
        }
    }

    fun cancelPlayback() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun getLastMacro(): List<MacroAction> = recordedActions.toList()
}