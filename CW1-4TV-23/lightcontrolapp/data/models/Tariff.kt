package com.example.lightcontrolapp.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Tariff(
    val start_min: Int,
    val end_min: Int,
    val price: Double
)
