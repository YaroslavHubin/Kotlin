package com.example.pw2tv_23

// Імпорти необхідних бібліотек Jetpack Compose і Android
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.example.pw2tv_23.ui.theme.PW2TV23Theme

// --- Головний клас активності (точка входу програми) ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent визначає, який інтерфейс буде відображений на екрані
        setContent {
            PW2TV23Theme { // Використовуємо тему оформлення
                // Surface — контейнер, що займає весь екран
                Surface(modifier = Modifier.fillMaxSize()) {
                    //EVStationCalculator()
                    ApplianceEnergyCalculator()
                }
            }
        }
    }
}

// --- Основна функція інтерфейсу користувача ---
@Composable
fun EVStationCalculator() {
    // Змінні для введення користувачем (поля вводу)
    // remember{} зберігає стан введення при зміні UI
    var powerList by remember { mutableStateOf(TextFieldValue("")) }   // Список потужностей
    var hoursList by remember { mutableStateOf(TextFieldValue("")) }   // Список часів зарядки
    var efficiency by remember { mutableStateOf(TextFieldValue("90")) } // ККД за замовчуванням
    var tariff by remember { mutableStateOf(TextFieldValue("6.5")) }    // Тариф за 1 кВт·год

    var result by remember { mutableStateOf("") } // Рядок для відображення результату

    // --- Головна вертикальна колонка для розташування елементів ---
    Column(
        modifier = Modifier
            .fillMaxSize()               // Заповнює весь екран
            .padding(16.dp),        // Відступи з країв
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнювання елементів по центру
    ) {
        // Заголовок програми
        Text(
            "Розрахунок енергоспоживання зарядної станції",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp)) // Відступ між елементами

        // --- Поля введення даних ---
        OutlinedTextField(
            powerList,                                           // Поточне значення тексту
            { powerList = it },                          // Оновлення змінної при введенні
            label = { Text("Потужність авто (через кому, кВт)") } // Підпис поля
        )

        OutlinedTextField(
            hoursList,
            { hoursList = it },
            label = { Text("Час зарядки авто (через кому, год)") }
        )

        OutlinedTextField(
            efficiency,
            { efficiency = it },
            label = { Text("ККД (%)") }
        )

        OutlinedTextField(
            tariff,
            { tariff = it },
            label = { Text("Тариф (грн/кВт·год)") }
        )

        Spacer(Modifier.height(12.dp))

        // --- Кнопка розрахунку ---
        Button(onClick = {
            try {
                // Розбиваємо введені значення за комами, обрізаємо пробіли та перетворюємо у числа
                val powers = powerList.text.split(",").map { it.trim().toDouble() }
                val times = hoursList.text.split(",").map { it.trim().toDouble() }

                // Зчитуємо ККД і тариф
                val e = efficiency.text.toDouble()
                val c = tariff.text.toDouble()

                // Якщо кількість потужностей не збігається з кількістю часів — помилка
                if (powers.size != times.size) {
                    result = "Кількість потужностей і часів має збігатися"
                    return@Button // Вихід з обробника кнопки
                }

                // --- Обчислення енергії для кожного авто ---
                // mapIndexed дає доступ і до елемента, і до його індексу
                val energies = powers.mapIndexed { i, p ->
                    // Для кожного авто викликаємо функцію розрахунку енергії
                    calculateEnergy(p, times[i], e)
                }.toDoubleArray() // Перетворюємо список у масив типу DoubleArray

                // --- Використання vararg-функції ---
                // Використовуємо оператор * для "розгортання" масиву у список аргументів
                val totalEnergy = totalEnergyForCars(*energies)

                // Розраховуємо загальну вартість зарядки
                val totalCost = calculateCost(totalEnergy, c)

                // Формуємо рядок результату
                result = buildString {
                    appendLine("Енергія кожного авто:")
                    energies.forEachIndexed { i, value ->
                        appendLine("Авто ${i + 1}: %.2f кВт·год".format(value))
                    }
                    appendLine("\nЗагалом: %.2f кВт·год".format(totalEnergy))
                    appendLine("Вартість: %.2f грн".format(totalCost))
                }

            } catch (ex: Exception) {
                // Якщо виникла будь-яка помилка при конвертації — показуємо повідомлення
                result = "Помилка введення даних"
            }
        }) {
            Text("Обчислити для всіх авто")
        }

        Spacer(Modifier.height(20.dp))

        // Вивід результату на екран
        Text(result)
    }
}

//
// --- 1 Функція calculateEnergy() ---
// Використовується для обчислення енергії, яку спожило одне авто
// Формула: E = P * t / η
// де P — потужність (кВт), t — час (год), η — ККД (у частках, не у відсотках)
fun calculateEnergy(power: Double, time: Double, efficiency: Double): Double {
    val kpd = efficiency / 100.0  // Переводимо ККД у десяткову частку
    return power * time / kpd     // Обчислюємо фактичне споживання енергії
}

//
// --- 2 Функція calculateCost() ---
// Використовується для обчислення вартості спожитої енергії
// Формула: Вартість = Енергія (кВт·год) * Тариф (грн/кВт·год)
fun calculateCost(energyKWh: Double, tariff: Double): Double {
    return energyKWh * tariff
}

//
// --- 3 Функція totalEnergyForCars() ---
// Використовує параметр vararg — приймає змінну кількість аргументів типу Double
// Наприклад, totalEnergyForCars(36.6, 24.4, 25.0)
// Використовується для підрахунку сумарної енергії для всіх автомобілів
fun totalEnergyForCars(vararg energies: Double): Double {
    return energies.sum() // Повертає суму всіх переданих значень
}


