package com.pit.bahromtaxi.ui.intercity

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.domain.IntercityStatus
import com.pit.bahromtaxi.domain.IntercityTrip
import com.pit.bahromtaxi.domain.TripPointMode
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.TASHKENT
import com.pit.bahromtaxi.ui.common.AddressSearchDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityPassengerScreen(viewModel: IntercityPassengerViewModel, onBack: () -> Unit) {
    val trips by viewModel.browseTrips.collectAsState()
    val myTrips by viewModel.myTrips.collectAsState()
    val error by viewModel.lastError.collectAsState()
    val bookingTrip = viewModel.bookingTrip
    var addressTarget by remember { mutableStateOf<PickTarget?>(null) }

    val sortedTrips = remember(trips, viewModel.sortByTimeSoonest) {
        if (viewModel.sortByTimeSoonest) trips.sortedBy { it.scheduledAt ?: "" } else trips
    }

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
            Text(
                "Поездка не трогается, пока не наберётся нужное число пассажиров, либо пока водитель " +
                    "не решит выехать раньше с неполной машиной.",
                style = MaterialTheme.typography.bodySmall
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            if (myTrips.isNotEmpty()) {
                Text("Мои бронирования", fontWeight = FontWeight.Bold)
                myTrips.forEach { trip -> MyBookingCard(trip, onCancel = { viewModel.cancelBooking(trip.id) }) }
                HorizontalDivider()
            }

            Text("Найти попутку", fontWeight = FontWeight.Bold)
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
            Button(onClick = { viewModel.search() }, modifier = Modifier.fillMaxWidth()) {
                Text("Искать")
            }
            FilterChip(
                selected = viewModel.sortByTimeSoonest,
                onClick = { viewModel.sortByTimeSoonest = !viewModel.sortByTimeSoonest },
                label = { Text("Сортировка: сначала ближайшие по времени") }
            )

            if (sortedTrips.isEmpty()) {
                Text("Пока нет открытых поездок по этому направлению.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedTrips, key = { it.id }) { trip ->
                        TripCard(trip, onBook = { viewModel.openBooking(trip) })
                    }
                }
            }
        }
    }

    if (bookingTrip != null) {
        val needsPickupAddress = bookingTrip.pickupMode == TripPointMode.COLLECT
        val needsDropoffAddress = bookingTrip.dropoffMode == TripPointMode.COLLECT
        AlertDialog(
            onDismissRequest = { viewModel.closeBooking() },
            title = { Text("${bookingTrip.fromCity} → ${bookingTrip.toCity}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = viewModel.seatsToBookInput,
                        onValueChange = { viewModel.seatsToBookInput = it },
                        label = { Text("Сколько мест") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (needsPickupAddress) {
                        AddressPickRow(
                            label = "Откуда забрать",
                            value = viewModel.pickupAddressInput,
                            onClick = { addressTarget = PickTarget.FROM }
                        )
                    } else {
                        Text("Место посадки: ${bookingTrip.pickupPoint ?: bookingTrip.fromCity}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (needsDropoffAddress) {
                        AddressPickRow(
                            label = "Куда довезти",
                            value = viewModel.dropoffAddressInput,
                            onClick = { addressTarget = PickTarget.TO }
                        )
                    } else {
                        Text("Место высадки: ${bookingTrip.dropoffPoint ?: bookingTrip.toCity}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        bookingTrip.scheduledAt?.let { "Отправление: $it" } ?: "Отправление — как только наберётся машина",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBooking {} },
                    enabled = !viewModel.booking &&
                        (!needsPickupAddress || viewModel.pickupAddressInput.isNotBlank()) &&
                        (!needsDropoffAddress || viewModel.dropoffAddressInput.isNotBlank())
                ) {
                    if (viewModel.booking) CircularProgressIndicator(modifier = Modifier.height(18.dp)) else Text("Забронировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeBooking() }) { Text("Отмена") }
            }
        )
    }

    addressTarget?.let { target ->
        AddressSearchDialog(
            title = if (target == PickTarget.FROM) "Откуда забрать" else "Куда довезти",
            near = TASHKENT,
            onDismiss = { addressTarget = null },
            onSelect = { place ->
                if (target == PickTarget.FROM) viewModel.pickupAddressInput = place.address
                else viewModel.dropoffAddressInput = place.address
                addressTarget = null
            }
        )
    }
}

@Composable
private fun AddressPickRow(label: String, value: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value.ifBlank { "Выбрать адрес" }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MyBookingCard(trip: IntercityTrip, onCancel: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${trip.fromCity} → ${trip.toCity}", fontWeight = FontWeight.Bold)
            Text("Забронировано мест: ${trip.myBookedSeats} · ${trip.bookedSeats}/${trip.totalSeats} в машине")
            Text(statusLabel(trip.status), style = MaterialTheme.typography.bodySmall)
            trip.scheduledAt?.let { Text("Дата: $it", style = MaterialTheme.typography.bodySmall) }
            (trip.driverCarMake ?: trip.driverCarPlate)?.let {
                Text(
                    "Машина: ${trip.driverCarMake ?: ""}${if (trip.driverCarPlate != null) " · ${trip.driverCarPlate}" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            trip.driverPhone?.let { phone ->
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                    Text("Позвонить водителю${trip.driverName?.let { " ($it)" } ?: ""}: $phone")
                }
            }
            if (trip.status == IntercityStatus.OPEN || trip.status == IntercityStatus.FULL) {
                TextButton(onClick = onCancel) { Text("Отменить бронь", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TripCard(trip: IntercityTrip, onBook: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${trip.fromCity} → ${trip.toCity}", fontWeight = FontWeight.Bold)
            (trip.driverCarMake ?: trip.driverCarPlate)?.let {
                Text(
                    "${trip.driverCarMake ?: "Машина"}${if (trip.driverCarPlate != null) " · ${trip.driverCarPlate}" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("${trip.pricePerSeat.toInt()} ₽ за место")
            LinearProgressIndicator(
                progress = { if (trip.totalSeats > 0) trip.bookedSeats.toFloat() / trip.totalSeats else 0f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Занято ${trip.bookedSeats} из ${trip.totalSeats} мест · свободно ${trip.seatsLeft}")
            Text(
                trip.scheduledAt?.let { "Отправление: $it" } ?: "Отправление — как только наберётся машина",
                style = MaterialTheme.typography.bodySmall
            )
            if (trip.pickupMode == TripPointMode.COLLECT || trip.dropoffMode == TripPointMode.COLLECT) {
                Text("Водитель заберёт/довезёт по вашему адресу", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onBook, enabled = trip.seatsLeft > 0, modifier = Modifier.fillMaxWidth()) {
                Text(if (trip.seatsLeft > 0) "Забронировать" else "Мест нет")
            }
        }
    }
}

private fun statusLabel(status: IntercityStatus): String = when (status) {
    IntercityStatus.OPEN -> "Набор пассажиров"
    IntercityStatus.FULL -> "Машина набрана, скоро отправление"
    IntercityStatus.IN_PROGRESS -> "В пути"
    IntercityStatus.COMPLETED -> "Завершена"
    IntercityStatus.CANCELLED -> "Отменена"
}
