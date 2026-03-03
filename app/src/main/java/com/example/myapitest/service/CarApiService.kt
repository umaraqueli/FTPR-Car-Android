package com.example.myapitest.service

import com.example.myapitest.model.Car
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CarApiService {

    @GET("car")
    suspend fun getCars(): List<Car>

    @POST("car")
    suspend fun addCar(@Body car: Car): Car
}
