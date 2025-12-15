package com.example.lightcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lightcontrolapp.viewmodel.AppViewModel
import com.example.lightcontrolapp.viewmodel.UiState
import kotlinx.coroutines.delay

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun MonitorScreen(ui: UiState, vm: AppViewModel) {
    var auto by remember { mutableStateOf(false) }
    var selectedLampId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(auto) {
        if (auto) {
            while (true) {
                vm.refreshLamps()
                delay(2000)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Моніторинг у реальному часі", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Автооновлення")
            Spacer(Modifier.width(12.dp))
            Switch(checked = auto, onCheckedChange = { auto = it })
        }

        Spacer(Modifier.height(12.dp))

        ui.lamps.forEach { lamp ->
            val displayName = lamp.name?.takeIf { it.isNotBlank() } ?: lamp.lamp_id
            val formattedEnergy = "%.3f".format(lamp.energy_kwh)

            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Лампочка: $displayName")
                    Text("Стан: ${if (lamp.state) "Увімкнено" else "Вимкнено"}")
                    Text("Яскравість: ${lamp.brightness}%")
                    Text("Енергія: $formattedEnergy кВт⋅год")

                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { selectedLampId = lamp.lamp_id }) {
                        Text("Графік")
                    }

                    if (selectedLampId == lamp.lamp_id) {
                        val stats = vm.lampStats[lamp.lamp_id]?.takeLast(20) ?: emptyList()

                        if (stats.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Графік енергії (останні 20 точок)", style = MaterialTheme.typography.titleSmall)

                            Canvas(modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .padding(8.dp)) {
                                val maxEnergy = stats.maxOf { it.second }
                                val minEnergy = stats.minOf { it.second }
                                val range = maxEnergy - minEnergy
                                val stepX = size.width / (stats.size - 1).coerceAtLeast(1)
                                val points = stats.mapIndexed { i, (_, value) ->
                                    val normY = if (range == 0f) 0f else (value - minEnergy) / range
                                    Offset(i * stepX, size.height * (1f - normY))
                                }
                                for (i in 0 until points.size - 1) {
                                    drawLine(Color.Blue, points[i], points[i + 1], strokeWidth = 3f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
