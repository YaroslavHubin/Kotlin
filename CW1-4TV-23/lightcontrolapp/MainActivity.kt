package com.example.lightcontrolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.lightcontrolapp.data.network.TokenStore
import com.example.lightcontrolapp.data.network.provideApi
import com.example.lightcontrolapp.navigation.Screen
import com.example.lightcontrolapp.ui.screens.*
import com.example.lightcontrolapp.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(this)
        val api = provideApi("http://192.168.0.130:5000/", tokenStore) // Flask локально
        val vm = AppViewModel(api)

        setContent {
            MaterialTheme {
                MainScreen(vm = vm, tokenStore = tokenStore)
            }
        }
    }
}

@Composable
fun MainScreen(vm: AppViewModel, tokenStore: TokenStore) {
    val navController = rememberNavController()
    val ui by vm.state.collectAsState()

    Scaffold(bottomBar = { BottomNavigationBar(navController) }) { padding ->
        NavHost(navController = navController, startDestination = Screen.Profile.route, modifier = Modifier.padding(padding)) {
            composable(Screen.Profile.route) { ProfileScreen(vm, tokenStore) }
            composable(Screen.Control.route) { ControlScreen(ui, vm) }
            composable(Screen.Lamps.route) { LampsScreen(ui, vm) }
            composable(Screen.Energy.route) { EnergyScreen(ui, vm) }
            composable(Screen.Monitor.route) { MonitorScreen(ui, vm) }
        }
    }
}



@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Profile, Screen.Control, Screen.Lamps, Screen.Energy, Screen.Monitor)
    NavigationBar {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(
                    text = screen.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) },
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
