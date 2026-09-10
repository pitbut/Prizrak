package com.pit.bahromtaxi.ui.grouptrip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pit.bahromtaxi.domain.GroupTrip
import com.pit.bahromtaxi.domain.GroupTripOfferStatus
import com.pit.bahromtaxi.domain.GroupTripStatus
import com.pit.bahromtaxi.network.AuthStore

/** Водитель видит групповые заявки, которым не хватает мест, и предлагает свои. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTripDriverScreen(viewModel: GroupTripDriverViewModel, onBack: () -> Unit) {
    val openTrips by viewModel.openTrips.collectAsState()
    val myTrips by viewModel.myTrips.collectAsState()
    val error by viewModel.lastError.collectAsState()
    val myOfferedTripIds = myTrips.map { it.id }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Групповые заявки") },
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

            OutlinedTextField(
                value = viewModel.seatsInput,
                onValueChange = { viewModel.seatsInput = it },
                label = { Text("Сколько мест вы можете предложить") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Нужны водители", fontWeight = FontWeight.Bold)
            val stillOpen = openTrips.filter { it.id !in myOfferedTripIds }
            if (stillOpen.isEmpty()) {
                Text("Пока нет открытых групповых заявок.")
            } else {
                stillOpen.forEach { trip ->
                    OpenTripCard(trip, onOffer = { viewModel.offerSeats(trip.id) })
                }
            }

            HorizontalDivider()

            Text("Мои отклики", fontWeight = FontWeight.Bold)
            if (myTrips.isEmpty()) {
                Text("Вы ещё не откликались на заявки.")
            } else {
                myTrips.forEach { trip -> MyOfferCard(trip, onCancel = { offerId -> viewModel.cancelOffer(trip.id, offerId) }) }
            }
        }
    }
}

@Composable
private fun OpenTripCard(trip: GroupTrip, onOffer: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${trip.fromCity} → ${trip.toCity}", fontWeight = FontWeight.Bold)
            Text("Нужно мест: ${trip.seatsStillNeeded} из ${trip.peopleCount}")
            trip.desiredAt?.let { Text("Желаемое время: $it", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onOffer, modifier = Modifier.fillMaxWidth()) { Text("Предложить места") }
        }
    }
}

@Composable
private fun MyOfferCard(trip: GroupTrip, onCancel: (String) -> Unit) {
    val myId = AuthStore.userId
    val myOffer = trip.offers.find { it.driverId == myId }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${trip.fromCity} → ${trip.toCity}", fontWeight = FontWeight.Bold)
            myOffer?.let { Text("Ваше предложение: ${it.seatsOffered} мест — ${offerStatusLabel(it.status)}") }
            trip.desiredAt?.let { Text("Желаемое время: $it", style = MaterialTheme.typography.bodySmall) }
            Text(statusLabel(trip.status), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (myOffer != null && myOffer.status == GroupTripOfferStatus.PENDING) {
                TextButton(onClick = { onCancel(myOffer.id) }) { Text("Отозвать предложение", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun offerStatusLabel(status: GroupTripOfferStatus): String = when (status) {
    GroupTripOfferStatus.PENDING -> "ждём решения пассажира"
    GroupTripOfferStatus.ACCEPTED -> "принято"
    GroupTripOfferStatus.REJECTED -> "отклонено"
    GroupTripOfferStatus.WITHDRAWN -> "отозвано"
}

private fun statusLabel(status: GroupTripStatus): String = when (status) {
    GroupTripStatus.OPEN -> "Собираем водителей"
    GroupTripStatus.CONFIRMED -> "Подтверждено — ждите напоминание перед отправлением"
    GroupTripStatus.CANCELLED -> "Отменена"
    GroupTripStatus.COMPLETED -> "Завершена"
}
