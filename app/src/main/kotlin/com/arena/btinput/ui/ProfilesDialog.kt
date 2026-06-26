package com.arena.btinput.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arena.btinput.data.ControlProfile
import com.arena.btinput.data.ProfileManager

@Composable
fun ProfilesDialog(
    profileManager: ProfileManager,
    currentControls: List<com.arena.btinput.data.EditableControl>,
    onLoad: (List<com.arena.btinput.data.EditableControl>) -> Unit,
    onDismiss: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Profiles") },
        text = {
            Column {
                if (profileManager.profiles.isEmpty()) {
                    Text("No saved profiles yet.")
                } else {
                    profileManager.profiles.forEach { profile ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(profile.name, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val loaded = profileManager.loadProfile(profile.name)
                                onLoad(loaded)
                                onDismiss()
                            }) { Text("Load") }
                            TextButton(onClick = {
                                profileManager.deleteProfile(profile.name)
                            }) { Text("Delete", color = androidx.compose.ui.graphics.Color.Red) }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(onClick = { showSaveDialog = true }) {
                    Text("Save Current as New Profile")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Profile") },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Profile Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newProfileName.isNotBlank()) {
                        profileManager.saveProfile(newProfileName, currentControls)
                        newProfileName = ""
                        showSaveDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}