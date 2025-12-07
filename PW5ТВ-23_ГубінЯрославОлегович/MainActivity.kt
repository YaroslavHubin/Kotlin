package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import kotlin.random.Random

// Модель одного вимірювання з датчика зарядної станції
data class SensorReading(
    val sensorId: String,          // Ідентифікатор сенсора
    val timestamp: Long,           // Час зчитування даних
    val voltage: Double,           // Напруга (В)
    val current: Double,           // Сила струму (А)
    val powerKW: Double,           // Потужність (кВт)
    val temperatureC: Double      // Температура (°C)
)

// Клас генератора даних для зарядної станції
class ChargingStationGenerator {

    // Зберігає останні значення з кожного сенсора (Map)
    private val latestReadings: MutableState<Map<String, SensorReading>> =
        mutableStateOf(emptyMap())

    // Список усіх вимірювань (List)
    private val history: MutableList<SensorReading> = mutableListOf()

    // Множина унікальних сенсорів (Set)
    private val sensorIds: MutableSet<String> = mutableSetOf()

    // Корутин-область для асинхронної роботи
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Список активних задач (Job)
    private val jobs: MutableList<Job> = mutableListOf()

    // Доступ до поточних показників у вигляді State
    val latest: State<Map<String, SensorReading>> get() = latestReadings

    // Повертає копію всіх збережених вимірів
    fun getHistorySnapshot(): List<SensorReading> = history.toList()

    // Повертає список унікальних сенсорів
    fun getSensorIdsSnapshot(): Set<String> = sensorIds.toSet()

    // Асинхронний симулятор одного сенсора
    private fun simulateSensor(sensorId: String, intervalMs: Long): Job {
        return scope.launch {                 // Запуск корутини
            while (isActive) {                // Поки задача активна
                val reading = randomReading(sensorId)  // Генерація випадкового значення

                // Оновлення останнього значення для сенсора в Map
                latestReadings.value =
                    latestReadings.value.toMutableMap().apply { put(sensorId, reading) }

                // Синхронне додавання значення в історію
                synchronized(history) {
                    history.add(reading)
                    if (history.size > 500) history.removeAt(0)  // Обмеження історії
                }

                // Додавання сенсора до множини
                sensorIds.add(sensorId)

                // Затримка між вимірюваннями
                delay(intervalMs)
            }
        }
    }

    // Запуск групи сенсорів
    fun startSensors(count: Int = 4) {
        if (jobs.any { it.isActive }) return   // Якщо вже запущено — не перезапускати

        val ids = (1..count).map { "CHG-$it" } // Створення ідентифікаторів сенсорів

        ids.forEachIndexed { idx, id ->        // Запуск кожного сенсора
            val interval = 500L + (idx * 300L)// Різні інтервали
            jobs += simulateSensor(id, interval)
        }
    }

    // Зупинка всіх сенсорів
    fun stop() {
        jobs.forEach { it.cancel() }          // Зупинка кожної задачі
        jobs.clear()                          // Очищення списку
    }

    // Повне завершення роботи генератора
    fun shutdown() {
        stop()                                // Зупинка сенсорів
        scope.cancel()                        // Завершення корутин
    }

    // Аналітика: середня потужність та топ-сенсори
    fun analytics(): Pair<Map<String, Double>, List<String>> {
        val snapshot = getHistorySnapshot()  // Отримання історії

        // Групування за сенсорами
        val grouped: Map<String, List<SensorReading>> =
            snapshot.groupBy { it.sensorId }

        // Обчислення середньої потужності для кожного сенсора
        val avgPower: Map<String, Double> = grouped.mapValues { (_, readings) ->
            if (readings.isEmpty()) 0.0 else readings.map { it.powerKW }.average()
        }

        // Визначення топ-3 сенсорів
        val topSensors: List<String> = avgPower.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        return Pair(avgPower, topSensors)     // Повернення результату
    }

    // Генерація випадкового вимірювання
    private fun randomReading(sensorId: String): SensorReading {
        val voltage = Random.nextDouble(300.0, 420.0)   // Напруга
        val current = Random.nextDouble(0.0, 200.0)    // Струм
        val powerKW = (voltage * current) / 1000.0     // Потужність
        val temp = Random.nextDouble(20.0, 60.0)       // Температура

        // Формування об'єкта вимірювання
        return SensorReading(
            sensorId,
            System.currentTimeMillis(),
            voltage,
            current,
            powerKW,
            temp
        )
    }
}

// Головна Activity додатку
class MainActivity : ComponentActivity() {

    private val generator = ChargingStationGenerator() // Генератор даних

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {                           // Тема додатку
                Surface(
                    modifier = Modifier.fillMaxSize(),   // Розмір екрану
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChargingStationScreen(generator)     // Відображення UI
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        generator.shutdown()                             // Безпечне завершення
    }
}

// Головний екран користувача
@Composable
fun ChargingStationScreen(generator: ChargingStationGenerator) {

    val latestMap by remember { generator.latest }       // Отримання стану сенсорів

    var running by remember { mutableStateOf(false) }  // Стан роботи
    var sensorCount by remember { mutableStateOf(4) }  // Кількість сенсорів

    var avgPower by remember { mutableStateOf<Map<String, Double>>(emptyMap()) } // Середня потужність
    var topSensors by remember { mutableStateOf<List<String>>(emptyList()) }     // Топ сенсорів

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Кнопка запуску
            Button(
                onClick = {
                    if (!running) generator.startSensors(sensorCount)
                    running = true
                },
                modifier = Modifier.weight(1f)
            ) { Text("Start") }

            // Кнопка зупинки
            Button(
                onClick = {
                    generator.stop()
                    running = false
                },
                modifier = Modifier.weight(1f)
            ) { Text("Stop") }

            // Кнопка аналітики
            Button(
                onClick = {
                    val result = generator.analytics()
                    avgPower = result.first
                    topSensors = result.second
                },
                modifier = Modifier.weight(1f)
            ) { Text("Analytics") }
        }

        Spacer(modifier = Modifier.height(12.dp))       // Відступ

        Text("Latest readings (map). Sensors: ${latestMap.size}") // Кількість сенсорів

        // Список сенсорів
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            items(latestMap.values.sortedBy { it.sensorId }) { reading ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sensor: ${reading.sensorId}")
                            Text("Power: ${"%.2f".format(reading.powerKW)} kW")
                            Text("Voltage: ${"%.1f".format(reading.voltage)} V")
                            Text("Current: ${"%.1f".format(reading.current)} A")
                            Text("Temp: ${"%.1f".format(reading.temperatureC)} °C")
                        }
                        //Text("${reading.timestamp}")   // Час виміру
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))       // Відступ

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Analytics summary:")                  // Заголовок

            avgPower.forEach { (id, avg) ->
                Text("$id -> avg power: ${"%.2f".format(avg)} kW")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Top sensors: ${
                    if (topSensors.isEmpty()) "-" else topSensors.joinToString()
                }"
            )
        }
    }
}