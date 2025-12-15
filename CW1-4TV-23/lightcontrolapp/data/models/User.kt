package com.example.lightcontrolapp.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val user_id: String,
    val email: String,
    val lamp_ids: List<String>,
    val tariffs: List<Tariff>
)
