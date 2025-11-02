package com.example.myapplication


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*

import androidx.compose.material3.*
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Головна Activity: підключає тему (MyApplicationTheme) з шаблону Android Studio
 * і рендерить екран зарядки із ViewModel.
 */
class MainActivity : ComponentActivity() {
    private val vm: ChargingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChargingScreen(vm = vm)
                }
            }
        }
    }
}

/**
 * Компонент екрану зарядки.
 * Відображає:
 * - Карточку з інформацією про автомобіль
 * - Прогрес-індикатор (анімований)
 * - Кнопки Start/Stop
 * - Вибір зарядника (Fast / Slow)
 * - Невелика "пульсація" анімація під час зарядки
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingScreen(vm: ChargingViewModel = viewModel()) {
    val car by vm.car.collectAsState()
    val charger by vm.charger.collectAsState()
    val isCharging by vm.isCharging.collectAsState()
    val added by vm.addedKWh.collectAsState()
    val secs by vm.chargingSeconds.collectAsState()

    // Для анімації пульсації при зарядці
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCharging) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Круговий прогрес - від 0f до 1f
    val progress = car.battery.percent().coerceIn(0, 100) / 100f

    // Простий формат часу hh:mm:ss
    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    // головний layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Зарядна станція — модель EV Charger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Інформація про автомобіль
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Автомобіль: ${car.model}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(text = "Батарея: ${car.battery.currentKWh.roundToInt()} / ${car.battery.capacityKWh.roundToInt()} kWh (${car.battery.percent()}%)")
                Spacer(Modifier.height(6.dp))
                Text(text = "Вибраний зарядник: ${charger.name} (${charger.powerKW} kW)")
            }
        }

        // Центральна частина: круговий прогрес з анімацією
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            // Фонова "пульсація" — видима під час зарядки
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulse)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF80D8FF).copy(alpha = if (isCharging) 0.4f else 0.08f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Круговий прогрес (ProgressIndicator) + центральний текст
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(160.dp),
                strokeWidth = 12.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${car.battery.percent()}%", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text("${car.battery.currentKWh.format(1)} / ${car.battery.capacityKWh.format(0)} kWh", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Статистика та контролли
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                if (!isCharging) vm.startCharging() else vm.stopCharging()
            }, modifier = Modifier.weight(1f)) {
                Text(if (!isCharging) "Почати зарядку" else "Зупинити")
            }

            OutlinedButton(onClick = {
                vm.setBatteryPercent(20) // швидко скидаємо до 20% для демонстрації
            }, modifier = Modifier.weight(1f)) {
                Text("Встановити 20%")
            }
        }

        // Вибір зарядника
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButtonWithLabel(
                label = "Повільний (7 kW)",
                selected = charger is SlowCharger,
                onSelect = { vm.selectCharger(SlowCharger()) }
            )

            RadioButtonWithLabel(
                label = "Швидкий (50 kW)",
                selected = charger is FastCharger,
                onSelect = { vm.selectCharger(FastCharger()) }
            )
        }

        // Невелика панель зі статистикою
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Статистика сесії", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Додано енергії: ${added.format(3)} kWh")
                Text("Час зарядки: ${formatTime(secs)}")
            }
        }

        // Примітка / інструкція
        Text("Підказка: натисни 'Почати зарядку' — прогрес буде оновлюватися. Можна переключити тип зарядника.", style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Маленький компонент — radio + label
 */
@Composable
fun RadioButtonWithLabel(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label)
    }
}

/**
 * Розширення для форматування чисел
 */
private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)