package com.pit.bahromtaxi.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pit.bahromtaxi.maps.Coordinate
import com.pit.bahromtaxi.maps.NominatimClient
import com.pit.bahromtaxi.maps.PlacePoint
import kotlinx.coroutines.delay

/** Полноэкранный поиск адреса с приоритетом на местоположение рядом (см. NominatimClient.search). */
@Composable
fun AddressSearchDialog(title: String, near: Coordinate, onDismiss: () -> Unit, onSelect: (PlacePoint) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlacePoint>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 3) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(600) // не долбить Nominatim на каждое нажатие клавиши — лимит ~1 запрос/сек
        loading = true
        NominatimClient.search(trimmed, near = near)
            .onSuccess { results = it }
            .onFailure { results = emptyList() }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Закрыть")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Введите адрес") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results) { place ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onSelect(place) }
                                .padding(vertical = 14.dp)
                        ) {
                            Text(place.address, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
