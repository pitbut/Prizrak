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
    val driverName: String? = null
)

data class RegisterRequest(val role: String, val name: String)

data class AuthResponse(val token: String, val userId: String, val role: String, val name: String)

data class CreateRideRequest(
    val fromAddress: String,
    val toAddress: String,
    val distanceKm: Double,
    val durationMin: Double
)

data class OnlineRequest(val online: Boolean)

data class CommissionDto(val owed: Double, val paid: Double)

data class WsEvent(val type: String, val ride: RideDto? = null)
