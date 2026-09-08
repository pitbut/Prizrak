package com.pit.bahromtaxi.ui.driver

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.PaymentMethod
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import com.pit.bahromtaxi.domain.label
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(viewModel: DriverViewModel, onBack: () -> Unit) {
    var registered by remember { mutableStateOf(viewModel.isRegistered) }
    val online by viewModel.online.collectAsState()
    val rides by viewModel.rides.collectAsState()
    val commissionOwed by viewModel.commissionOwed.collectAsState()
    val commissionPaid by viewModel.commissionPaid.collectAsState()
    val error by viewModel.lastError.collectAsState()

    LaunchedEffect(registered) {
        if (registered) {
            RideRepository.ensureConnected()
            RideRepository.refreshCommission()
        }
    }

    val myRide = rides.find {
        it.driverId == viewModel.driverId &&
            (it.status == RideStatus.ACCEPTED || it.status == RideStatus.IN_PROGRESS)
    }
    val pending = rides.filter { it.status == RideStatus.SEARCHING }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Водитель") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (!registered) {
                PhoneAuthGate(viewModel = viewModel, onDone = { registered = true })
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (online) "На линии" else "Не на линии", fontWeight = FontWeight.Bold)
                Switch(checked = online, onCheckedChange = { viewModel.setOnline(it) })
            }

            CommissionCard(owed = commissionOwed, paid = commissionPaid, onPay = { viewModel.payCommission() })

            HorizontalDivider()

            when {
                myRide != null -> {
                    Text("Текущая поездка", fontWeight = FontWeight.Bold)
                    DriverRideCard(myRide, viewModel)
                }
                online -> {
                    Text("Свободные заказы", fontWeight = FontWeight.Bold)
                    if (pending.isEmpty()) {
                        Text("Пока нет заказов поблизости.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pending, key = { it.id }) { ride ->
                                PendingRideCard(ride, onAccept = { viewModel.accept(ride.id) })
                            }
                        }
                    }
                }
                else -> {
                    Text("Выйдите на линию, чтобы видеть заказы.")
                }
            }
        }
    }
}

@Composable
private fun PhoneAuthGate(viewModel: DriverViewModel, onDone: () -> Unit) {
    val activity = LocalContext.current as Activity

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        viewModel.authError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        when (viewModel.authStep) {
            DriverAuthStep.PHONE -> {
                if (!viewModel.showEmailForm) {
                    Text("Ваш номер телефона", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.phoneInput,
                        onValueChange = { viewModel.phoneInput = it },
                        label = { Text("+998901234567") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.sendCode(activity) },
                        enabled = !viewModel.authLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (viewModel.authLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Получить код")
                    }
                    TextButton(onClick = { viewModel.showEmailForm = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Войти по email вместо телефона")
                    }
                } else {
                    Text("Вход по email", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.emailInput,
                        onValueChange = { viewModel.emailInput = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.passwordInput,
                        onValueChange = { viewModel.passwordInput = it },
                        label = { Text("Пароль") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.continueWithEmail() },
                        enabled = !viewModel.authLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (viewModel.authLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Продолжить")
                    }
                    if (viewModel.passwordResetSent) {
                        Text(
                            "Письмо со ссылкой для сброса пароля отправлено, если такой аккаунт существует.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        TextButton(onClick = { viewModel.sendPasswordReset() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Забыли пароль?")
                        }
                    }
                    TextButton(onClick = { viewModel.showEmailForm = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Назад к номеру телефона")
                    }
                }
            }
            DriverAuthStep.CODE -> {
                Text("Код из SMS", fontWeight = FontWeight.Bold)
                Text("Отправлен на ${viewModel.phoneInput}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = viewModel.codeInput,
                    onValueChange = { viewModel.codeInput = it },
                    label = { Text("Код") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.confirmCode() },
                    enabled = !viewModel.authLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (viewModel.authLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Подтвердить")
                }
            }
            DriverAuthStep.VERIFY_EMAIL -> {
                Text("Подтвердите email", fontWeight = FontWeight.Bold)
                Text(
                    "Мы отправили письмо на ${viewModel.emailInput}. Перейдите по ссылке в письме, " +
                        "потом нажмите «Я подтвердил».",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { viewModel.checkEmailVerified() },
                    enabled = !viewModel.authLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (viewModel.authLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Я подтвердил")
                }
                TextButton(onClick = { viewModel.resendVerificationEmail() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Отправить письмо ещё раз")
                }
            }
            DriverAuthStep.PROFILE -> {
                Text("Данные водителя", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = viewModel.nameInput,
                    onValueChange = { viewModel.nameInput = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.carMake,
                    onValueChange = { viewModel.carMake = it },
                    label = { Text("Марка и модель авто") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.carColor,
                        onValueChange = { viewModel.carColor = it },
                        label = { Text("Цвет") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.carPlate,
                        onValueChange = { viewModel.carPlate = it },
                        label = { Text("Гос. номер") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = viewModel.clickHandle,
                    onValueChange = { viewModel.clickHandle = it },
                    label = { Text("Click для оплаты (телефон, необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Марка, цвет и номер помогают пассажиру узнать машину. Click — если хотите принимать " +
                        "оплату переводом, а не только наличными или картой на месте.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { viewModel.register(onDone) },
                    enabled = !viewModel.registering,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (viewModel.registering) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Продолжить")
                }
            }
        }
    }
}

@Composable
private fun CommissionCard(owed: Double, paid: Double, onPay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Комиссия агрегатора — 1% с поездки", fontWeight = FontWeight.Bold)
            Text("К оплате: ${fmt2(owed)} ₽")
            Text("Оплачено всего: ${fmt2(paid)} ₽", style = MaterialTheme.typography.bodySmall)
            if (owed > 0.0) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onPay) { Text("Погасить комиссию") }
            }
        }
    }
}

@Composable
private fun PendingRideCard(ride: Ride, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${ride.fromAddress} → ${ride.toAddress}", fontWeight = FontWeight.Bold)
            Text("${fmt1(ride.distanceKm)} км · ${ride.durationMin.toInt()} мин · ${ride.paymentMethod.label}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Заказ: ${ride.price.total.toInt()} ₽")
                    Text(
                        "Вам: ${ride.price.driverPayout.toInt()} ₽ (комиссия ${fmt2(ride.price.commission)} ₽)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(onClick = onAccept) { Text("Принять") }
            }
        }
    }
}

@Composable
private fun DriverRideCard(ride: Ride, viewModel: DriverViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${ride.fromAddress} → ${ride.toAddress}", fontWeight = FontWeight.Bold)
            Text("Заказ: ${ride.price.total.toInt()} ₽ · Вам: ${ride.price.driverPayout.toInt()} ₽ · ${ride.paymentMethod.label}")
            if (ride.paymentMethod == PaymentMethod.CLICK) {
                Text(
                    "Пассажир увидит ваш Click-номер из профиля для перевода.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            when (ride.status) {
                RideStatus.ACCEPTED -> Button(
                    onClick = { viewModel.start(ride.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Начать поездку") }
                RideStatus.IN_PROGRESS -> Button(
                    onClick = { viewModel.complete(ride.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Завершить поездку") }
                else -> {}
            }
        }
    }
}

private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)
