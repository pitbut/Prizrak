package com.pit.bahromtaxi.ui.intercity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pit.bahromtaxi.data.IntercityRepository
import com.pit.bahromtaxi.domain.IntercityTrip
import kotlinx.coroutines.flow.StateFlow

class IntercityPassengerViewModel : ViewModel() {

    var fromCityInput by mutableStateOf("")
    var toCityInput by mutableStateOf("")
    var seatsToBookInput by mutableStateOf("1")

    val browseTrips: StateFlow<List<IntercityTrip>> = IntercityRepository.browseTrips
    val myTrips: StateFlow<List<IntercityTrip>> = IntercityRepository.myTrips
    val lastError: StateFlow<String?> = IntercityRepository.lastError

    init {
        IntercityRepository.ensureSubscribed()
        search()
        IntercityRepository.refreshMine()
    }

    fun search() {
        IntercityRepository.refreshBrowse(fromCityInput, toCityInput)
    }

    fun book(tripId: String, onDone: (Boolean) -> Unit) {
        val seats = seatsToBookInput.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
        IntercityRepository.bookSeats(tripId, seats) { onDone(it != null) }
    }

    fun cancelBooking(tripId: String) = IntercityRepository.cancelBooking(tripId)
}
