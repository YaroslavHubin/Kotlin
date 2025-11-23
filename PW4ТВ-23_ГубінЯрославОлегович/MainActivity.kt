package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import java.text.SimpleDateFormat
import java.util.*

/* --------------------
   моделі даних
   -------------------- */
// Модель станції зарядки EV
data class ChargerStation(
    val id: String,          // Унікальний ідентифікатор станції
    val name: String,        // Назва станції
    val location: String,    // Локація станції
    val capacityKWh: Double, // Максимальна ємність у кВт·год
    val totalConnectors: Int // Кількість роз’ємів для зарядки
)

/* --------------------
   Result wrapper (обгортка результату для безпечного виклику)
   -------------------- */
data class ResultWrapper<T>(
    val data: T? = null,      // Дані (якщо успішно)
    val error: String? = null,// Повідомлення про помилку
    val success: Boolean = data != null && error == null // Флаг успішності
)

/* --------------------
   Safe call utility (функція безпечного виклику з обробкою помилок)
   -------------------- */
inline fun <T> safeCall(action: () -> T): ResultWrapper<T> {
    return try {
        ResultWrapper(data = action()) // Якщо успішно, повертаємо дані
    } catch (e: Exception) {
        Log.e("safeCall", "Error in safeCall", e) // Лог помилки
        ResultWrapper(error = e.message ?: "Unknown error") // Обгортка з повідомленням про помилку
    }
}

/* --------------------
   Extensions (розширення для зручності)
   -------------------- */
fun Double.toKWhString(): String = String.format(Locale.getDefault(), "%.2f kWh", this) // Форматування кВт·год
fun Double.toCurrencyString(): String = String.format(Locale.getDefault(), "%.2f ₴", this) // Форматування валюти
fun Long.toReadableTime(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this)) // Конвертація часу в зручний формат
}
fun String.toDoubleSafe(default: Double = 0.0): Double =
    try { this.replace(',', '.').trim().toDouble() } catch (e: Exception) { default } // Безпечне перетворення рядка в Double
fun ChargerStation.fullDescription(): String =
    "$name — $location • ${capacityKWh.toKWhString()} • ${totalConnectors} connectors" // Повний опис станції

/* --------------------
  делегати для властивостей
   -------------------- */
// Делегат для цілих чисел, щоб не було від’ємних значень
class NonNegativeDelegate(private var value: Int = 0) : ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: Int) {
        value = if (newValue < 0) 0 else newValue
    }
}

// Делегат для логування змін властивості
class LoggingDelegate<T>(private var value: T, private val name: String = "prop") : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        Log.d("LoggingDelegate", "$name: $value -> $newValue")
        value = newValue
    }
}

/* --------------------
   (репозиторій даних
   -------------------- */
class InMemoryChargingRepo {
    // Список станцій
    val stations = mutableListOf(
        ChargerStation("s-1", "Central Station", "Main St 10", 150.0, 4),
        ChargerStation("s-2", "Mall Station", "Mall Blvd 2", 200.0, 6)
    )

    // Ціни на зарядку для станцій
    val stationPrices = mutableMapOf(
        "s-1" to 50.0,
        "s-2" to 55.0
    )

    // Отримати всі станції (безпечно)
    fun getAllStations(): ResultWrapper<List<ChargerStation>> = safeCall { stations.toList() }
}

/* --------------------
   ViewModel (логіка UI та стан)
   -------------------- */
class StationViewModel(val repo: InMemoryChargingRepo = InMemoryChargingRepo()) : ViewModel() {

    // Кількість доступних слотів зарядки (не може бути <0)
    var availableSlots: Int by NonNegativeDelegate(4)

    // Режим логування змін
    var verboseMode: Boolean by LoggingDelegate(false, "verboseMode")

    // Стан списку станцій для UI
    var stationsState by mutableStateOf<ResultWrapper<List<ChargerStation>>?>(null)
        private set

    // Повідомлення про помилку
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Завантаження станцій з репозиторію
    fun loadStations() {
        stationsState = repo.getAllStations()
        if (!stationsState!!.success) errorMessage = stationsState!!.error
    }

    // Додавання нової станції
    fun addStation(station: ChargerStation) {
        repo.stations.add(station)
        stationsState = ResultWrapper(data = repo.stations.toList())
    }
}

/* --------------------
   MainActivity (головна Activity)
   -------------------- */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Встановлюємо Compose UI
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EVChargingApp() // Головний UI додатка
                }
            }
        }
    }
}

/* --------------------
   EVChargingApp UI (головний UI-компонент)
   -------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EVChargingApp(viewModel: StationViewModel = viewModel()) {
    val scope = rememberCoroutineScope() // CoroutineScope для роботи з корутинами
    val snackbarHostState = remember { SnackbarHostState() } // Стан для snackbar

    var showAddDialog by remember { mutableStateOf(false) } // Показ діалогу додавання
    var query by rememberSaveable { mutableStateOf("") } // Поле для пошуку станції

    // Завантажуємо станції при першому запуску
    LaunchedEffect(Unit) { viewModel.loadStations() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EV Charging Stations", fontWeight = FontWeight.Bold) },
                actions = {
                    var showInfo by remember { mutableStateOf(false) }
                    IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, "Info") }
                    IconButton(onClick = { viewModel.loadStations() }) { Icon(Icons.Default.Refresh, "Оновити") }

                    if (showInfo) {
                        AlertDialog(
                            onDismissRequest = { showInfo = false },
                            title = { Text("Про додаток") },
                            text = { Text("Додаток демонструє моніторинг зарядних станцій EV.\n\nДодавання нових станцій та перегляд інформації.") },
                            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Закрити") } }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add station")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp)) {
            // Поле пошуку
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Пошук станції") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Список станцій
            StationsList(viewModel, query, onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } })

            Spacer(modifier = Modifier.height(8.dp))

            // Карточка доступних слотів
            Card(modifier = Modifier.fillMaxWidth().animateContentSize(tween(300))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ElectricBolt, "Slots",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Доступні слоти", style = MaterialTheme.typography.titleMedium)
                        Text("${viewModel.availableSlots}", style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.availableSlots -= 1 }) { Text("Відняти слот") }
                }
            }
        }

        // Діалог додавання станції
        if (showAddDialog) {
            AddStationDialog(viewModel = viewModel, onDismiss = { showAddDialog = false }, onResult = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            })
        }
    }
}

/* --------------------
   StationsList (список станцій)
   -------------------- */
@Composable
fun StationsList(viewModel: StationViewModel, query: String, onShowMessage: (String) -> Unit) {
    val state = viewModel.stationsState

    when {
        // Якщо стан ще не завантажено
        state == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        // Якщо сталася помилка
        !state.success -> Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Помилка: ${state.error}"); Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.loadStations() }) { Text("Спробувати ще") }
        }
        // Якщо дані успішно завантажено
        else -> {
            val items = state.data?.filter { it.name.contains(query, true) || it.location.contains(query, true) } ?: emptyList()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { station ->
                    StationCardWithPrice(station = station, price = viewModel.repo.stationPrices[station.id] ?: 0.0)
                }
            }
        }
    }
}

/* --------------------
   StationCardWithPrice (карточка станції з ціною)
   -------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationCardWithPrice(station: ChargerStation, price: Double) {
    var expanded by remember { mutableStateOf(false) } // Розкриття додаткової інформації
    val elevation by animateFloatAsState(targetValue = if (expanded) 12f else 4f, animationSpec = tween(350, easing = FastOutSlowInEasing))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).animateContentSize(),
        elevation = CardDefaults.cardElevation(Dp(elevation))
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(station.location, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(station.capacityKWh.toKWhString(), fontWeight = FontWeight.SemiBold)
                    Text("${station.totalConnectors} connectors", style = MaterialTheme.typography.bodySmall)
                    Text("Ціна: ${price.toCurrencyString()}", style = MaterialTheme.typography.bodySmall)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Divider(); Spacer(Modifier.height(8.dp))
                    Text("Опис: ${station.fullDescription()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Оновлено: ${System.currentTimeMillis().toReadableTime()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/* --------------------
   AddStationDialog (діалог додавання станції)
   -------------------- */
@Composable
fun AddStationDialog(viewModel: StationViewModel, onDismiss: () -> Unit, onResult: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var capacityText by rememberSaveable { mutableStateOf("50.0") }
    var connectorsText by rememberSaveable { mutableStateOf("2") }
    var priceText by rememberSaveable { mutableStateOf("50.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати нову станцію") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Назва станції") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(location, { location = it }, label = { Text("Локація") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(capacityText, { capacityText = it }, label = { Text("Об'єм (kWh)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(connectorsText, { connectorsText = it }, label = { Text("Підключення") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(priceText, { priceText = it }, label = { Text("Ціна за кВт·год") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val capacity = capacityText.toDoubleSafe()
                val connectors = connectorsText.toIntOrNull() ?: 1
                val price = priceText.toDoubleSafe()
                if (name.isBlank() || location.isBlank()) { onResult("Будь ласка, введіть назву та локацію"); return@Button }
                val station = ChargerStation(UUID.randomUUID().toString(), name, location, capacity, connectors)
                viewModel.addStation(station)
                viewModel.repo.stationPrices[station.id] = price
                onResult("Станцію додано успішно (ціна: ${price.toCurrencyString()})")
                onDismiss()
            }) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
