package com.example.lightcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lightcontrolapp.data.models.Tariff
import com.example.lightcontrolapp.viewmodel.AppViewModel
import com.example.lightcontrolapp.viewmodel.UiState

@Composable
fun EnergyScreen(ui: UiState, vm: AppViewModel) {
    var dayTariff by remember { mutableStateOf(ui.tariffs.getOrNull(0) ?: Tariff(480, 1080, 2.0)) }
    var nightTariff by remember { mutableStateOf(ui.tariffs.getOrNull(1) ?: Tariff(0, 480, 1.5)) }
    var totalCost by remember { mutableStateOf<Double?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Налаштування тарифів", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        TariffCard("Денний тариф", dayTariff) { dayTariff = it }
        Spacer(Modifier.height(12.dp))
        TariffCard("Нічний тариф", nightTariff) { nightTariff = it }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                vm.saveTariffs(listOf(nightTariff, dayTariff))
            }) {
                Text("Зберегти тарифи")
            }

            Button(onClick = {
                val lamps = ui.lamps
                val totalEnergy = lamps.sumOf { it.energy_kwh }
                val avgPrice = listOf(dayTariff, nightTariff).map { it.price }.average()
                totalCost = totalEnergy * avgPrice
            }) {
                Text("Розрахувати тариф")
            }
        }

        totalCost?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                "Загальна вартість: %.2f грн".format(it),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun TariffCard(title: String, tariff: Tariff, onChange: (Tariff) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            var start by remember { mutableStateOf((tariff.start_min / 60).toString()) }
            Text("Початок (година):")
            OutlinedTextField(
                value = start,
                onValueChange = {
                    start = it
                    it.toIntOrNull()?.let { v ->
                        onChange(tariff.copy(start_min = (v * 60).coerceIn(0, 1380)))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            var end by remember { mutableStateOf((tariff.end_min / 60).toString()) }
            Text("Кінець (година):")
            OutlinedTextField(
                value = end,
                onValueChange = {
                    end = it
                    it.toIntOrNull()?.let { v ->
                        onChange(tariff.copy(end_min = (v * 60).coerceIn(1, 1440)))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            var price by remember { mutableStateOf(tariff.price.toString()) }
            Text("Ціна, грн/кВт⋅год:")
            OutlinedTextField(
                value = price,
                onValueChange = {
                    price = it
                    it.toDoubleOrNull()?.let { v ->
                        onChange(tariff.copy(price = v))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
