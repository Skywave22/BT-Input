package com.arena.btinput.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class ControlProfile(
    val name: String,
    val controlsJson: String
)

class ProfileManager(context: Context) {

    private val prefs = context.getSharedPreferences("btinput_profiles", Context.MODE_PRIVATE)
    val profiles: SnapshotStateList<ControlProfile> = mutableStateListOf()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        profiles.clear()
        val names = prefs.getStringSet("profile_names", emptySet()) ?: emptySet()
        names.forEach { name ->
            val json = prefs.getString("profile_$name", "") ?: ""
            if (json.isNotBlank()) {
                profiles.add(ControlProfile(name, json))
            }
        }
    }

    fun saveProfile(name: String, controls: List<EditableControl>) {
        val json = controls.joinToString("|||") { it.toJson() }
        val editor = prefs.edit()
        editor.putString("profile_$name", json)

        val currentNames = prefs.getStringSet("profile_names", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentNames.add(name)
        editor.putStringSet("profile_names", currentNames)
        editor.apply()

        // Refresh list
        loadProfiles()
    }

    fun loadProfile(name: String): List<EditableControl> {
        val json = prefs.getString("profile_$name", "") ?: ""
        return parseControls(json)
    }

    fun deleteProfile(name: String) {
        val editor = prefs.edit()
        editor.remove("profile_$name")
        val currentNames = prefs.getStringSet("profile_names", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentNames.remove(name)
        editor.putStringSet("profile_names", currentNames)
        editor.apply()
        loadProfiles()
    }
}