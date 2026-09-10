package com.pit.bahromtaxi.ui.grouptrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pit.bahromtaxi.data.GroupTripRepository
import com.pit.bahromtaxi.domain.GroupTrip
import kotlinx.coroutines.flow.StateFlow

class GroupTripDriverViewModel : ViewModel() {

    val openTrips: StateFlow<List<GroupTrip>> = GroupTripRepository.openTrips
    val myTrips: StateFlow<List<GroupTrip>> = GroupTripRepository.myTrips
    val lastError: StateFlow<String?> = GroupTripRepository.lastError

    var seatsInput by mutableStateOf("2")

    init {
        GroupTripRepository.ensureSubscribed()
        GroupTripRepository.refreshOpen()
        GroupTripRepository.refreshMine()
    }

    fun offerSeats(tripId: String) {
        val seats = seatsInput.trim().toIntOrNull()?.coerceAtLeast(1) ?: return
        GroupTripRepository.offerSeats(tripId, seats)
    }

    fun cancelOffer(tripId: String, offerId: String) = GroupTripRepository.cancelOffer(tripId, offerId)
}
