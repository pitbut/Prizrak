package com.pit.bahromtaxi.ui.driver

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(viewModel: DriverViewModel, onBack: () -> Unit) {
    val online by viewModel.online.collectAsState()
    val rides by viewModel.rides.collectAsState()
    val commissionOwed by viewModel.commissionOwed.collectAsState()
    val commissionPaid by viewModel.commissionPaid.collectAsState()

    val myRide = rides.find {
        it.driverName == viewModel.driverName &&
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
            Text("${fmt1(ride.distanceKm)} км · ${ride.durationMin.toInt()} мин")
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
            Text("Заказ: ${ride.price.total.toInt()} ₽ · Вам: ${ride.price.driverPayout.toInt()} ₽")
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
