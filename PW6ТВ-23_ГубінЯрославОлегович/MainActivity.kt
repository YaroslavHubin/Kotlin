package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

// --- Модель даних ---
data class SensorReading(
    val timestamp: Long,   // час зчитування
    val stationId: String, // ідентифікатор станції
    val powerKw: Double,   // потужність у кВт
    val voltage: Double,   // напруга В
    val currentA: Double,  // струм А
    val status: String     // стан станції (charging/idle/error)
)

// --- Агрегат по станції для статистики ---
data class StationAggregate(
    val stationId: String, // ID станції
    val count: Int,        // кількість зчитувань
    val sumPower: Double,  // сумарна потужність
    val lastPower: Double  // остання потужність
) {
    // додаємо нове зчитування, повертаючи новий агрегат
    fun add(r: SensorReading): StationAggregate =
        copy(count = count + 1, sumPower = sumPower + r.powerKw, lastPower = r.powerKw)

    // середня потужність по станції
    val avgPower: Double get() = if (count == 0) 0.0 else sumPower / count

    companion object {
        // створення "порожнього" агрегату
        fun empty(stationId: String) = StationAggregate(stationId, 0, 0.0, 0.0)
    }
}

// --- Репозиторій: обробка, агрегування, збереження даних ---
class MonitorRepository(private val scope: CoroutineScope) {

    private val TAG = "MonitorRepository"

    // канал для прийому зчитувань
    private val inputChannel = Channel<SensorReading>(Channel.UNLIMITED)
    private val rawFlow = inputChannel.receiveAsFlow() // конвертація в Flow

    // стан останніх зчитувань
    private val _recentReadings = MutableStateFlow<List<SensorReading>>(emptyList())
    val recentReadings: StateFlow<List<SensorReading>> = _recentReadings.asStateFlow()

    // агрегати станцій
    private val _aggregates = MutableStateFlow<Map<String, StationAggregate>>(emptyMap())
    val aggregates: StateFlow<Map<String, StationAggregate>> = _aggregates.asStateFlow()

    init {
        // запускаємо обробку Flow у корутині
        scope.launch {
            rawFlow
                .filter { valid(it) }       // відкидаємо некоректні значення
                .map { transform(it) }      // округлюємо числа
                .onEach { r ->
                    // оновлюємо останні зчитування (макс 50)
                    _recentReadings.update { prev -> (listOf(r) + prev).take(50) }

                    // оновлюємо агрегати по станціях
                    _aggregates.update { prev ->
                        val copy = prev.toMutableMap()
                        val agg = copy[r.stationId] ?: StationAggregate.empty(r.stationId)
                        copy[r.stationId] = agg.add(r)
                        copy
                    }
                }
                .catch { t ->
                    Log.e(TAG, "flow processing error: ${t.localizedMessage}", t)
                }
                .collect() // збір значень
        }
    }

    // безпечна відправка зчитування в канал
    suspend fun push(reading: SensorReading) {
        try {
            inputChannel.send(reading)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to push reading: ${t.localizedMessage}", t)
        }
    }

    // перевірка чи дані валідні
    private fun valid(r: SensorReading): Boolean =
        r.powerKw >= 0.0 && r.powerKw < 1000.0 && r.voltage in 100.0..1000.0 && r.currentA >= 0.0

    // округлення даних до 2-х знаків після коми
    private fun transform(r: SensorReading): SensorReading {
        fun d(x: Double) = kotlin.math.round(x * 100.0) / 100.0
        return r.copy(powerKw = d(r.powerKw), voltage = d(r.voltage), currentA = d(r.currentA))
    }

    // очищення даних
    fun reset() {
        _recentReadings.value = emptyList()
        _aggregates.value = emptyMap()
    }

    // закриття каналу
    fun close() {
        inputChannel.close()
    }
}

// --- Генератор даних ---
class DataGenerator(private val repo: MonitorRepository, private val scope: CoroutineScope) {
    private var job: Job? = null
    private val stationIds = listOf("S1", "S2", "S3", "S4")
    private val TAG = "DataGenerator"

    fun start(rateMs: Long = 500L) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            val rnd = Random(System.currentTimeMillis())
            while (isActive) {
                for (id in stationIds) {
                    val status = randomStatus(rnd)
                    val basePower = when (status) {
                        "charging" -> rnd.nextDouble(3.0, 150.0)
                        "idle" -> 0.0
                        "error" -> 0.0
                        else -> 0.0
                    }
                    val reading = SensorReading(
                        timestamp = System.currentTimeMillis(),
                        stationId = id,
                        powerKw = basePower,
                        voltage = rnd.nextDouble(200.0, 420.0),
                        currentA = if (basePower > 0) (basePower * 1000.0 / 400.0) else 0.0,
                        status = status
                    )
                    // захищена відправка: repo.push має свій try/catch
                    try {
                        repo.push(reading)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error pushing reading: ${t.localizedMessage}", t)
                    }
                }
                delay(rateMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun randomStatus(rnd: Random): String {
        val p = rnd.nextDouble()
        return when {
            p < 0.7 -> "charging"
            p < 0.95 -> "idle"
            else -> "error"
        }
    }
}

// --- ViewModel-like контейнер з ExceptionHandler ---
class MonitorViewModel {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("MonitorViewModel", "Unhandled coroutine exception: ${throwable.localizedMessage}", throwable)
    }
    private val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Main + job + handler)

    val repository = MonitorRepository(scope)
    val generator = DataGenerator(repository, scope)

    var isRunning by mutableStateOf(false)
        private set

    fun startGenerator() {
        generator.start()
        isRunning = true
    }

    fun stopGenerator() {
        generator.stop()
        isRunning = false
    }

    fun clearData() {
        repository.reset()
    }

    fun onCleared() {
        try {
            generator.stop()
            repository.close()
        } finally {
            job.cancel()
        }
    }
}

// --- Activity ---
class MainActivity : ComponentActivity() {
    private val vm = MonitorViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MonitorScreen(vm = vm)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(vm: MonitorViewModel) {
    val aggregates by vm.repository.aggregates.collectAsState()
    val recent by vm.repository.recentReadings.collectAsState()
    val isRunning by rememberUpdatedState(newValue = vm.isRunning)

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "EV Charger Monitor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { if (!isRunning) vm.startGenerator() else vm.stopGenerator() }) {
                if (!isRunning) Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                else Icon(Icons.Default.Stop, contentDescription = "Stop")
            }
            Button(onClick = { vm.clearData() }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AggregatesRow(aggregates = aggregates, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(12.dp))

        Text("Графік: останні потужності (кВт)", style = MaterialTheme.typography.titleMedium)
        PowerChart(recent = recent, modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant))

        Spacer(modifier = Modifier.height(12.dp))

        Text("Останні вимірювання", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(recent) { r ->
                ReadingRow(r)
                Divider()
            }
        }
    }
}

@Composable
fun AggregatesRow(aggregates: Map<String, StationAggregate>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (aggregates.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text("Немає агрегованих даних")
                }
            }
        } else {
            aggregates.values.forEach { agg ->
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Станція: ${agg.stationId}", fontWeight = FontWeight.SemiBold)
                        Text("Остання: ${"%.2f".format(agg.lastPower)} кВт")
                        Text("Середня: ${"%.2f".format(agg.avgPower)} кВт")
                        Text("Зчитувань: ${agg.count}")
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingRow(r: SensorReading) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${r.stationId} • ${sdf.format(Date(r.timestamp))}", fontWeight = FontWeight.Medium)
            Text("Power: ${"%.2f".format(r.powerKw)} kW • V: ${"%.1f".format(r.voltage)} V • I: ${"%.1f".format(r.currentA)} A")
        }
        Text(r.status.uppercase(), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun PowerChart(recent: List<SensorReading>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val padding = 8f
        val w = (size.width - padding * 2).coerceAtLeast(1f)
        val h = (size.height - padding * 2).coerceAtLeast(1f)

        if (recent.isEmpty()) {
            // Намалюємо текст без ризику NPE
            drawContext.canvas.nativeCanvas.drawText(
                "Немає даних",
                padding + 10f,
                padding + 20f,
                android.graphics.Paint().apply { textSize = 30f }
            )
            return@Canvas
        }

        val values = recent.map { it.powerKw.coerceAtLeast(0.0) }
        val maxVal = (values.maxOrNull() ?: 1.0).toFloat().coerceAtLeast(1f)
        val minVal = 0f
        val pointCount = values.size
        val stepX = if (pointCount > 1) w / (pointCount - 1) else w / 1f

        val points = values.mapIndexed { i, v ->
            val x = padding + i * stepX
            val y = padding + h - ((v.toFloat() - minVal) / (maxVal - minVal + 1e-6f)) * h
            Offset(x, y)
        }

        // Лінія (з кольором) — безпечний виклик
        for (i in 0 until points.size - 1) {
            drawLine(
                color = androidx.compose.ui.graphics.Color.Black,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Горизонтальна допоміжна лінія
        drawLine(
            color = androidx.compose.ui.graphics.Color.Gray,
            start = Offset(padding, padding + h),
            end = Offset(padding + w, padding + h),
            strokeWidth = 1f
        )
    }
}
