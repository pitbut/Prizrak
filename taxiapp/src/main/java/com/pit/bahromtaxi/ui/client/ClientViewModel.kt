package com.pit.bahromtaxi.ui.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.PricingEngine
import com.pit.bahromtaxi.domain.Ride
import kotlinx.coroutines.flow.StateFlow

class ClientViewModel : ViewModel() {

    var fromAddress by mutableStateOf("")
    var toAddress by mutableStateOf("")
    var distanceKm by mutableStateOf(5.0)
        private set
    var durationMin by mutableStateOf(14.0)
        private set

    var activeRideId by mutableStateOf<Long?>(null)
        private set

    val rides: StateFlow<List<Ride>> = RideRepository.rides

    val estimate: PriceBreakdown
        get() = PricingEngine.calculate(distanceKm, durationMin, RideRepository.currentDemandFactor())

    fun setDistance(km: Double) {
        distanceKm = km
        // грубая оценка времени в пути по расстоянию — в реальном приложении её вернёт
        // маршрутизатор картографического сервиса вместе с расстоянием
        durationMin = km * 2.2 + 3.0
    }

    fun order() {
        if (fromAddress.isBlank() || toAddress.isBlank()) return
        val ride = RideRepository.createOrder(fromAddress, toAddress, distanceKm, durationMin)
        activeRideId = ride.id
    }

    fun resetOrder() {
        activeRideId = null
        fromAddress = ""
        toAddress = ""
    }
}
