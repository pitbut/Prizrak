package com.pit.bahromtaxi.ui.client

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
import com.pit.bahromtaxi.domain.OrderType
import com.pit.bahromtaxi.domain.PaymentMethod
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.PricingEngine
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.maps.OsrmClient
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.PlacePoint
import com.pit.bahromtaxi.network.AuthStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AuthStep { PHONE, CODE, VERIFY_EMAIL, PROFILE }
enum class EmailAuthMode { REGISTER, LOGIN }

class ClientViewModel : ViewModel() {

    var authStep by mutableStateOf(AuthStep.PHONE)
    var phoneInput by mutableStateOf("+998")
    var codeInput by mutableStateOf("")
    var authLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    private var verificationId: String? = null
    private var firebaseIdToken: String? = null

    var showEmailForm by mutableStateOf(false)
    var emailAuthMode by mutableStateOf(EmailAuthMode.REGISTER)
    var emailInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")

    fun toggleEmailAuthMode() {
        emailAuthMode = if (emailAuthMode == EmailAuthMode.REGISTER) EmailAuthMode.LOGIN else EmailAuthMode.REGISTER
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
            val result = if (mode == EmailAuthMode.REGISTER) {
                runCatching { EmailAuthClient.register(email, password) }
            } else {
                runCatching { EmailAuthClient.signIn(email, password) }
            }
            result
                .onSuccess { token ->
                    firebaseIdToken = token
                    authStep = if (mode == EmailAuthMode.REGISTER) AuthStep.VERIFY_EMAIL else AuthStep.PROFILE
                }
                .onFailure {
                    authError = if (mode == EmailAuthMode.REGISTER) "Не удалось зарегистрироваться: ${it.message}"
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
                    if (verified) authStep = AuthStep.PROFILE
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
    var registering by mutableStateOf(false)
        private set

    var fromPlace by mutableStateOf<PlacePoint?>(null)
        private set
    var toPlace by mutableStateOf<PlacePoint?>(null)
        private set

    var pickTarget by mutableStateOf<PickTarget?>(null)
        private set

    var route by mutableStateOf<OsrmClient.RouteResult?>(null)
        private set
    var routeLoading by mutableStateOf(false)
        private set
    var routeError by mutableStateOf<String?>(null)
        private set

    var paymentMethod by mutableStateOf(PaymentMethod.CASH)

    var orderType by mutableStateOf(OrderType.RIDE)
    var senderPhoneInput by mutableStateOf("")
    var receiverPhoneInput by mutableStateOf("")
    var orderError by mutableStateOf<String?>(null)
        private set

    var activeRideId by mutableStateOf<String?>(null)
        private set

    val isRegistered: Boolean get() = AuthStore.isRegisteredAs("passenger")

    val rides: StateFlow<List<Ride>> = RideRepository.rides
    val lastError: StateFlow<String?> = RideRepository.lastError

    /** Оценочная цена по реальному маршруту — окончательную (с учётом спроса) вернёт backend. */
    val estimate: PriceBreakdown?
        get() = route?.let { PricingEngine.calculate(it.distanceKm, it.durationMin, demandFactor = 1.0) }

    fun sendCode(activity: Activity) {
        authError = null
        authLoading = true
        viewModelScope.launch {
            runCatching { PhoneAuthClient.sendCode(activity, phoneInput.trim()) }
                .onSuccess { result ->
                    when (result) {
                        is PhoneCodeResult.CodeSent -> {
                            verificationId = result.verificationId
                            authStep = AuthStep.CODE
                        }
                        is PhoneCodeResult.AutoVerified -> {
                            runCatching { PhoneAuthClient.signIn(result.credential) }
                                .onSuccess { token -> firebaseIdToken = token; authStep = AuthStep.PROFILE }
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
                .onSuccess { token -> firebaseIdToken = token; authStep = AuthStep.PROFILE }
                .onFailure { authError = "Неверный код: ${it.message}" }
            authLoading = false
        }
    }

    fun startPicking(target: PickTarget) {
        pickTarget = target
    }

    fun setPointFromMap(point: PlacePoint) {
        when (pickTarget) {
            PickTarget.FROM -> chooseFromPlace(point)
            PickTarget.TO -> chooseToPlace(point)
            null -> return
        }
        pickTarget = null
    }

    fun chooseFromPlace(place: PlacePoint) {
        fromPlace = place
        route = null
        fetchRouteIfReady()
    }

    fun chooseToPlace(place: PlacePoint) {
        toPlace = place
        route = null
        fetchRouteIfReady()
    }

    private fun fetchRouteIfReady() {
        val from = fromPlace ?: return
        val to = toPlace ?: return
        routeError = null
        routeLoading = true
        viewModelScope.launch {
            OsrmClient.fetchRoute(from.coordinate, to.coordinate)
                .onSuccess { route = it }
                .onFailure { routeError = "Не удалось построить маршрут: ${it.message}" }
            routeLoading = false
        }
    }

    fun register(onDone: () -> Unit) {
        val token = firebaseIdToken ?: return
        val name = nameInput.trim().ifBlank { "Пассажир" }
        registering = true
        viewModelScope.launch {
            val success = RideRepository.register(token, "passenger", name)
            registering = false
            if (success) onDone()
        }
    }

    fun order() {
        val from = fromPlace ?: return
        val to = toPlace ?: return
        val r = route ?: return
        if (orderType == OrderType.DELIVERY) {
            if (senderPhoneInput.isBlank() || receiverPhoneInput.isBlank()) {
                orderError = "Укажите телефон отправителя и получателя"
                return
            }
        }
        orderError = null
        RideRepository.createOrder(
            fromAddress = from.address,
            toAddress = to.address,
            distanceKm = r.distanceKm,
            durationMin = r.durationMin,
            paymentMethod = paymentMethod,
            orderType = orderType,
            senderPhone = if (orderType == OrderType.DELIVERY) senderPhoneInput.trim() else null,
            receiverPhone = if (orderType == OrderType.DELIVERY) receiverPhoneInput.trim() else null
        ) { ride ->
            activeRideId = ride?.id
        }
    }

    fun resetOrder() {
        activeRideId = null
        fromPlace = null
        toPlace = null
        route = null
        routeError = null
        pickTarget = null
        senderPhoneInput = ""
        receiverPhoneInput = ""
    }
}
