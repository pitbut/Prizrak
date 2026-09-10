package com.pit.bahromtaxi.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.network.AuthStore
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var deleting by mutableStateOf(false)
        private set

    var nameInput by mutableStateOf("")
    var phoneInput by mutableStateOf("")
    var role by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        loading = true
        error = null
        viewModelScope.launch {
            val profile = RideRepository.getProfile()
            if (profile != null) {
                nameInput = profile.name
                phoneInput = profile.phone ?: ""
                role = profile.role
                email = profile.email
            } else {
                error = "Не удалось загрузить профиль"
            }
            loading = false
        }
    }

    fun save() {
        val name = nameInput.trim()
        val phone = phoneInput.trim()
        if (phone.isNotEmpty() && phone.length < 7) {
            error = "Проверьте номер телефона"
            return
        }
        error = null
        saving = true
        saved = false
        viewModelScope.launch {
            val profile = RideRepository.updateProfile(
                name.ifBlank { null },
                phone.ifBlank { null }
            )
            if (profile != null) {
                AuthStore.updateActiveName(profile.name)
                saved = true
            } else {
                error = "Не удалось сохранить"
            }
            saving = false
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        error = null
        deleting = true
        viewModelScope.launch {
            val success = RideRepository.deleteAccount()
            deleting = false
            if (success) onDeleted() else error = "Не удалось удалить аккаунт"
        }
    }
}
