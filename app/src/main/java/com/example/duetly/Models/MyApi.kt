package com.example.duetly.Models

import com.example.duetly.AuthFragment
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface MyApi {
    @POST("register")
    fun registerUser(@Body request: AuthFragment.RegisterRequest): Call<RegisterResponse>
}