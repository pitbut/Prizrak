package com.pit.bahromtaxi.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("rides")
    suspend fun createRide(@Body body: CreateRideRequest): RideDto

    @GET("rides/pending")
    suspend fun pendingRides(): List<RideDto>

    @POST("rides/{id}/accept")
    suspend fun acceptRide(@Path("id") id: String): RideDto

    @POST("rides/{id}/start")
    suspend fun startRide(@Path("id") id: String): RideDto

    @POST("rides/{id}/complete")
    suspend fun completeRide(@Path("id") id: String): RideDto

    @POST("drivers/{id}/online")
    suspend fun setOnline(@Path("id") id: String, @Body body: OnlineRequest)

    @GET("drivers/{id}/commission")
    suspend fun commission(@Path("id") id: String): CommissionDto

    @POST("drivers/{id}/commission/pay")
    suspend fun payCommission(@Path("id") id: String): CommissionDto
}
