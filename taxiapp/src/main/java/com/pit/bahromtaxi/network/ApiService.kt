package com.pit.bahromtaxi.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/phone/send-code")
    suspend fun sendPhoneCode(@Body body: PhoneSendCodeRequest)

    @POST("auth/phone/verify-code")
    suspend fun verifyPhoneCode(@Body body: PhoneVerifyCodeRequest): PhoneVerifyResponse

    @GET("profile")
    suspend fun getProfile(): ProfileDto

    @PATCH("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): ProfileDto

    @GET("rides/history")
    suspend fun rideHistory(@Query("limit") limit: Int = 20, @Query("before") before: String? = null): List<RideDto>

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

    @GET("rides/{id}/messages")
    suspend fun getMessages(@Path("id") rideId: String): List<ChatMessageDto>

    @Multipart
    @POST("rides/{id}/messages")
    suspend fun sendMessage(
        @Path("id") rideId: String,
        @Part("text") text: RequestBody?,
        @Part image: MultipartBody.Part?
    ): ChatMessageDto
}
