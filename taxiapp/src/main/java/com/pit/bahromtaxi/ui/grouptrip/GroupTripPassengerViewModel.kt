package com.pit.bahromtaxi.ui.grouptrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pit.bahromtaxi.data.GroupTripRepository
import com.pit.bahromtaxi.domain.GroupTrip
import kotlinx.coroutines.flow.StateFlow

class GroupTripPassengerViewModel : ViewModel() {

    var fromCityInput by mutableStateOf("")
    var toCityInput by mutableStateOf("")
    var peopleCountInput by mutableStateOf("2")
    var desiredAtInput by mutableStateOf("")
    var creating by mutableStateOf(false)
        private set
    var createError by mutableStateOf<String?>(null)
        private set

    val myTrips: StateFlow<List<GroupTrip>> = GroupTripRepository.myTrips
    val lastError: StateFlow<String?> = GroupTripRepository.lastError

    init {
        GroupTripRepository.ensureSubscribed()
        GroupTripRepository.refreshMine()
    }

    fun createTrip() {
        val from = fromCityInput.trim()
        val to = toCityInput.trim()
        val people = peopleCountInput.trim().toIntOrNull()
        if (from.isBlank() || to.isBlank()) {
            createError = "Укажите город отправления и назначения"
            return
        }
        if (people == null || people < 2) {
            createError = "Укажите число людей (минимум 2 — для одного используйте обычный заказ)"
            return
        }
        createError = null
        creating = true
        GroupTripRepository.createTrip(from, to, people, desiredAtInput.trim().ifBlank { null }) {
            creating = false
            if (it != null) {
                fromCityInput = ""
                toCityInput = ""
                peopleCountInput = "2"
                desiredAtInput = ""
            } else {
                createError = "Не удалось создать заявку"
            }
        }
    }

    fun acceptOffer(tripId: String, offerId: String) = GroupTripRepository.acceptOffer(tripId, offerId)
    fun rejectOffer(tripId: String, offerId: String) = GroupTripRepository.rejectOffer(tripId, offerId)
    fun cancelTrip(tripId: String) = GroupTripRepository.cancelTrip(tripId)
}
