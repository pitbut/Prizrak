package com.pit.bahromtaxi.ui.intercity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pit.bahromtaxi.data.IntercityRepository
import com.pit.bahromtaxi.domain.IntercityTrip
import com.pit.bahromtaxi.domain.TripPointMode
import kotlinx.coroutines.flow.StateFlow

class IntercityDriverViewModel : ViewModel() {

    var fromCityInput by mutableStateOf("")
    var toCityInput by mutableStateOf("")
    var totalSeatsInput by mutableStateOf("4")
    var pricePerSeatInput by mutableStateOf("")
    var scheduledAtInput by mutableStateOf("")
    /** SINGLE — все садятся/выходят в одной точке (fromCityInput/toCityInput и есть эта точка).
     *  COLLECT — водитель объезжает адреса, которые пассажиры укажут при бронировании. */
    var pickupMode by mutableStateOf(TripPointMode.SINGLE)
    var dropoffMode by mutableStateOf(TripPointMode.SINGLE)
    var creating by mutableStateOf(false)
        private set
    var createError by mutableStateOf<String?>(null)
        private set

    val myTrips: StateFlow<List<IntercityTrip>> = IntercityRepository.myTrips
    val lastError: StateFlow<String?> = IntercityRepository.lastError

    init {
        IntercityRepository.ensureSubscribed()
        IntercityRepository.refreshMine()
    }

    fun createTrip() {
        val from = fromCityInput.trim()
        val to = toCityInput.trim()
        val seats = totalSeatsInput.trim().toIntOrNull()
        val price = pricePerSeatInput.trim().toDoubleOrNull()
        if (from.isBlank() || to.isBlank()) {
            createError = "Укажите город отправления и назначения"
            return
        }
        if (seats == null || seats < 1) {
            createError = "Укажите число мест (минимум 1)"
            return
        }
        if (price == null || price <= 0) {
            createError = "Укажите цену за место"
            return
        }
        createError = null
        creating = true
        IntercityRepository.createTrip(
            fromCity = from,
            toCity = to,
            totalSeats = seats,
            pricePerSeat = price,
            scheduledAt = scheduledAtInput.trim().ifBlank { null },
            pickupMode = pickupMode,
            dropoffMode = dropoffMode,
            pickupPoint = if (pickupMode == TripPointMode.SINGLE) from else null,
            dropoffPoint = if (dropoffMode == TripPointMode.SINGLE) to else null
        ) {
            creating = false
            if (it != null) {
                fromCityInput = ""
                toCityInput = ""
                totalSeatsInput = "4"
                pricePerSeatInput = ""
                scheduledAtInput = ""
                pickupMode = TripPointMode.SINGLE
                dropoffMode = TripPointMode.SINGLE
            } else {
                createError = "Не удалось создать поездку"
            }
        }
    }

    fun depart(tripId: String) = IntercityRepository.depart(tripId)
    fun complete(tripId: String) = IntercityRepository.complete(tripId)
    fun cancelTrip(tripId: String) = IntercityRepository.cancelTrip(tripId)
}
