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
    val createdAt: String? = null,
    val driverPhone: String? = null,
    val passengerName: String? = null,
    val passengerPhone: String? = null,
    val orderType: OrderType = OrderType.RIDE,
    val senderPhone: String? = null,
    val receiverPhone: String? = null,
    val cargoDescription: String? = null,
    val cargoWeightKg: Double? = null,
    val scheduledAt: String? = null
)
