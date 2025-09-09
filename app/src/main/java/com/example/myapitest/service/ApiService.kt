package com.example.myapitest.service

import com.example.myapitest.model.Car
import com.example.myapitest.model.CarResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("car") suspend fun getItems():List<Car>
    @GET("car/{id}") suspend fun getCar(@Path("id") id: String): CarResponse

    @POST("car") suspend fun addCar(@Body item: Car): Car

}