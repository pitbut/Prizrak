package com.pit.bahromtaxi.ui.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.PaymentMethod
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.PricingEngine
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.maps.OsrmClient
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.PlacePoint
import com.pit.bahromtaxi.network.AuthStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientViewModel : ViewModel() {

    var nameInput by mutableStateOf("")
    var registering by mutableStateOf(false)
        private set

    var fromPlace by mutableStateOf<PlacePoint?>(null)
        private set
    var toPlace by mutableStateOf<PlacePoint?>(null)
        private set

    var pickTarget by mutableStateOf<PickTarget?>(null)
        private set

    var route by mutableStateOf<OsrmClient.RouteResult?>(null)
        private set
    var routeLoading by mutableStateOf(false)
        private set
    var routeError by mutableStateOf<String?>(null)
        private set

    var paymentMethod by mutableStateOf(PaymentMethod.CASH)

    var activeRideId by mutableStateOf<String?>(null)
        private set

    val isRegistered: Boolean get() = AuthStore.isRegisteredAs("passenger")

    val rides: StateFlow<List<Ride>> = RideRepository.rides
    val lastError: StateFlow<String?> = RideRepository.lastError

    /** Оценочная цена по реальному маршруту — окончательную (с учётом спроса) вернёт backend. */
    val estimate: PriceBreakdown?
        get() = route?.let { PricingEngine.calculate(it.distanceKm, it.durationMin, demandFactor = 1.0) }

    fun startPicking(target: PickTarget) {
        pickTarget = target
    }

    fun setPointFromMap(point: PlacePoint) {
        when (pickTarget) {
            PickTarget.FROM -> chooseFromPlace(point)
            PickTarget.TO -> chooseToPlace(point)
            null -> return
        }
        pickTarget = null
    }

    fun chooseFromPlace(place: PlacePoint) {
        fromPlace = place
        route = null
        fetchRouteIfReady()
    }

    fun chooseToPlace(place: PlacePoint) {
        toPlace = place
        route = null
        fetchRouteIfReady()
    }

    private fun fetchRouteIfReady() {
        val from = fromPlace ?: return
        val to = toPlace ?: return
        routeError = null
        routeLoading = true
        viewModelScope.launch {
            OsrmClient.fetchRoute(from.coordinate, to.coordinate)
                .onSuccess { route = it }
                .onFailure { routeError = "Не удалось построить маршрут: ${it.message}" }
            routeLoading = false
        }
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
        val from = fromPlace ?: return
        val to = toPlace ?: return
        val r = route ?: return
        RideRepository.createOrder(from.address, to.address, r.distanceKm, r.durationMin, paymentMethod) { ride ->
            activeRideId = ride?.id
        }
    }

    fun resetOrder() {
        activeRideId = null
        fromPlace = null
        toPlace = null
        route = null
        routeError = null
        pickTarget = null
    }
}
