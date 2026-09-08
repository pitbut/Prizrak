package com.pit.bahromtaxi.ui.driver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.network.AuthStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DriverViewModel : ViewModel() {

    var nameInput by mutableStateOf("")
    var carMake by mutableStateOf("")
    var carColor by mutableStateOf("")
    var carPlate by mutableStateOf("")
    var clickHandle by mutableStateOf("")
    var registering by mutableStateOf(false)
        private set

    val isRegistered: Boolean get() = AuthStore.isRegisteredAs("driver")
    val driverId: String? get() = AuthStore.userId

    val online: StateFlow<Boolean> = RideRepository.driverOnline
    val rides: StateFlow<List<Ride>> = RideRepository.rides
    val commissionOwed: StateFlow<Double> = RideRepository.commissionOwed
    val commissionPaid: StateFlow<Double> = RideRepository.commissionPaid
    val lastError: StateFlow<String?> = RideRepository.lastError

    fun register(onDone: () -> Unit) {
        val name = nameInput.trim().ifBlank { "Водитель" }
        registering = true
        viewModelScope.launch {
            RideRepository.register(
                role = "driver",
                name = name,
                carMake = carMake.trim().ifBlank { null },
                carColor = carColor.trim().ifBlank { null },
                carPlate = carPlate.trim().ifBlank { null },
                clickHandle = clickHandle.trim().ifBlank { null }
            )
            RideRepository.refreshCommission()
            registering = false
            onDone()
        }
    }

    fun setOnline(value: Boolean) = RideRepository.setDriverOnline(value)
    fun accept(rideId: String) = RideRepository.acceptRide(rideId)
    fun start(rideId: String) = RideRepository.startRide(rideId)
    fun complete(rideId: String) = RideRepository.completeRide(rideId)
    fun payCommission() = RideRepository.payCommission()
}
