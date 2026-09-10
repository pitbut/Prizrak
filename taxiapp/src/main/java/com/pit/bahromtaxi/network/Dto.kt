package com.pit.bahromtaxi.network

data class PriceDto(
    val baseFare: Double,
    val distanceCost: Double,
    val timeCost: Double,
    val demandFactor: Double,
    val total: Double,
    val commission: Double,
    val driverPayout: Double
)

data class RideDto(
    val id: String,
    val fromAddress: String,
    val toAddress: String,
    val distanceKm: Double,
    val durationMin: Double,
    val status: String,
    val price: PriceDto,
    val driverId: String? = null,
    val driverName: String? = null,
    val paymentMethod: String? = null,
    val driverClickHandle: String? = null,
    val createdAt: String? = null,
    val driverPhone: String? = null,
    val passengerName: String? = null,
    val passengerPhone: String? = null,
    val type: String? = null,
    val senderPhone: String? = null,
    val receiverPhone: String? = null,
    val cargoDescription: String? = null,
    val cargoWeightKg: Double? = null,
    val scheduledAt: String? = null
)

data class RegisterRequest(
    val firebaseIdToken: String,
    val role: String,
    val name: String,
    val carMake: String? = null,
    val carColor: String? = null,
    val carPlate: String? = null,
    val clickHandle: String? = null,
    val vehicleType: String? = null,
    val capacityKg: Double? = null
)

data class AuthResponse(val token: String, val userId: String, val role: String, val name: String)

data class CreateRideRequest(
    val fromAddress: String,
    val toAddress: String,
    val distanceKm: Double,
    val durationMin: Double,
    val paymentMethod: String,
    val type: String = "ride",
    val senderPhone: String? = null,
    val receiverPhone: String? = null,
    val cargoDescription: String? = null,
    val cargoWeightKg: Double? = null,
    val scheduledAt: String? = null
)

data class OnlineRequest(val online: Boolean)

data class CommissionDto(val owed: Double, val paid: Double)

data class WsEvent(val type: String, val ride: RideDto? = null, val message: ChatMessageDto? = null)

data class ChatMessageDto(
    val id: String,
    val rideId: String,
    val senderId: String,
    val senderRole: String,
    val text: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null
)

data class ProfileDto(val userId: String, val role: String, val name: String, val phone: String? = null, val email: String? = null)

data class ProfileUpdateRequest(val name: String? = null, val phone: String? = null)

data class PhoneSendCodeRequest(val phone: String)

data class PhoneVerifyCodeRequest(val phone: String, val code: String)

data class PhoneVerifyResponse(val firebaseCustomToken: String)
