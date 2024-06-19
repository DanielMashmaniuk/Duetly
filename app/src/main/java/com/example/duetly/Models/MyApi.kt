package com.example.duetly.Models

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface MyApi {
    @POST("register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>
}