package com.example.lightcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lightcontrolapp.data.models.Lamp
import com.example.lightcontrolapp.ui.components.DropdownMenuBox
import com.example.lightcontrolapp.viewmodel.AppViewModel
import com.example.lightcontrolapp.viewmodel.UiState

@Composable
fun ControlScreen(ui: UiState, vm: AppViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLamp by remember { mutableStateOf<Lamp?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Керування лампочками", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        // Випадаючий список лампочок
        Box {
            Button(onClick = { expanded = true }) {
                Text(
                    selectedLamp?.name?.takeIf { it.isNotBlank() }
                        ?: selectedLamp?.lamp_id
                        ?: "Виберіть лампочку"
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ui.lamps.forEach { lamp ->
                    DropdownMenuItem(
                        text = {
                            Text(lamp.name?.takeIf { it.isNotBlank() } ?: lamp.lamp_id)
                        },
                        onClick = {
                            selectedLamp = lamp
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Якщо вибрана лампочка — показуємо керування
        selectedLamp?.let { lamp ->
            val displayName = lamp.name?.takeIf { it.isNotBlank() } ?: lamp.lamp_id
            Text("Керування: $displayName", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(12.dp))

            // Перемикач стану
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Стан:")
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = lamp.state,
                    onCheckedChange = { vm.updateLamp(lamp.lamp_id, state = it) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Локальний стан для яскравості
            var localBrightness by remember(lamp.lamp_id) {
                mutableStateOf(lamp.brightness)
            }

            Text("Яскравість: $localBrightness%")

            Slider(
                value = localBrightness.toFloat(),
                onValueChange = { newValue ->
                    // одразу оновлюється локально
                    localBrightness = newValue.toInt()
                },
                valueRange = 0f..100f
            )

            Spacer(Modifier.height(8.dp))

            // Кнопка збереження налаштувань
            Button(onClick = {
                vm.updateLamp(lamp.lamp_id, brightness = localBrightness)
            }) {
                Text("Зберегти налаштування")
            }
        }
    }
}
