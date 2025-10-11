package com.example.lightcontrolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.lightcontrolapp.ui.theme.LightcontrolappTheme



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightcontrolappTheme{
                MainScreen()
            }
        }
    }
}

// --- Навігаційні елементи ---
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Profile : Screen("profile", "Профіль", Icons.Filled.AccountCircle)
    object Control : Screen("control", "Керування", Icons.Filled.Tune)
    object Lamps : Screen("lamps", "Лампочки", Icons.Filled.Lightbulb)
    object Energy : Screen("energy", "Енергія", Icons.Filled.Bolt)
    object Monitor : Screen("monitor", "Моніторинг", Icons.Filled.List)
}

// --- Головний екран з Bottom Navigation ---
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Profile.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Control.route) { ControlScreen() }
            composable(Screen.Lamps.route) { LampsScreen() }
            composable(Screen.Energy.route) { EnergyScreen() }
            composable(Screen.Monitor.route) { MonitorScreen() }
        }
    }
}

// --- Нижнє меню ---
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Profile,
        Screen.Control,
        Screen.Lamps,
        Screen.Energy,
        Screen.Monitor
    )

    NavigationBar {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.route == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// --- Екрани ---
@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Екран профілю користувача")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: Реєстрація/Вхід */ }) {
            Text("Увійти / Зареєструватись")
        }
    }
}

@Composable
fun ControlScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Керування яскравістю та часом роботи")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: Змінити параметри лампи */ }) {
            Text("Налаштувати лампочку")
        }
    }
}

@Composable
fun LampsScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Додавання / видалення лампочок")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: Додати нову лампочку */ }) {
            Text("Додати лампочку")
        }
    }
}

@Composable
fun EnergyScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Обрахунок енергоспоживання")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: Розрахувати споживання */ }) {
            Text("Розрахувати")
        }
    }
}

@Composable
fun MonitorScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Моніторинг лампочок у реальному часі")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: Оновити дані */ }) {
            Text("Оновити стан")
        }
    }
}