package com.example.lightcontrolapp.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

fun provideApi(baseUrl: String, tokenStore: TokenStore): ApiService {
    val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val b: Request.Builder = original.newBuilder()
        tokenStore.token()?.let { b.addHeader("Authorization", "Bearer $it") }
        chain.proceed(b.build())
    }

    val client = OkHttpClient.Builder().addInterceptor(authInterceptor).build()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // ← обов’язково для Kotlin data класів
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ApiService::class.java)
}
