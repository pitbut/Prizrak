package com.pit.bahromtaxi.ui.intercity

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pit.bahromtaxi.domain.IntercityStatus
import com.pit.bahromtaxi.domain.IntercityTrip
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.TASHKENT
import com.pit.bahromtaxi.ui.common.AddressSearchDialog
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Водитель создаёт межгородние поездки "по сбору мест" и управляет набором пассажиров. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityDriverScreen(viewModel: IntercityDriverViewModel, onBack: () -> Unit) {
    val myTrips by viewModel.myTrips.collectAsState()
    val error by viewModel.lastError.collectAsState()

    var searchTarget by remember { mutableStateOf<PickTarget?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

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

            CityRow(label = "Откуда", value = viewModel.fromCityInput, onClick = { searchTarget = PickTarget.FROM })
            CityRow(label = "Куда", value = viewModel.toCityInput, onClick = { searchTarget = PickTarget.TO })

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

            Card(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Дата и время отправления", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(viewModel.scheduledAtInput.ifBlank { "Как наберётся машина" }, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (viewModel.scheduledAtInput.isNotBlank()) {
                        TextButton(onClick = { viewModel.scheduledAtInput = "" }) { Text("Очистить") }
                    }
                }
            }
            Text(
                "Оставьте пустым, если поездка отправится сразу как наберётся полная машина. " +
                    "Укажите дату и время для заранее спланированной поездки (например, через 3 дня) — " +
                    "тогда она ждёт этого момента, даже если наберётся раньше.",
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

    searchTarget?.let { target ->
        AddressSearchDialog(
            title = if (target == PickTarget.FROM) "Откуда" else "Куда",
            near = TASHKENT,
            onDismiss = { searchTarget = null },
            onSelect = { place ->
                if (target == PickTarget.FROM) viewModel.fromCityInput = place.address else viewModel.toCityInput = place.address
                searchTarget = null
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = pickedDateMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Далее") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = true)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Время отправления", fontWeight = FontWeight.Bold)
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                        TextButton(onClick = {
                            val millis = pickedDateMillis
                            if (millis != null) {
                                viewModel.scheduledAtInput = formatScheduledAt(millis, timePickerState.hour, timePickerState.minute)
                            }
                            showTimePicker = false
                        }) { Text("Готово") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(label: String, value: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value.ifBlank { "Найти на карте" }, style = MaterialTheme.typography.bodyLarge)
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

private fun formatScheduledAt(dateMillisUtc: Long, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = dateMillisUtc
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute)
}
