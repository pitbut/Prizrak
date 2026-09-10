package com.pit.bahromtaxi.data

import com.pit.bahromtaxi.domain.IntercityBooking
import com.pit.bahromtaxi.domain.IntercityStatus
import com.pit.bahromtaxi.domain.IntercityTrip
import com.pit.bahromtaxi.domain.TripPointMode
import com.pit.bahromtaxi.network.ApiClient
import com.pit.bahromtaxi.network.BookSeatsRequest
import com.pit.bahromtaxi.network.CreateIntercityTripRequest
import com.pit.bahromtaxi.network.IntercityTripDto
import com.pit.bahromtaxi.network.AuthStore
import com.pit.bahromtaxi.notify.TripReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Межгородние поездки "по сбору мест" (см. domain/IntercityTrip.kt). Отдельный репозиторий
 * от RideRepository — модель другая (1 водитель : N пассажиров), но живые обновления мест
 * приходят по тому же WebSocket-соединению, которое держит RideRepository, поэтому здесь
 * только подписка на RideRepository.intercityTripEvents, без своего сокета.
 */
object IntercityRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = ApiClient.service

    /** Открытые поездки по направлению — для пассажира, который ищет попутку. */
    private val _browseTrips = MutableStateFlow<List<IntercityTrip>>(emptyList())
    val browseTrips: StateFlow<List<IntercityTrip>> = _browseTrips

    /** Свои поездки: у водителя — созданные им, у пассажира — забронированные им (отдаёт сервер по роли). */
    private val _myTrips = MutableStateFlow<List<IntercityTrip>>(emptyList())
    val myTrips: StateFlow<List<IntercityTrip>> = _myTrips

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var subscribed = false

    /** Подписывается на живые обновления мест по WebSocket — вызывать один раз после подключения сокета. */
    fun ensureSubscribed() {
        if (subscribed) return
        subscribed = true
        scope.launch {
            RideRepository.intercityTripEvents.collect { dto ->
                val trip = dto.toDomain()
                mergeInto(_browseTrips, trip)
                mergeInto(_myTrips, trip)
            }
        }
        // Будильник за час до отправления — только для поездок, в которых я участвую (свои
        // как водитель, забронированные места как пассажир), и только пока не отменена/не доехали.
        scope.launch {
            _myTrips.collect { list ->
                list.forEach { trip ->
                    val committed = trip.status in setOf(IntercityStatus.OPEN, IntercityStatus.FULL, IntercityStatus.IN_PROGRESS) &&
                        (trip.driverId == AuthStore.userId || trip.myBookedSeats > 0)
                    val departureMillis = trip.scheduledAt?.let { TripReminderScheduler.parseDateTime(it) }
                    if (committed && departureMillis != null) {
                        TripReminderScheduler.schedule(trip.id, "${trip.fromCity} → ${trip.toCity}", departureMillis)
                    } else {
                        TripReminderScheduler.cancel(trip.id)
                    }
                }
            }
        }
    }

    fun refreshBrowse(fromCity: String? = null, toCity: String? = null) {
        scope.launch {
            runCatching { api.intercityTrips(fromCity?.trim()?.ifBlank { null }, toCity?.trim()?.ifBlank { null }) }
                .onSuccess { list -> _browseTrips.value = list.map { it.toDomain() } }
                .onFailure { _lastError.value = "Не удалось загрузить поездки: ${it.message}" }
        }
    }

    fun refreshMine() {
        scope.launch {
            runCatching { api.myIntercityTrips() }
                .onSuccess { list -> _myTrips.value = list.map { it.toDomain() } }
                .onFailure { _lastError.value = "Не удалось загрузить свои поездки: ${it.message}" }
        }
    }

    fun createTrip(
        fromCity: String,
        toCity: String,
        totalSeats: Int,
        pricePerSeat: Double,
        scheduledAt: String?,
        pickupMode: TripPointMode,
        dropoffMode: TripPointMode,
        pickupPoint: String?,
        dropoffPoint: String?,
        onResult: (IntercityTrip?) -> Unit
    ) {
        scope.launch {
            val trip = runCatching {
                api.createIntercityTrip(
                    CreateIntercityTripRequest(
                        fromCity, toCity, totalSeats, pricePerSeat, scheduledAt,
                        pickupMode = pickupMode.name.lowercase(),
                        dropoffMode = dropoffMode.name.lowercase(),
                        pickupPoint = pickupPoint,
                        dropoffPoint = dropoffPoint
                    )
                )
            }
                .onFailure { _lastError.value = "Не удалось создать поездку: ${it.message}" }
                .getOrNull()
                ?.toDomain()
            trip?.let { mergeInto(_myTrips, it) }
            onResult(trip)
        }
    }

    fun bookSeats(
        tripId: String,
        seats: Int,
        pickupAddress: String?,
        dropoffAddress: String?,
        onResult: (IntercityTrip?) -> Unit
    ) {
        scope.launch {
            val trip = runCatching { api.bookIntercitySeats(tripId, BookSeatsRequest(seats, pickupAddress, dropoffAddress)) }
                .onFailure { _lastError.value = "Не удалось забронировать места: ${it.message}" }
                .getOrNull()
                ?.toDomain()
            trip?.let { mergeInto(_browseTrips, it); mergeInto(_myTrips, it) }
            onResult(trip)
        }
    }

    fun cancelBooking(tripId: String) = act { api.cancelIntercityBooking(tripId) }
    fun depart(tripId: String) = act { api.departIntercityTrip(tripId) }
    fun complete(tripId: String) = act { api.completeIntercityTrip(tripId) }
    fun cancelTrip(tripId: String) = act { api.cancelIntercityTrip(tripId) }

    private fun act(call: suspend () -> IntercityTripDto) {
        scope.launch {
            runCatching { call() }
                .onSuccess { trip -> val domain = trip.toDomain(); mergeInto(_browseTrips, domain); mergeInto(_myTrips, domain) }
                .onFailure { _lastError.value = "Не удалось обновить поездку: ${it.message}" }
        }
    }

    private fun mergeInto(flow: MutableStateFlow<List<IntercityTrip>>, trip: IntercityTrip) {
        flow.update { current ->
            if (current.none { it.id == trip.id }) return@update current
            current.map { if (it.id == trip.id) trip else it }
        }
    }

    private fun IntercityTripDto.toDomain() = IntercityTrip(
        id = id,
        driverId = driverId,
        driverName = driverName,
        driverPhone = driverPhone,
        driverCarMake = driverCarMake,
        driverCarPlate = driverCarPlate,
        fromCity = fromCity,
        toCity = toCity,
        totalSeats = totalSeats,
        bookedSeats = bookedSeats,
        pricePerSeat = pricePerSeat,
        scheduledAt = scheduledAt,
        status = runCatching { IntercityStatus.valueOf(status.uppercase()) }.getOrDefault(IntercityStatus.OPEN),
        createdAt = createdAt,
        myBookedSeats = myBookedSeats ?: 0,
        pickupMode = runCatching { TripPointMode.valueOf((pickupMode ?: "single").uppercase()) }.getOrDefault(TripPointMode.SINGLE),
        dropoffMode = runCatching { TripPointMode.valueOf((dropoffMode ?: "single").uppercase()) }.getOrDefault(TripPointMode.SINGLE),
        pickupPoint = pickupPoint,
        dropoffPoint = dropoffPoint,
        bookings = bookings.orEmpty().map {
            IntercityBooking(
                id = it.id,
                passengerId = it.passengerId,
                passengerName = it.passengerName,
                passengerPhone = it.passengerPhone,
                seats = it.seats,
                pickupAddress = it.pickupAddress,
                dropoffAddress = it.dropoffAddress
            )
        }
    )
}
