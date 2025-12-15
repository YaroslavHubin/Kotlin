package com.example.lightcontrolapp.viewmodel

import com.example.lightcontrolapp.data.models.*

data class UiState(
    val selectedLamp: Lamp? = null,
    val profile: UserProfile? = null,
    val lamps: List<Lamp> = emptyList(),
    val tariffs: List<Tariff> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)
