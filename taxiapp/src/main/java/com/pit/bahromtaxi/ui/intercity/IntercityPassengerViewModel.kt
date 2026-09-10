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
    var pickupAddressInput by mutableStateOf("")
    var dropoffAddressInput by mutableStateOf("")

    /** Сначала ближайшие по времени отправления — переключается кнопкой сортировки на экране. */
    var sortByTimeSoonest by mutableStateOf(true)

    /** Поездка, для которой сейчас открыт диалог бронирования — null, если диалог закрыт. */
    var bookingTrip by mutableStateOf<IntercityTrip?>(null)
        private set
    var booking by mutableStateOf(false)
        private set

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

    fun openBooking(trip: IntercityTrip) {
        bookingTrip = trip
        seatsToBookInput = "1"
        pickupAddressInput = ""
        dropoffAddressInput = ""
    }

    fun closeBooking() {
        bookingTrip = null
    }

    fun confirmBooking(onDone: (Boolean) -> Unit) {
        val trip = bookingTrip ?: return
        val seats = seatsToBookInput.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
        booking = true
        IntercityRepository.bookSeats(
            tripId = trip.id,
            seats = seats,
            pickupAddress = pickupAddressInput.trim().ifBlank { null },
            dropoffAddress = dropoffAddressInput.trim().ifBlank { null }
        ) {
            booking = false
            if (it != null) bookingTrip = null
            onDone(it != null)
        }
    }

    fun cancelBooking(tripId: String) = IntercityRepository.cancelBooking(tripId)
}
