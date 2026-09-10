package com.pit.bahromtaxi.data

import com.pit.bahromtaxi.domain.GroupTrip
import com.pit.bahromtaxi.domain.GroupTripOffer
import com.pit.bahromtaxi.domain.GroupTripOfferStatus
import com.pit.bahromtaxi.domain.GroupTripStatus
import com.pit.bahromtaxi.network.ApiClient
import com.pit.bahromtaxi.network.AuthStore
import com.pit.bahromtaxi.network.CreateGroupTripRequest
import com.pit.bahromtaxi.network.GroupTripDto
import com.pit.bahromtaxi.network.GroupTripOfferRequest
import com.pit.bahromtaxi.notify.TripReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Групповая поездка (см. domain/GroupTrip.kt) — заявка пассажира на N человек, водители
 * откликаются предложениями мест, пассажир принимает нужные. Как и Intercity, живые обновления
 * идут через общий сокет RideRepository, здесь только подписка.
 */
object GroupTripRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = ApiClient.service

    /** Открытые заявки — для водителей, которые могут предложить места. */
    private val _openTrips = MutableStateFlow<List<GroupTrip>>(emptyList())
    val openTrips: StateFlow<List<GroupTrip>> = _openTrips

    /** Свои заявки: у пассажира — созданные им, у водителя — те, на которые он откликнулся. */
    private val _myTrips = MutableStateFlow<List<GroupTrip>>(emptyList())
    val myTrips: StateFlow<List<GroupTrip>> = _myTrips

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var subscribed = false

    fun ensureSubscribed() {
        if (subscribed) return
        subscribed = true
        scope.launch {
            RideRepository.groupTripEvents.collect { dto ->
                val trip = dto.toDomain()
                mergeInto(_openTrips, trip)
                mergeInto(_myTrips, trip)
            }
        }
        // Будильник за час до отправления — только когда заявка подтверждена (CONFIRMED) и
        // я в ней участвую (сам подал заявку, или моё предложение мест приняли).
        scope.launch {
            _myTrips.collect { list ->
                list.forEach { trip ->
                    val myId = AuthStore.userId
                    val committed = trip.status == GroupTripStatus.CONFIRMED &&
                        (trip.passengerId == myId || trip.offers.any { it.driverId == myId && it.status == GroupTripOfferStatus.ACCEPTED })
                    val departureMillis = trip.desiredAt?.let { TripReminderScheduler.parseDateTime(it) }
                    if (committed && departureMillis != null) {
                        TripReminderScheduler.schedule(trip.id, "${trip.fromCity} → ${trip.toCity}", departureMillis)
                    } else {
                        TripReminderScheduler.cancel(trip.id)
                    }
                }
            }
        }
    }

    fun refreshOpen() {
        scope.launch {
            runCatching { api.openGroupTrips() }
                .onSuccess { list -> _openTrips.value = list.map { it.toDomain() } }
                .onFailure { _lastError.value = "Не удалось загрузить заявки: ${it.message}" }
        }
    }

    fun refreshMine() {
        scope.launch {
            runCatching { api.myGroupTrips() }
                .onSuccess { list -> _myTrips.value = list.map { it.toDomain() } }
                .onFailure { _lastError.value = "Не удалось загрузить свои заявки: ${it.message}" }
        }
    }

    fun createTrip(fromCity: String, toCity: String, peopleCount: Int, desiredAt: String?, onResult: (GroupTrip?) -> Unit) {
        scope.launch {
            val trip = runCatching { api.createGroupTrip(CreateGroupTripRequest(fromCity, toCity, peopleCount, desiredAt)) }
                .onFailure { _lastError.value = "Не удалось создать заявку: ${it.message}" }
                .getOrNull()
                ?.toDomain()
            trip?.let { mergeInto(_myTrips, it) }
            onResult(trip)
        }
    }

    fun offerSeats(tripId: String, seats: Int) = act { api.offerGroupTripSeats(tripId, GroupTripOfferRequest(seats)) }
    fun acceptOffer(tripId: String, offerId: String) = act { api.acceptGroupTripOffer(tripId, offerId) }
    fun rejectOffer(tripId: String, offerId: String) = act { api.rejectGroupTripOffer(tripId, offerId) }
    fun cancelOffer(tripId: String, offerId: String) = act { api.cancelGroupTripOffer(tripId, offerId) }
    fun cancelTrip(tripId: String) = act { api.cancelGroupTrip(tripId) }

    private fun act(call: suspend () -> GroupTripDto) {
        scope.launch {
            runCatching { call() }
                .onSuccess { trip -> val domain = trip.toDomain(); mergeInto(_openTrips, domain); mergeInto(_myTrips, domain) }
                .onFailure { _lastError.value = "Не удалось обновить заявку: ${it.message}" }
        }
    }

    private fun mergeInto(flow: MutableStateFlow<List<GroupTrip>>, trip: GroupTrip) {
        flow.update { current ->
            if (current.none { it.id == trip.id }) return@update current
            current.map { if (it.id == trip.id) trip else it }
        }
    }

    private fun GroupTripDto.toDomain() = GroupTrip(
        id = id,
        passengerId = passengerId,
        passengerName = passengerName,
        passengerPhone = passengerPhone,
        fromCity = fromCity,
        toCity = toCity,
        peopleCount = peopleCount,
        seatsConfirmed = seatsConfirmed,
        desiredAt = desiredAt,
        status = runCatching { GroupTripStatus.valueOf(status.uppercase()) }.getOrDefault(GroupTripStatus.OPEN),
        createdAt = createdAt,
        offers = offers.orEmpty().map {
            GroupTripOffer(
                id = it.id,
                driverId = it.driverId,
                driverName = it.driverName,
                driverPhone = it.driverPhone,
                carMake = it.carMake,
                carPlate = it.carPlate,
                seatsOffered = it.seatsOffered,
                status = runCatching { GroupTripOfferStatus.valueOf(it.status.uppercase()) }.getOrDefault(GroupTripOfferStatus.PENDING)
            )
        }
    )
}
