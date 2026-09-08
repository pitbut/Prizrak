package com.pit.bahromtaxi.data

import com.pit.bahromtaxi.domain.PricingEngine
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Заглушка сервера агрегатора для песочницы. В реальном сервисе матчинг заказов и
 * водителей, а также расчёт цены, происходят на бэкенде — здесь это общее состояние
 * в памяти процесса, чтобы режимы "Пассажир" и "Водитель" могли обмениваться заказами
 * прямо на одном устройстве.
 */
object RideRepository {

    private var nextId = 1L

    private val _rides = MutableStateFlow<List<Ride>>(emptyList())
    val rides: StateFlow<List<Ride>> = _rides

    private val _driverOnline = MutableStateFlow(false)
    val driverOnline: StateFlow<Boolean> = _driverOnline

    private val _commissionOwed = MutableStateFlow(0.0)
    val commissionOwed: StateFlow<Double> = _commissionOwed

    private val _commissionPaid = MutableStateFlow(0.0)
    val commissionPaid: StateFlow<Double> = _commissionPaid

    fun setDriverOnline(online: Boolean) {
        _driverOnline.value = online
    }

    /**
     * Коэффициент спроса считает алгоритм от соотношения активных заказов и свободных
     * водителей — так же, как повышающий коэффициент у реальных агрегаторов. Ни
     * пассажир, ни водитель на него не влияют напрямую.
     */
    fun currentDemandFactor(): Double {
        val pending = _rides.value.count { it.status == RideStatus.SEARCHING }
        val freeDrivers = if (_driverOnline.value) 1 else 0
        val ratio = if (freeDrivers == 0) (pending + 1).toDouble() else pending.toDouble() / freeDrivers
        val factor = 1.0 + 0.2 * ratio
        return (factor.coerceIn(1.0, 2.5) * 10.0).let(Math::round) / 10.0
    }

    fun createOrder(fromAddress: String, toAddress: String, distanceKm: Double, durationMin: Double): Ride {
        val demand = currentDemandFactor()
        val price = PricingEngine.calculate(distanceKm, durationMin, demand)
        val ride = Ride(
            id = nextId++,
            fromAddress = fromAddress,
            toAddress = toAddress,
            distanceKm = distanceKm,
            durationMin = durationMin,
            price = price,
            status = RideStatus.SEARCHING
        )
        _rides.update { it + ride }
        return ride
    }

    fun acceptRide(rideId: Long, driverName: String) {
        updateRide(rideId) { it.copy(status = RideStatus.ACCEPTED, driverName = driverName) }
    }

    fun startRide(rideId: Long) {
        updateRide(rideId) { it.copy(status = RideStatus.IN_PROGRESS) }
    }

    fun completeRide(rideId: Long) {
        val ride = _rides.value.find { it.id == rideId } ?: return
        updateRide(rideId) { it.copy(status = RideStatus.COMPLETED) }
        _commissionOwed.update { it + ride.price.commission }
    }

    fun cancelRide(rideId: Long) {
        updateRide(rideId) { it.copy(status = RideStatus.CANCELLED) }
    }

    fun payCommission() {
        val owed = _commissionOwed.value
        if (owed <= 0.0) return
        _commissionPaid.update { it + owed }
        _commissionOwed.value = 0.0
    }

    private fun updateRide(rideId: Long, transform: (Ride) -> Ride) {
        _rides.update { list -> list.map { if (it.id == rideId) transform(it) else it } }
    }
}
