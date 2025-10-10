package com.example.pw2tv_23

// Імпортуємо потрібні бібліотеки Jetpack Compose
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// --- Модель приладу з реактивними станами ---
data class Appliance(
    val name: String,                        // Назва приладу
    val power: Double,                       // Потужність у кВт
    val hours: MutableState<Double> = mutableStateOf(1.0),    // Тривалість роботи (год)
    val selected: MutableState<Boolean> = mutableStateOf(false) // Чи обрано прилад
)

// --- Основний інтерфейс ---
@Composable
fun ApplianceEnergyCalculator() {
    // Створюємо список приладів, який зберігає стан під час роботи додатку
    val appliances = remember {
        mutableStateListOf(
            Appliance("Холодильник", 0.15),
            Appliance("Електрочайник", 2.0),
            Appliance("Пральна машина", 1.5),
            Appliance("Телевізор", 0.1),
            Appliance("Фен", 1.8),
            Appliance("Мікрохвильова піч", 1.2),
            Appliance("Комп’ютер", 0.3)
        )
    }

    var tariff by rememberSaveable { mutableStateOf("6.5") } // Змінна для тарифу
    var result by remember { mutableStateOf("") }             // Текст результату

    // Основна колонка (вертикальне розташування)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Заголовок ---
        Text(
            "Енергоспоживання побутових приладів",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(10.dp)) // Відступ між елементами

        // --- Поле для введення тарифу ---
        OutlinedTextField(
            value = tariff,                         // поточне значення
            onValueChange = { tariff = it },        // зміна значення
            label = { Text("Тариф (грн/кВт·год)") },// напис над полем
            modifier = Modifier.fillMaxWidth(0.9f)  // ширина поля
        )

        Spacer(Modifier.height(10.dp))

        // --- Список приладів ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // займає весь доступний простір по висоті
        ) {
            // Для кожного елемента у списку створюється ApplianceCard
            items(appliances, key = { it.name }) { appliance ->
                ApplianceCard(appliance)
            }
        }

        // --- Кнопка розрахунку ---
        Button(
            onClick = {
                try {
                    val c = tariff.toDouble() // перетворення тарифу в число

                    // Вибираємо лише ті прилади, які були позначені користувачем
                    val selectedAppliances = appliances.filter { it.selected.value }

                    // Якщо жодного приладу не вибрано — виводимо повідомлення
                    if (selectedAppliances.isEmpty()) {
                        result = "Оберіть хоча б один прилад"
                        return@Button
                    }

                    // Обчислення енергії для кожного приладу
                    val energies = selectedAppliances.map {
                        calculateEnergy(it.power, it.hours.value, 100.0) // виклик функції розрахунку
                    }.toDoubleArray()

                    // Обчислення сумарної енергії та вартості
                    val totalEnergy = totalEnergyForCars(*energies)
                    val totalCost = calculateCost(totalEnergy, c)

                    // Формуємо текст для виведення користувачу
                    result = buildString {
                        appendLine("Енергія кожного приладу:")
                        selectedAppliances.forEachIndexed { i, app ->
                            appendLine("${app.name}: %.2f кВт·год".format(energies[i]))
                        }
                        appendLine("\nЗагалом: %.2f кВт·год".format(totalEnergy))
                        appendLine("Вартість: %.2f грн".format(totalCost))
                    }

                } catch (ex: Exception) {
                    // Якщо введено некоректні дані тарифу
                    result = "Некоректні дані тарифу"
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text("Обчислити споживання")
        }

        Spacer(Modifier.height(16.dp))

        // --- Вивід результату ---
        Text(result)
    }
}

// --- Компонент для одного приладу ---
@Composable
fun ApplianceCard(appliance: Appliance) {
    // Карточка з інформацією про прилад
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // --- Назва приладу та чекбокс ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = appliance.selected.value,                // поточний стан
                    onCheckedChange = { appliance.selected.value = it } // зміна стану
                )
                Text(appliance.name, style = MaterialTheme.typography.titleSmall)
            }

            // --- Потужність приладу ---
            Text("Потужність: ${appliance.power} кВт")

            // --- Повзунок для вибору часу роботи ---
            Text("Час роботи: %.1f год".format(appliance.hours.value))
            Slider(
                value = appliance.hours.value.toFloat(),
                onValueChange = { appliance.hours.value = it.toDouble() },
                valueRange = 0.5f..12f, // діапазон (0.5–12 год)
                steps = 23              // кількість кроків
            )
        }
    }
}
