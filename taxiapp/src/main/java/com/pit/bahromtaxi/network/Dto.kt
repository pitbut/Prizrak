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

data class WsEvent(
    val type: String,
    val ride: RideDto? = null,
    val message: ChatMessageDto? = null,
    val intercityTrip: IntercityTripDto? = null,
    val groupTrip: GroupTripDto? = null
)

data class IntercityBookingDto(
    val id: String,
    val passengerId: String,
    val passengerName: String? = null,
    val passengerPhone: String? = null,
    val seats: Int,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null
)

data class IntercityTripDto(
    val id: String,
    val driverId: String,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverCarMake: String? = null,
    val driverCarPlate: String? = null,
    val fromCity: String,
    val toCity: String,
    val totalSeats: Int,
    val bookedSeats: Int,
    val pricePerSeat: Double,
    val scheduledAt: String? = null,
    val status: String,
    val createdAt: String? = null,
    val myBookedSeats: Int? = null,
    val pickupMode: String? = null,
    val dropoffMode: String? = null,
    val pickupPoint: String? = null,
    val dropoffPoint: String? = null,
    val bookings: List<IntercityBookingDto>? = null
)

data class CreateIntercityTripRequest(
    val fromCity: String,
    val toCity: String,
    val totalSeats: Int,
    val pricePerSeat: Double,
    val scheduledAt: String? = null,
    val pickupMode: String = "single",
    val dropoffMode: String = "single",
    val pickupPoint: String? = null,
    val dropoffPoint: String? = null
)

data class BookSeatsRequest(
    val seats: Int,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null
)

data class GroupTripOfferDto(
    val id: String,
    val driverId: String,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val carMake: String? = null,
    val carPlate: String? = null,
    val seatsOffered: Int,
    val status: String
)

data class GroupTripDto(
    val id: String,
    val passengerId: String,
    val passengerName: String? = null,
    val passengerPhone: String? = null,
    val fromCity: String,
    val toCity: String,
    val peopleCount: Int,
    val seatsConfirmed: Int,
    val desiredAt: String? = null,
    val status: String,
    val createdAt: String? = null,
    val offers: List<GroupTripOfferDto>? = null
)

data class CreateGroupTripRequest(
    val fromCity: String,
    val toCity: String,
    val peopleCount: Int,
    val desiredAt: String? = null
)

data class GroupTripOfferRequest(val seats: Int)

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
