package com.pit.bahromtaxi.ui.driver

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.auth.EmailAuthClient
import com.pit.bahromtaxi.auth.PhoneAuthClient
import com.pit.bahromtaxi.auth.PhoneCodeResult
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.network.AuthStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class DriverAuthStep { PHONE, CODE, VERIFY_EMAIL, PROFILE }
enum class DriverEmailAuthMode { REGISTER, LOGIN }

class DriverViewModel : ViewModel() {

    var authStep by mutableStateOf(DriverAuthStep.PHONE)
    var phoneInput by mutableStateOf("+998")
    var codeInput by mutableStateOf("")
    var authLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    private var verificationId: String? = null
    private var firebaseIdToken: String? = null

    var showEmailForm by mutableStateOf(false)
    var emailAuthMode by mutableStateOf(DriverEmailAuthMode.REGISTER)
    var emailInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")

    fun toggleEmailAuthMode() {
        emailAuthMode = if (emailAuthMode == DriverEmailAuthMode.REGISTER) DriverEmailAuthMode.LOGIN else DriverEmailAuthMode.REGISTER
        authError = null
    }

    fun continueWithEmail() {
        val email = emailInput.trim()
        val password = passwordInput
        if (email.isBlank() || password.length < 6) {
            authError = "Введите email и пароль (минимум 6 символов)"
            return
        }
        authError = null
        authLoading = true
        val mode = emailAuthMode
        viewModelScope.launch {
            val result = if (mode == DriverEmailAuthMode.REGISTER) {
                runCatching { EmailAuthClient.register(email, password) }
            } else {
                runCatching { EmailAuthClient.signIn(email, password) }
            }
            result
                .onSuccess { token ->
                    firebaseIdToken = token
                    authStep = if (mode == DriverEmailAuthMode.REGISTER) DriverAuthStep.VERIFY_EMAIL else DriverAuthStep.PROFILE
                }
                .onFailure {
                    authError = if (mode == DriverEmailAuthMode.REGISTER) "Не удалось зарегистрироваться: ${it.message}"
                    else "Не удалось войти: ${it.message}"
                }
            authLoading = false
        }
    }

    fun checkEmailVerified() {
        authError = null
        authLoading = true
        viewModelScope.launch {
            runCatching { EmailAuthClient.refreshEmailVerified() }
                .onSuccess { verified ->
                    if (verified) authStep = DriverAuthStep.PROFILE
                    else authError = "Email ещё не подтверждён — перейдите по ссылке в письме и попробуйте снова."
                }
                .onFailure { authError = "Не удалось проверить: ${it.message}" }
            authLoading = false
        }
    }

    fun resendVerificationEmail() {
        authLoading = true
        viewModelScope.launch {
            runCatching { EmailAuthClient.resendVerificationEmail() }
            authLoading = false
        }
    }

    var passwordResetSent by mutableStateOf(false)
        private set

    fun sendPasswordReset() {
        val email = emailInput.trim()
        if (email.isBlank()) {
            authError = "Введите email, на который прислать ссылку"
            return
        }
        authError = null
        authLoading = true
        viewModelScope.launch {
            runCatching { EmailAuthClient.sendPasswordReset(email) }
                .onSuccess { passwordResetSent = true }
                .onFailure { authError = "Не удалось отправить письмо: ${it.message}" }
            authLoading = false
        }
    }

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

    fun sendCode(activity: Activity) {
        authError = null
        authLoading = true
        viewModelScope.launch {
            runCatching { PhoneAuthClient.sendCode(activity, phoneInput.trim()) }
                .onSuccess { result ->
                    when (result) {
                        is PhoneCodeResult.CodeSent -> {
                            verificationId = result.verificationId
                            authStep = DriverAuthStep.CODE
                        }
                        is PhoneCodeResult.AutoVerified -> {
                            runCatching { PhoneAuthClient.signIn(result.credential) }
                                .onSuccess { token -> firebaseIdToken = token; authStep = DriverAuthStep.PROFILE }
                                .onFailure { authError = "Не удалось подтвердить номер: ${it.message}" }
                        }
                    }
                }
                .onFailure { authError = "Не удалось отправить код: ${it.message}" }
            authLoading = false
        }
    }

    fun confirmCode() {
        val vId = verificationId ?: return
        authError = null
        authLoading = true
        viewModelScope.launch {
            runCatching { PhoneAuthClient.confirmCode(vId, codeInput.trim()) }
                .onSuccess { token -> firebaseIdToken = token; authStep = DriverAuthStep.PROFILE }
                .onFailure { authError = "Неверный код: ${it.message}" }
            authLoading = false
        }
    }

    fun register(onDone: () -> Unit) {
        val token = firebaseIdToken ?: return
        val name = nameInput.trim().ifBlank { "Водитель" }
        registering = true
        viewModelScope.launch {
            val success = RideRepository.register(
                firebaseIdToken = token,
                role = "driver",
                name = name,
                carMake = carMake.trim().ifBlank { null },
                carColor = carColor.trim().ifBlank { null },
                carPlate = carPlate.trim().ifBlank { null },
                clickHandle = clickHandle.trim().ifBlank { null }
            )
            registering = false
            if (success) {
                RideRepository.refreshCommission()
                onDone()
            }
        }
    }

    fun setOnline(value: Boolean) = RideRepository.setDriverOnline(value)
    fun accept(rideId: String) = RideRepository.acceptRide(rideId)
    fun start(rideId: String) = RideRepository.startRide(rideId)
    fun complete(rideId: String) = RideRepository.completeRide(rideId)
    fun payCommission() = RideRepository.payCommission()
}
