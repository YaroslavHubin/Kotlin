package com.example.lightcontrolapp.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun DropdownMenuBox(items: List<String>, selectedId: String?, onSelect: (String)->Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedId ?: "Оберіть лампочку"
    OutlinedButton(onClick = { expanded = true }, modifier = modifier) { Text(label) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        items.forEach {
            DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false })
        }
    }
}
