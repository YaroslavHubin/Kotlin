package com.example.lightcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.lightcontrolapp.data.network.TokenStore
import com.example.lightcontrolapp.viewmodel.AppViewModel

@Composable
fun ProfileScreen(vm: AppViewModel, tokenStore: TokenStore) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val ui by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Акаунт", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Електронна адреса") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Пароль") }, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.login(email, pass) { tokenStore.saveToken(it) } }) { Text("Увійти") }
            Button(onClick = { vm.register(email, pass) { tokenStore.saveToken(it) } }) { Text("Зареєструватись") }
        }
        Spacer(Modifier.height(12.dp))
        ui.profile?.let { Text("Ви увійшли як: ${it.email}") }
        ui.error?.let { Text("Помилка: $it", color = MaterialTheme.colorScheme.error) }
    }
}
