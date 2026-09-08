package com.pit.bahromtaxi.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(viewModel: ClientViewModel, onBack: () -> Unit) {
    val rides by viewModel.rides.collectAsState()
    val activeRide = rides.find { it.id == viewModel.activeRideId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пассажир") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (activeRide == null || activeRide.status == RideStatus.CANCELLED) {
                OrderForm(viewModel)
            } else {
                ActiveRideCard(activeRide, onNewOrder = { viewModel.resetOrder() })
            }
        }
    }
}

@Composable
private fun OrderForm(viewModel: ClientViewModel) {
    val price = viewModel.estimate
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = viewModel.fromAddress,
            onValueChange = { viewModel.fromAddress = it },
            label = { Text("Откуда") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = viewModel.toAddress,
            onValueChange = { viewModel.toAddress = it },
            label = { Text("Куда") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Расстояние: ${fmt1(viewModel.distanceKm)} км")
        Slider(
            value = viewModel.distanceKm.toFloat(),
            onValueChange = { viewModel.setDistance(it.toDouble()) },
            valueRange = 1f..40f
        )
        Text(
            "В песочнице расстояние задаётся вручную — в реальном приложении его посчитает " +
                "картографический сервис по маршруту и пробкам.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        PriceBreakdownView(price)

        Button(
            onClick = { viewModel.order() },
            enabled = viewModel.fromAddress.isNotBlank() && viewModel.toAddress.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Заказать за ${price.total.toInt()} ₽")
        }
    }
}

@Composable
private fun PriceBreakdownView(price: PriceBreakdown) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Расчёт цены (считает алгоритм)", fontWeight = FontWeight.Bold)
        PriceRow("Посадка", price.baseFare)
        PriceRow("За расстояние", price.distanceCost)
        PriceRow("За время в пути", price.timeCost)
        if (price.demandFactor > 1.0) {
            Text(
                "Коэффициент спроса: ×${fmt1(price.demandFactor)} (заказов больше, чем свободных машин)",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider()
        PriceRow("Итого", price.total, bold = true)
    }
}

@Composable
private fun PriceRow(label: String, value: Double, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text("${value.toInt()} ₽", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ActiveRideCard(ride: Ride, onNewOrder: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${ride.fromAddress} → ${ride.toAddress}", fontWeight = FontWeight.Bold)
                Text(statusText(ride.status, ride.driverName))
                Text("Стоимость: ${ride.price.total.toInt()} ₽")
            }
        }
        when (ride.status) {
            RideStatus.COMPLETED -> {
                Text("Поездка завершена. Спасибо!")
                Button(onClick = onNewOrder, modifier = Modifier.fillMaxWidth()) {
                    Text("Заказать снова")
                }
            }
            RideStatus.CANCELLED -> {
                Button(onClick = onNewOrder, modifier = Modifier.fillMaxWidth()) {
                    Text("Заказать снова")
                }
            }
            else -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun statusText(status: RideStatus, driverName: String?): String = when (status) {
    RideStatus.SEARCHING -> "Ищем свободного водителя…"
    RideStatus.ACCEPTED -> "Водитель $driverName принял заказ и едет к вам"
    RideStatus.IN_PROGRESS -> "Поездка началась"
    RideStatus.COMPLETED -> "Завершена"
    RideStatus.CANCELLED -> "Отменена"
}

private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
