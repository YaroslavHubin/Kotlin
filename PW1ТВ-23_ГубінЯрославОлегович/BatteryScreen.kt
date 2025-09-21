package com.example.pw1tv_23

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// Окремий Composable для роботи з рівнем батареї
@Composable
fun BatteryScreen() {
    // Оголошуємо змінну input — рядок, куди буде записуватися введене користувачем значення.
    // remember + mutableStateOf("") означає, що це стан, який зберігається між перерисовками UI.
    var input by remember { mutableStateOf("") }

    // Оголошуємо змінну message — у ній будемо зберігати повідомлення для користувача після перевірки.
    var message by remember { mutableStateOf("") }

    // Column — контейнер, який розташовує елементи один під одним (вертикально).
    Column(
        modifier = Modifier
            .fillMaxSize()      // Колонка займає весь доступний простір екрану.
            .padding(16.dp),    // Відступ від країв екрану по 16 dp.
        verticalArrangement = Arrangement.spacedBy(12.dp) // Відстань між елементами — 12 dp.
    ) {
        // Текст із підказкою для користувача.
        Text(
            text = "Введіть рівень заряду (%)",             // Сам текст.
            style = MaterialTheme.typography.titleMedium    // Стиль із теми (середній заголовок).
        )

        // Поле введення для користувача.
        TextField(
            value = input,                         // Поточне значення текстового поля (змінна input).
            onValueChange = { input = it },        // Кожного разу, коли користувач щось вводить — оновлюємо input.
            label = { Text("Заряд (0–100)") },     // Підпис під полем, підказка.
            modifier = Modifier.fillMaxWidth()     // Текстове поле займає всю ширину контейнера.
        )

        // Кнопка, при натисканні якої виконується перевірка введеного значення.
        Button(
            onClick = {
                // Перетворюємо введений рядок у число (Int). Якщо не вийде — повернеться -1.
                val charge = input.toIntOrNull() ?: -1

                // Використовуємо конструкцію when (аналог switch), щоб визначити повідомлення залежно від рівня заряду.
                message = when {
                    charge in 81..100 -> "Повний заряд!"                        // Від 81 до 100 %
                    charge in 51..80 -> "Заряд нормальний."                     // Від 51 до 80 %
                    charge in 21..50 -> "Заряд низький."                        // Від 21 до 50 %
                    charge in 0..20 -> "Дуже низький заряд! Потрібна підзарядка." // Від 0 до 20 %
                    else -> "Некоректне значення!"                              // Якщо число некоректне або >100
                }
            },
            modifier = Modifier.fillMaxWidth() // Кнопка також займає всю ширину.
        ) {
            // Текст усередині кнопки.
            Text("Перевірити")
        }

        // Перевіряємо: якщо повідомлення (message) не порожнє — виводимо його.
        if (message.isNotEmpty()) {
            Text(
                text = message,                                // Виводимо текст результату.
                style = MaterialTheme.typography.titleLarge    // Стиль — великий заголовок.
            )
        }
    }
}

// Функція для перегляду екрану у вікні Preview в Android Studio.
// Завдяки їй ми можемо бачити, як виглядатиме BatteryScreen, без запуску на телефоні.
@Preview(showBackground = true)
@Composable
fun BatteryScreenPreview() {
    BatteryScreen() // Виклик нашої функції-компонента.
}