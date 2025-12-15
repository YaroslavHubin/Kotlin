package com.example.lightcontrolapp.data.network

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    fun saveToken(t: String) = prefs.edit().putString("jwt", t).apply()
    fun token(): String? = prefs.getString("jwt", null)
}
