package com.pit.bahromtaxi.domain

enum class RideStatus { SEARCHING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }

data class Ride(
    val id: String,
    val fromAddress: String,
    val toAddress: String,
    val distanceKm: Double,
    val durationMin: Double,
    val price: PriceBreakdown,
    val status: RideStatus = RideStatus.SEARCHING,
    val driverId: String? = null,
    val driverName: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val driverClickHandle: String? = null,
    val createdAt: String? = null
)
