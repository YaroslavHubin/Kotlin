package com.example.lightcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lightcontrolapp.viewmodel.AppViewModel
import com.example.lightcontrolapp.viewmodel.UiState

@Composable
fun LampsScreen(ui: UiState, vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ваші лампочки", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { vm.refreshLamps() }) { Text("Оновити список") }
            Button(onClick = { vm.addLamp() }) { Text("Додати лампочку") }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(ui.lamps, key = { it.lamp_id }) { lamp ->
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        // ID
                        Text("ID: ${lamp.lamp_id}")

                        // Toggle state
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Button(onClick = { vm.updateLamp(lamp.lamp_id, state = !lamp.state) }) {
                                Text(if (lamp.state) "Вимкнути" else "Увімкнути")
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        // Name field
                        Spacer(Modifier.height(8.dp))
                        var name by remember(lamp.lamp_id) { mutableStateOf(lamp.name ?: "") }
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                if (it.length <= 20) name = it   // обмеження 20 символів
                            },
                            label = { Text("Назва лампочки") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { vm.updateLamp(lamp.lamp_id, name = name.trim()) }) {
                            Text("Зберегти назву")
                        }

                        // Info
                        Spacer(Modifier.height(10.dp))
                        val formattedPower = "%.3f".format(lamp.power_w)
                        val formattedEnergy = "%.3f".format(lamp.energy_kwh)
                        Text("Стан: ${if (lamp.state) "увімк" else "вимк"} | Яскравість: ${lamp.brightness}%")
                        Text("Потужність: $formattedPower Вт | Енергія: $formattedEnergy кВт⋅год")

                        // Brightness +/- buttons
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Button(onClick = {
                                val newB = (lamp.brightness + 10).coerceAtMost(100)
                                vm.updateLamp(lamp.lamp_id, brightness = newB)
                            }) { Text("Яскравість +10") }

                            Spacer(Modifier.width(8.dp))

                            Button(onClick = {
                                val newB = (lamp.brightness - 10).coerceAtLeast(0)
                                vm.updateLamp(lamp.lamp_id, brightness = newB)
                            }) { Text("Яскравість -10") }
                        }

                        // Power field
                        Spacer(Modifier.height(10.dp))
                        Text("Потужність (Вт):")
                        var power by remember(lamp.lamp_id) { mutableStateOf(lamp.power_w.toString()) }
                        OutlinedTextField(
                            value = power,
                            onValueChange = {
                                if (it.length <= 7 && it.all { ch -> ch.isDigit() || ch == '.' }) {
                                    power = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = {
                            power.toDoubleOrNull()?.let { newPower ->
                                if (newPower <= 9999999) {
                                    vm.updateLamp(lamp.lamp_id, power_w = newPower)
                                }
                            }
                        }) {
                            Text("Зберегти потужність")
                        }

                        // Delete button
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.deleteLamp(lamp.lamp_id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                        ) {
                            Text("Видалити лампочку")
                        }
                    }
                }
            }
        }
    }
}
