package com.example.lightcontrolapp.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightcontrolapp.data.models.*
import com.example.lightcontrolapp.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.lightcontrolapp.data.models.Lamp

class AppViewModel(private val api: ApiService): ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    val lampStats = mutableStateMapOf<String, MutableList<Pair<Int, Float>>>()

    fun trackLampStats(lamps: List<Lamp>) {
        val timestamp = (System.currentTimeMillis() / 1000).toInt()
        lamps.forEach { lamp ->
            val list = lampStats.getOrPut(lamp.lamp_id) { mutableListOf() }
            list.add(timestamp to lamp.energy_kwh.toFloat())
            if (list.size > 20) list.removeAt(0)
        }
    }

    fun login(email: String, password: String, onToken: (String)->Unit) {
        viewModelScope.launch {
            runCatching {
                _state.value = _state.value.copy(loading = true, error = null)
                val auth = api.login(mapOf("email" to email, "password" to password))
                onToken(auth.token)
                val me = api.me()
                val lamps = api.lamps()
                val tariffs = api.getTariffs()
                _state.value = UiState(profile = me, lamps = lamps, tariffs = tariffs)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message)
            }
        }
    }

    fun addLamp() {
        viewModelScope.launch {
            try {
                val lampId = UUID.randomUUID().toString()
                api.addLamp(mapOf("lamp_id" to lampId))
                refreshLamps()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun deleteLamp(id: String) {
        viewModelScope.launch {
            try {
                api.deleteLamp(id)
                refreshLamps()
            } catch (e: Exception) {
                // optional: UI error state/snackbar
            }
        }
    }
    fun selectLamp(lamp: Lamp) {
        _state.update { it.copy(selectedLamp = lamp) }
    }
    fun register(email: String, password: String, onToken: (String)->Unit) {
        viewModelScope.launch {
            runCatching {
                _state.value = _state.value.copy(loading = true, error = null)
                val auth = api.register(mapOf("email" to email, "password" to password))
                onToken(auth.token)
                val me = api.me()
                _state.value = _state.value.copy(profile = me, loading = false)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message)
            }
        }
    }

    fun refreshLamps() {
        viewModelScope.launch {
            try {
                val lamps = api.getLamps().map { lamp ->
                    if (lamp.state) {
                        val minutes = lamp.work_time_min + 2 // автооновлення кожні 2 секунди ≈ 2 хвилини
                        val energy = (lamp.power_w * lamp.brightness * minutes) / (100 * 60 * 1000)
                        lamp.copy(
                            work_time_min = minutes,
                            energy_kwh = energy
                        )
                    } else {
                        lamp
                    }
                }

                _state.update { it.copy(lamps = lamps) }

                // накопичуємо дані для графіка
                val timestamp = (System.currentTimeMillis() / 1000).toInt()
                lamps.forEach { lamp ->
                    val list = lampStats.getOrPut(lamp.lamp_id) { mutableListOf() }
                    list.add(timestamp to lamp.energy_kwh.toFloat())
                    if (list.size > 50) list.removeAt(0)
                }
            } catch (e: Exception) {
                // TODO: snackbar або лог
            }
        }
    }

    fun updateLamp(
        id: String,
        state: Boolean? = null,
        brightness: Int? = null,
        name: String? = null,
        power_w: Double? = null,
        energy_kwh: Double? = null
    ) {
        viewModelScope.launch {
            val patch = mutableMapOf<String, Any>()
            state?.let { patch["state"] = it }
            brightness?.let { patch["brightness"] = it }
            name?.let { patch["name"] = it }
            power_w?.let { patch["power_w"] = it }
            energy_kwh?.let { patch["energy_kwh"] = it }
            if (patch.isNotEmpty()) {
                api.updateLamp(id, patch)
                refreshLamps()
            }
        }
    }

    fun saveTariffs(tariffs: List<Tariff>) {
        viewModelScope.launch {
            try {
                api.saveTariffs(tariffs)
                refreshProfile()
            } catch (e: Exception) {
                // TODO: snackbar або лог
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val profile = api.me()
                _state.update { it.copy(profile = profile, tariffs = profile.tariffs) }
            } catch (e: Exception) {
                // TODO: snackbar або лог
            }
        }
    }
}
