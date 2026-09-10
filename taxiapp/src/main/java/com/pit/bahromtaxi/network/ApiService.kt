package com.pit.bahromtaxi.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @POST("rides/{id}/cancel")
    suspend fun cancelRide(@Path("id") id: String): RideDto

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

    @DELETE("account")
    suspend fun deleteAccount()

    @GET("intercity/trips")
    suspend fun intercityTrips(
        @Query("from") fromCity: String? = null,
        @Query("to") toCity: String? = null
    ): List<IntercityTripDto>

    @GET("intercity/trips/mine")
    suspend fun myIntercityTrips(): List<IntercityTripDto>

    @POST("intercity/trips")
    suspend fun createIntercityTrip(@Body body: CreateIntercityTripRequest): IntercityTripDto

    @POST("intercity/trips/{id}/book")
    suspend fun bookIntercitySeats(@Path("id") id: String, @Body body: BookSeatsRequest): IntercityTripDto

    @POST("intercity/trips/{id}/cancel-booking")
    suspend fun cancelIntercityBooking(@Path("id") id: String): IntercityTripDto

    @POST("intercity/trips/{id}/depart")
    suspend fun departIntercityTrip(@Path("id") id: String): IntercityTripDto

    @POST("intercity/trips/{id}/complete")
    suspend fun completeIntercityTrip(@Path("id") id: String): IntercityTripDto

    @POST("intercity/trips/{id}/cancel")
    suspend fun cancelIntercityTrip(@Path("id") id: String): IntercityTripDto

    @GET("group-trips/open")
    suspend fun openGroupTrips(): List<GroupTripDto>

    @GET("group-trips/mine")
    suspend fun myGroupTrips(): List<GroupTripDto>

    @POST("group-trips")
    suspend fun createGroupTrip(@Body body: CreateGroupTripRequest): GroupTripDto

    @POST("group-trips/{id}/offer")
    suspend fun offerGroupTripSeats(@Path("id") id: String, @Body body: GroupTripOfferRequest): GroupTripDto

    @POST("group-trips/{id}/offers/{offerId}/accept")
    suspend fun acceptGroupTripOffer(@Path("id") id: String, @Path("offerId") offerId: String): GroupTripDto

    @POST("group-trips/{id}/offers/{offerId}/reject")
    suspend fun rejectGroupTripOffer(@Path("id") id: String, @Path("offerId") offerId: String): GroupTripDto

    @POST("group-trips/{id}/offers/{offerId}/cancel")
    suspend fun cancelGroupTripOffer(@Path("id") id: String, @Path("offerId") offerId: String): GroupTripDto

    @POST("group-trips/{id}/cancel")
    suspend fun cancelGroupTrip(@Path("id") id: String): GroupTripDto
}
