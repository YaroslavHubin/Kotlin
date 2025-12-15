package com.example.lightcontrolapp.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Lamp(
    val lamp_id: String,
    val owner_id: String? = null,
    val name: String? = null,
    val state: Boolean,
    val brightness: Int,
    val power_w: Double,
    val work_time_min: Int,
    val energy_kwh: Double
)
