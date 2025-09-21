package com.example.pw1tv_23

// Підключаємо потрібні бібліотеки Android та Jetpack Compose
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.pw1tv_23.ui.theme.PW1TV23Theme

// Головний клас активності, з якого починається робота застосунку
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent визначає, який інтерфейс буде відображений на екрані
        setContent {
            PW1TV23Theme { // Використовуємо тему оформлення
                // Surface — контейнер, що займає весь екран
                Surface(modifier = Modifier.fillMaxSize()) {
                    //GreetingScreen() // Викликаємо нашу функцію з інтерфейсом
                    BatteryScreen() // Виклик другої функції з інтерфейсом для відображення заряду рівня акамулятора
                }
            }
        }
    }
}

// Окрема функція-компонент для побудови інтерфейсу
@Composable
fun GreetingScreen() {
    // Змінна для збереження введеного імені
    var name by remember { mutableStateOf("") }
    // Змінна для збереження привітального тексту
    var greeting by remember { mutableStateOf("") }

    // Використовуємо Column для розташування елементів вертикально
    Column(
        modifier = Modifier
            .fillMaxSize() // Колонка займає весь доступний простір
            .padding(16.dp), // Відступи від країв екрану
        verticalArrangement = Arrangement.spacedBy(12.dp) // Відстань між елементами 12dp
    ) {
        // Текст із підказкою для користувача
        Text(
            text = "Введіть своє ім’я:",
            style = MaterialTheme.typography.titleMedium
        )

        // Поле введення (TextField) для імені
        TextField(
            value = name, // Поточне значення, яке зберігається у змінній name
            onValueChange = { name = it }, // Оновлюємо змінну при введенні
            label = { Text("Ім’я") }, // Підпис до поля
            modifier = Modifier.fillMaxWidth() // Розтягуємо на всю ширину
        )

        // Кнопка для формування привітання
        Button(
            onClick = { greeting = "Привіт, $name!" }, // При натисканні зберігаємо текст у змінній greeting
            modifier = Modifier.fillMaxWidth() // Кнопка на всю ширину
        ) {
            Text("Привітати") // Текст усередині кнопки
        }

        // Якщо змінна greeting не пуста — виводимо текст привітання
        if (greeting.isNotEmpty()) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// Функція для перегляду у вікні Preview в Android Studio
@Preview(showBackground = true)
@Composable
fun GreetingScreenPreview() {
    PW1TV23Theme {
        GreetingScreen() // Викликаємо нашу функцію для відображення у Preview
    }
}
