package com.pit.bahromtaxi.ui.intercity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.domain.IntercityStatus
import com.pit.bahromtaxi.domain.IntercityTrip

/** Водитель создаёт межгородние поездки "по сбору мест" и управляет набором пассажиров. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityDriverScreen(viewModel: IntercityDriverViewModel, onBack: () -> Unit) {
    val myTrips by viewModel.myTrips.collectAsState()
    val error by viewModel.lastError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Межгород") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            viewModel.createError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Text("Новая поездка", fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.fromCityInput,
                    onValueChange = { viewModel.fromCityInput = it },
                    label = { Text("Откуда") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.toCityInput,
                    onValueChange = { viewModel.toCityInput = it },
                    label = { Text("Куда") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.totalSeatsInput,
                    onValueChange = { viewModel.totalSeatsInput = it },
                    label = { Text("Мест в машине") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.pricePerSeatInput,
                    onValueChange = { viewModel.pricePerSeatInput = it },
                    label = { Text("Цена за место") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = viewModel.scheduledAtInput,
                onValueChange = { viewModel.scheduledAtInput = it },
                label = { Text("Дата отправления (необязательно, напр. 2026-09-15)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "Оставьте дату пустой, если поездка отправится сразу как наберётся полная машина. " +
                    "Укажите дату для заранее спланированной поездки (например, через 3 дня) — " +
                    "тогда она ждёт этой даты, даже если наберётся раньше.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = { viewModel.createTrip() },
                enabled = !viewModel.creating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.creating) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Создать поездку")
            }

            HorizontalDivider()

            Text("Мои поездки", fontWeight = FontWeight.Bold)
            if (myTrips.isEmpty()) {
                Text("Пока нет созданных поездок.")
            } else {
                myTrips.forEach { trip ->
                    DriverTripCard(
                        trip,
                        onDepart = { viewModel.depart(trip.id) },
                        onComplete = { viewModel.complete(trip.id) },
                        onCancel = { viewModel.cancelTrip(trip.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverTripCard(trip: IntercityTrip, onDepart: () -> Unit, onComplete: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${trip.fromCity} → ${trip.toCity}", fontWeight = FontWeight.Bold)
            Text("${trip.pricePerSeat.toInt()} ₽ за место")
            LinearProgressIndicator(
                progress = { if (trip.totalSeats > 0) trip.bookedSeats.toFloat() / trip.totalSeats else 0f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Забронировано ${trip.bookedSeats} из ${trip.totalSeats} мест")
            trip.scheduledAt?.let { Text("Дата отправления: $it", style = MaterialTheme.typography.bodySmall) }
            Text(statusLabel(trip.status), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            when (trip.status) {
                IntercityStatus.OPEN, IntercityStatus.FULL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDepart, enabled = trip.bookedSeats > 0) { Text("Выехать") }
                    TextButton(onClick = onCancel) { Text("Отменить", color = MaterialTheme.colorScheme.error) }
                }
                IntercityStatus.IN_PROGRESS -> Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("Завершить поездку")
                }
                else -> {}
            }
        }
    }
}

private fun statusLabel(status: IntercityStatus): String = when (status) {
    IntercityStatus.OPEN -> "Набор пассажиров"
    IntercityStatus.FULL -> "Машина набрана"
    IntercityStatus.IN_PROGRESS -> "В пути"
    IntercityStatus.COMPLETED -> "Завершена"
    IntercityStatus.CANCELLED -> "Отменена"
}
