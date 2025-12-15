package com.example.lightcontrolapp.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val user_id: String
)
