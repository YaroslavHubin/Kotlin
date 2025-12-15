package com.example.lightcontrolapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Profile : Screen("profile", "Профіль", Icons.Filled.AccountCircle)
    object Control : Screen("control", "Керування", Icons.Filled.Tune)
    object Lamps : Screen("lamps", "Лампочки", Icons.Filled.Lightbulb)
    object Energy : Screen("energy", "Енергія", Icons.Filled.Bolt)
    object Monitor : Screen("monitor", "Моніторинг", Icons.Filled.List)
}
