package com.arena.btinput.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ControlType {
    ROUND_BUTTON,
    RECT_BUTTON,
    ANALOG_STICK,
    DPAD,
    SLIDER
}

enum class ActionType {
    KEYBOARD,
    MOUSE,
    GAMEPAD
}

data class ControlAction(
    val type: ActionType,
    val value: String,           // e.g. "W", "LEFT", "BUTTON_1", "0x1F"
    val modifiers: Byte = 0      // for keyboard
)

data class EditableControl(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ControlType,
    var x: Float,                // position in dp (will convert)
    var y: Float,
    var width: Float = 72f,
    var height: Float = 72f,
    var label: String = "",
    var action: ControlAction? = null
) {
    fun toDisplayLabel(): String {
        return when {
            label.isNotBlank() -> label
            action != null -> action!!.value
            else -> type.name.take(3)
        }
    }
}

// Simple serialization helpers for save/load
fun EditableControl.toJson(): String {
    return """{"id":"$id","type":"${type.name}","x":$x,"y":$y,"w":$width,"h":$height,"label":"$label","actionType":"${action?.type?.name ?: ""}","actionValue":"${action?.value ?: ""}"}"""
}

fun parseControls(jsonList: String): List<EditableControl> {
    // Very simple parser for demo (in real app use Gson/Moshi)
    if (jsonList.isBlank()) return emptyList()
    return try {
        jsonList.split("|||").mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val map = line.trim('{', '}').split(",").associate { 
                val p = it.split(":", limit = 2)
                p[0].trim('"') to p.getOrNull(1)?.trim('"') ?: ""
            }
            EditableControl(
                id = map["id"] ?: java.util.UUID.randomUUID().toString(),
                type = ControlType.valueOf(map["type"] ?: "ROUND_BUTTON"),
                x = map["x"]?.toFloatOrNull() ?: 100f,
                y = map["y"]?.toFloatOrNull() ?: 400f,
                width = map["w"]?.toFloatOrNull() ?: 72f,
                height = map["h"]?.toFloatOrNull() ?: 72f,
                label = map["label"] ?: "",
                action = if (map["actionType"]?.isNotBlank() == true) {
                    ControlAction(
                        type = ActionType.valueOf(map["actionType"]!!),
                        value = map["actionValue"] ?: ""
                    )
                } else null
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}