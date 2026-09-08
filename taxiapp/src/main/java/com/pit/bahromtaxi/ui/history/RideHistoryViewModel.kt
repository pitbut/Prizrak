package com.pit.bahromtaxi.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.Ride
import kotlinx.coroutines.launch

class RideHistoryViewModel : ViewModel() {

    var loading by mutableStateOf(true)
        private set
    var rides by mutableStateOf<List<Ride>>(emptyList())
        private set

    init {
        load()
    }

    fun load() {
        loading = true
        viewModelScope.launch {
            rides = RideRepository.getRideHistory()
            loading = false
        }
    }
}
