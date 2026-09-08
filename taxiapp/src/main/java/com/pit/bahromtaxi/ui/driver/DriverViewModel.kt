package com.pit.bahromtaxi.ui.driver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.Ride
import kotlinx.coroutines.flow.StateFlow

class DriverViewModel : ViewModel() {

    var driverName by mutableStateOf("Демо-водитель")
        private set

    val online: StateFlow<Boolean> = RideRepository.driverOnline
    val rides: StateFlow<List<Ride>> = RideRepository.rides
    val commissionOwed: StateFlow<Double> = RideRepository.commissionOwed
    val commissionPaid: StateFlow<Double> = RideRepository.commissionPaid

    fun setOnline(value: Boolean) = RideRepository.setDriverOnline(value)

    fun accept(rideId: Long) = RideRepository.acceptRide(rideId, driverName)
    fun start(rideId: Long) = RideRepository.startRide(rideId)
    fun complete(rideId: Long) = RideRepository.completeRide(rideId)
    fun payCommission() = RideRepository.payCommission()
}
