package com.pit.bahromtaxi.ui.grouptrip

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.domain.GroupTrip
import com.pit.bahromtaxi.domain.GroupTripOffer
import com.pit.bahromtaxi.domain.GroupTripOfferStatus
import com.pit.bahromtaxi.domain.GroupTripStatus
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.TASHKENT
import com.pit.bahromtaxi.ui.common.AddressSearchDialog

/**
 * Групповая поездка — пассажир публикует заявку (направление, число людей, желаемое время),
 * водители предлагают места, пассажир принимает нужные предложения, пока не наберётся вся
 * группа. Подходит, когда одной машины не хватает: несколько водителей закрывают заявку вместе.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTripPassengerScreen(viewModel: GroupTripPassengerViewModel, onBack: () -> Unit) {
    val myTrips by viewModel.myTrips.collectAsState()
    val error by viewModel.lastError.collectAsState()
    var addressTarget by remember { mutableStateOf<PickTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Групповая поездка") },
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
                "Для группы, которая едет вместе (например, на отдых) — если одной машины мало, " +
                    "заявку могут закрыть несколько водителей сразу.",
                style = MaterialTheme.typography.bodySmall
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            viewModel.createError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Text("Новая заявка", fontWeight = FontWeight.Bold)
            AddressPickRow(label = "Откуда", value = viewModel.fromCityInput, onClick = { addressTarget = PickTarget.FROM })
            AddressPickRow(label = "Куда", value = viewModel.toCityInput, onClick = { addressTarget = PickTarget.TO })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.peopleCountInput,
                    onValueChange = { viewModel.peopleCountInput = it },
                    label = { Text("Сколько человек") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.desiredAtInput,
                    onValueChange = { viewModel.desiredAtInput = it },
                    label = { Text("Дата/время (напр. 2026-09-20 09:00)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Button(
                onClick = { viewModel.createTrip() },
                enabled = !viewModel.creating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.creating) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Опубликовать заявку")
            }

            HorizontalDivider()

            Text("Мои заявки", fontWeight = FontWeight.Bold)
            if (myTrips.isEmpty()) {
                Text("Пока нет заявок.")
            } else {
                myTrips.forEach { trip ->
                    PassengerTripCard(
                        trip,
                        onAccept = { offerId -> viewModel.acceptOffer(trip.id, offerId) },
                        onReject = { offerId -> viewModel.rejectOffer(trip.id, offerId) },
                        onCancel = { viewModel.cancelTrip(trip.id) }
                    )
                }
            }
        }
    }

    addressTarget?.let { target ->
        AddressSearchDialog(
            title = if (target == PickTarget.FROM) "Откуда" else "Куда",
            near = TASHKENT,
            onDismiss = { addressTarget = null },
            onSelect = { place ->
                if (target == PickTarget.FROM) viewModel.fromCityInput = place.address else viewModel.toCityInput = place.address
                addressTarget = null
            }
        )
    }
}

@Composable
private fun AddressPickRow(label: String, value: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value.ifBlank { "Найти адрес" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PassengerTripCard(
    trip: GroupTrip,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${trip.fromCity} → ${trip.toCity}", fontWeight = FontWeight.Bold)
            Text("Нужно мест: ${trip.peopleCount} · подтверждено: ${trip.seatsConfirmed}")
            trip.desiredAt?.let { Text("Желаемое время: $it", style = MaterialTheme.typography.bodySmall) }
            Text(statusLabel(trip.status), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

            val pending = trip.offers.filter { it.status == GroupTripOfferStatus.PENDING }
            val accepted = trip.offers.filter { it.status == GroupTripOfferStatus.ACCEPTED }

            if (accepted.isNotEmpty()) {
                Text("Приняты", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                accepted.forEach { offer -> OfferRow(offer, context) }
            }
            if (pending.isNotEmpty()) {
                Text("Предложения водителей", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                pending.forEach { offer ->
                    Column {
                        OfferRow(offer, context)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAccept(offer.id) }) { Text("Принять") }
                            TextButton(onClick = { onReject(offer.id) }) { Text("Отклонить") }
                        }
                    }
                }
            }
            if (trip.status == GroupTripStatus.OPEN || trip.status == GroupTripStatus.CONFIRMED) {
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Отменить заявку", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun OfferRow(offer: GroupTripOffer, context: android.content.Context) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("${offer.driverName ?: "Водитель"} — ${offer.seatsOffered} мест${carSuffix(offer.carMake, offer.carPlate)}")
        offer.driverPhone?.let { phone ->
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                Text("Позвонить: $phone")
            }
        }
    }
}

private fun carSuffix(carMake: String?, carPlate: String?): String {
    if (carMake == null && carPlate == null) return ""
    return " (${listOfNotNull(carMake, carPlate).joinToString(" · ")})"
}

private fun statusLabel(status: GroupTripStatus): String = when (status) {
    GroupTripStatus.OPEN -> "Собираем водителей"
    GroupTripStatus.CONFIRMED -> "Подтверждено — ждите напоминание перед отправлением"
    GroupTripStatus.CANCELLED -> "Отменена"
    GroupTripStatus.COMPLETED -> "Завершена"
}
