package com.example.lightcontrolapp.data.network

import com.example.lightcontrolapp.data.models.AuthResponse
import com.example.lightcontrolapp.data.models.*
import retrofit2.http.*

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @GET("auth/me")
    suspend fun me(): UserProfile

    @GET("auth/me")
    suspend fun getProfile(): UserProfile

    @GET("lamps")
    suspend fun lamps(): List<Lamp>

    @POST("lamps")
    suspend fun addLamp(@Body lamp: Map<String, @JvmSuppressWildcards Any>): Map<String, String>

    @GET("lamps/{id}")
    suspend fun lamp(@Path("id") id: String): Lamp

    @PATCH("lamps/{id}")
    suspend fun updateLamp(
        @Path("id") id: String,
        @Body patch: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, Boolean>

    @GET("lamps")
    suspend fun getLamps(): List<Lamp>
    @DELETE("lamps/{id}")
    suspend fun deleteLamp(@Path("id") id: String): Map<String, Boolean>

    @GET("energy/tariffs")
    suspend fun getTariffs(): List<Tariff>

    @PUT("energy/tariffs")
    suspend fun putTariffs(@Body tariffs: List<Tariff>): Map<String, Boolean>

    @PUT("energy/tariffs")
    suspend fun saveTariffs(@Body tariffs: List<Tariff>): Map<String, Boolean>

    @GET("energy/consumption")
    suspend fun consumption(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Map<String, Double>
}
