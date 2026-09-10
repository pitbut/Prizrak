package com.pit.bahromtaxi.data

import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.OrderType
import com.pit.bahromtaxi.domain.PaymentMethod
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import com.pit.bahromtaxi.network.ApiClient
import com.pit.bahromtaxi.network.AuthStore
import com.pit.bahromtaxi.network.ChatMessageDto
import com.pit.bahromtaxi.network.CreateRideRequest
import com.pit.bahromtaxi.network.OnlineRequest
import com.pit.bahromtaxi.network.ProfileDto
import com.pit.bahromtaxi.network.ProfileUpdateRequest
import com.pit.bahromtaxi.network.RegisterRequest
import com.pit.bahromtaxi.network.RideDto
import com.pit.bahromtaxi.network.RideSocket
import com.pit.bahromtaxi.network.WsEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Клиент к backend на VPS (см. taxiapp/README.md, раздел «Backend»). Матчинг заказов,
 * расчёт цены и коэффициента спроса — на сервере; здесь только запросы к его API и
 * WebSocket для live-обновлений, локальной бизнес-логики цены больше нет.
 */
object RideRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = ApiClient.service

    private val _rides = MutableStateFlow<List<Ride>>(emptyList())
    val rides: StateFlow<List<Ride>> = _rides

    private val _driverOnline = MutableStateFlow(false)
    val driverOnline: StateFlow<Boolean> = _driverOnline

    private val _commissionOwed = MutableStateFlow(0.0)
    val commissionOwed: StateFlow<Double> = _commissionOwed

    private val _commissionPaid = MutableStateFlow(0.0)
    val commissionPaid: StateFlow<Double> = _commissionPaid

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _chatMessages = MutableSharedFlow<ChatMessageDto>(extraBufferCapacity = 16)
    val chatMessages: SharedFlow<ChatMessageDto> = _chatMessages

    private var socket: RideSocket? = null
    private var connectedRole: String? = null

    /** Переподключается, если активная роль сменилась (пассажир ⇄ водитель), иначе не трогает сокет. */
    fun ensureConnected() {
        val activeRole = AuthStore.role
        if (socket != null && connectedRole == activeRole) return
        socket?.close()
        _rides.value = emptyList()
        _driverOnline.value = false
        _commissionOwed.value = 0.0
        _commissionPaid.value = 0.0
        connectedRole = activeRole
        socket = RideSocket { event -> handleEvent(event) }.also { it.connect() }
    }

    suspend fun register(
        firebaseIdToken: String,
        role: String,
        name: String,
        carMake: String? = null,
        carColor: String? = null,
        carPlate: String? = null,
        clickHandle: String? = null,
        vehicleType: String? = null,
        capacityKg: Double? = null
    ): Boolean = runCatching {
        val response = api.register(
            RegisterRequest(firebaseIdToken, role, name, carMake, carColor, carPlate, clickHandle, vehicleType, capacityKg)
        )
        AuthStore.saveSession(response.role, response.token, response.userId, response.name)
        ensureConnected()
        true
    }.getOrElse {
        _lastError.value = "Не удалось подключиться к серверу: ${it.message}"
        false
    }

    fun refreshPending() {
        scope.launch {
            runCatching { api.pendingRides() }
                .onSuccess { list -> mergeRides(list.map { it.toDomain() }) }
                .onFailure { _lastError.value = "Не удалось получить заказы: ${it.message}" }
        }
    }

    fun createOrder(
        fromAddress: String,
        toAddress: String,
        distanceKm: Double,
        durationMin: Double,
        paymentMethod: PaymentMethod,
        orderType: OrderType = OrderType.RIDE,
        senderPhone: String? = null,
        receiverPhone: String? = null,
        cargoDescription: String? = null,
        cargoWeightKg: Double? = null,
        scheduledAt: String? = null,
        onResult: (Ride?) -> Unit
    ) {
        scope.launch {
            val ride = runCatching {
                api.createRide(
                    CreateRideRequest(
                        fromAddress, toAddress, distanceKm, durationMin, paymentMethod.name,
                        type = orderType.name.lowercase(),
                        senderPhone = senderPhone,
                        receiverPhone = receiverPhone,
                        cargoDescription = cargoDescription,
                        cargoWeightKg = cargoWeightKg,
                        scheduledAt = scheduledAt
                    )
                )
            }
                .onFailure { _lastError.value = "Не удалось создать заказ: ${it.message}" }
                .getOrNull()
                ?.toDomain()
            ride?.let { mergeRides(listOf(it)) }
            onResult(ride)
        }
    }

    suspend fun getMessages(rideId: String): List<ChatMessageDto> = runCatching { api.getMessages(rideId) }
        .onFailure { _lastError.value = "Не удалось загрузить чат: ${it.message}" }
        .getOrElse { emptyList() }

    suspend fun sendChatText(rideId: String, text: String): ChatMessageDto? = runCatching {
        api.sendMessage(rideId, text.toRequestBody("text/plain".toMediaType()), null)
    }
        .onFailure { _lastError.value = "Не удалось отправить сообщение: ${it.message}" }
        .getOrNull()

    suspend fun sendChatImage(rideId: String, bytes: ByteArray): ChatMessageDto? = runCatching {
        val part = MultipartBody.Part.createFormData(
            "image", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
        )
        api.sendMessage(rideId, null, part)
    }
        .onFailure { _lastError.value = "Не удалось отправить фото: ${it.message}" }
        .getOrNull()

    suspend fun getProfile(): ProfileDto? = runCatching { api.getProfile() }
        .onFailure { _lastError.value = "Не удалось загрузить профиль: ${it.message}" }
        .getOrNull()

    suspend fun updateProfile(name: String?, phone: String?): ProfileDto? = runCatching {
        api.updateProfile(ProfileUpdateRequest(name, phone))
    }
        .onFailure { _lastError.value = "Не удалось сохранить профиль: ${it.message}" }
        .getOrNull()

    suspend fun getRideHistory(before: String? = null): List<Ride> = runCatching {
        api.rideHistory(before = before).map { it.toDomain() }
    }
        .onFailure { _lastError.value = "Не удалось загрузить историю: ${it.message}" }
        .getOrElse { emptyList() }

    /**
     * Удаляет аккаунт (soft delete на сервере — история поездок остаётся видна обеим сторонам).
     * После успеха стирает локальную сессию активной роли и выходит из Firebase, чтобы то же
     * имя/телефон/почта можно было сразу использовать для новой регистрации.
     */
    suspend fun deleteAccount(): Boolean = runCatching { api.deleteAccount() }
        .onSuccess {
            socket?.close()
            socket = null
            connectedRole = null
            _rides.value = emptyList()
            _driverOnline.value = false
            _commissionOwed.value = 0.0
            _commissionPaid.value = 0.0
            AuthStore.clearActiveRole()
            runCatching { FirebaseAuth.getInstance().signOut() }
        }
        .onFailure { _lastError.value = "Не удалось удалить аккаунт: ${it.message}" }
        .isSuccess

    fun setDriverOnline(online: Boolean) {
        val driverId = AuthStore.userId ?: return
        _driverOnline.value = online
        scope.launch {
            runCatching { api.setOnline(driverId, OnlineRequest(online)) }
                .onFailure { _lastError.value = "Не удалось обновить статус: ${it.message}" }
            if (online) refreshPending()
        }
    }

    fun acceptRide(rideId: String) = act { api.acceptRide(rideId) }
    fun startRide(rideId: String) = act { api.startRide(rideId) }

    fun completeRide(rideId: String) {
        act { api.completeRide(rideId) }
        refreshCommission()
    }

    fun refreshCommission() {
        val driverId = AuthStore.userId ?: return
        scope.launch {
            runCatching { api.commission(driverId) }
                .onSuccess {
                    _commissionOwed.value = it.owed
                    _commissionPaid.value = it.paid
                }
                .onFailure { _lastError.value = "Не удалось получить баланс комиссии: ${it.message}" }
        }
    }

    fun payCommission() {
        val driverId = AuthStore.userId ?: return
        scope.launch {
            runCatching { api.payCommission(driverId) }
                .onSuccess {
                    _commissionOwed.value = it.owed
                    _commissionPaid.value = it.paid
                }
                .onFailure { _lastError.value = "Не удалось погасить комиссию: ${it.message}" }
        }
    }

    private fun act(call: suspend () -> RideDto) {
        scope.launch {
            runCatching { call() }
                .onSuccess { mergeRides(listOf(it.toDomain())) }
                .onFailure { _lastError.value = "Не удалось обновить заказ: ${it.message}" }
        }
    }

    private fun handleEvent(event: WsEvent) {
        if (event.type == "chat_message") {
            event.message?.let { _chatMessages.tryEmit(it) }
            return
        }
        val ride = event.ride?.toDomain() ?: return
        mergeRides(listOf(ride))
        if (ride.status == RideStatus.COMPLETED && ride.driverId == AuthStore.userId) {
            refreshCommission()
        }
    }

    private fun mergeRides(updated: List<Ride>) {
        _rides.update { current ->
            val order = current.map { it.id }.toMutableList()
            val byId = current.associateBy { it.id }.toMutableMap()
            updated.forEach { ride ->
                byId[ride.id] = ride
                if (ride.id !in order) order.add(ride.id)
            }
            order.mapNotNull { byId[it] }
        }
    }

    private fun RideDto.toDomain() = Ride(
        id = id,
        fromAddress = fromAddress,
        toAddress = toAddress,
        distanceKm = distanceKm,
        durationMin = durationMin,
        price = PriceBreakdown(
            baseFare = price.baseFare,
            distanceCost = price.distanceCost,
            timeCost = price.timeCost,
            demandFactor = price.demandFactor,
            total = price.total,
            commission = price.commission,
            driverPayout = price.driverPayout
        ),
        status = runCatching { RideStatus.valueOf(status) }.getOrDefault(RideStatus.SEARCHING),
        driverId = driverId,
        driverName = driverName,
        paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod ?: "CASH") }.getOrDefault(PaymentMethod.CASH),
        driverClickHandle = driverClickHandle,
        createdAt = createdAt,
        driverPhone = driverPhone,
        passengerName = passengerName,
        passengerPhone = passengerPhone,
        orderType = runCatching { OrderType.valueOf((type ?: "ride").uppercase()) }.getOrDefault(OrderType.RIDE),
        senderPhone = senderPhone,
        receiverPhone = receiverPhone,
        cargoDescription = cargoDescription,
        cargoWeightKg = cargoWeightKg,
        scheduledAt = scheduledAt
    )
}
