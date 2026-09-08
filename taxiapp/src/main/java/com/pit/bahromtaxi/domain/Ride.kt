package com.pit.bahromtaxi.domain

enum class RideStatus { SEARCHING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }

data class Ride(
    val id: Long,
    val fromAddress: String,
    val toAddress: String,
    val distanceKm: Double,
    val durationMin: Double,
    val price: PriceBreakdown,
    val status: RideStatus = RideStatus.SEARCHING,
    val driverName: String? = null
)
