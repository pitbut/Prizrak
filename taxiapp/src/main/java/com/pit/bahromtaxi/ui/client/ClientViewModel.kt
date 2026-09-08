package com.pit.bahromtaxi.ui.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.PricingEngine
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.network.AuthStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientViewModel : ViewModel() {

    var nameInput by mutableStateOf("")
    var registering by mutableStateOf(false)
        private set

    var fromAddress by mutableStateOf("")
    var toAddress by mutableStateOf("")
    var distanceKm by mutableStateOf(5.0)
        private set
    var durationMin by mutableStateOf(14.0)
        private set

    var activeRideId by mutableStateOf<String?>(null)
        private set

    val isRegistered: Boolean get() = AuthStore.isRegisteredAs("passenger")

    val rides: StateFlow<List<Ride>> = RideRepository.rides
    val lastError: StateFlow<String?> = RideRepository.lastError

    /** Оценочная цена до заказа — окончательную (с учётом спроса на сервере) вернёт backend. */
    val estimate: PriceBreakdown
        get() = PricingEngine.calculate(distanceKm, durationMin, demandFactor = 1.0)

    fun setDistance(km: Double) {
        distanceKm = km
        // грубая оценка времени в пути по расстоянию — в реальном приложении её вернёт
        // маршрутизатор картографического сервиса вместе с расстоянием
        durationMin = km * 2.2 + 3.0
    }

    fun register(onDone: () -> Unit) {
        val name = nameInput.trim().ifBlank { "Пассажир" }
        registering = true
        viewModelScope.launch {
            RideRepository.register("passenger", name)
            registering = false
            onDone()
        }
    }

    fun order() {
        if (fromAddress.isBlank() || toAddress.isBlank()) return
        RideRepository.createOrder(fromAddress, toAddress, distanceKm, durationMin) { ride ->
            activeRideId = ride?.id
        }
    }

    fun resetOrder() {
        activeRideId = null
        fromAddress = ""
        toAddress = ""
    }
}
