package com.pit.bahromtaxi.ui.client

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.PaymentMethod
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import com.pit.bahromtaxi.domain.label
import com.pit.bahromtaxi.maps.DirectionsClient
import com.pit.bahromtaxi.maps.MapsConfig
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.PlacePoint
import com.pit.bahromtaxi.maps.ReverseGeocoder
import kotlinx.coroutines.launch
import java.util.Locale

private val TASHKENT = LatLng(41.2995, 69.2401)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(viewModel: ClientViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val apiKey = remember { MapsConfig.apiKey(context) }
    val coroutineScope = rememberCoroutineScope()

    var registered by remember { mutableStateOf(viewModel.isRegistered) }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val rides by viewModel.rides.collectAsState()
    val error by viewModel.lastError.collectAsState()
    val activeRide = rides.find { it.id == viewModel.activeRideId }

    LaunchedEffect(registered) {
        if (registered) RideRepository.ensureConnected()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> locationGranted = granted }

    val fromLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
            val place = Autocomplete.getPlaceFromIntent(data)
            place.latLng?.let { latLng ->
                viewModel.setFromPlace(PlacePoint(place.address ?: place.name.orEmpty(), latLng), apiKey)
            }
        }
    }
    val toLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
            val place = Autocomplete.getPlaceFromIntent(data)
            place.latLng?.let { latLng ->
                viewModel.setToPlace(PlacePoint(place.address ?: place.name.orEmpty(), latLng), apiKey)
            }
        }
    }

    fun launchPicker(launcher: ActivityResultLauncher<Intent>) {
        if (!Places.isInitialized()) {
            Toast.makeText(context, "Карта недоступна: не настроен ключ Google Maps API", Toast.LENGTH_LONG).show()
            return
        }
        val fields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(context as Activity)
        launcher.launch(intent)
    }

    fun onMapTap(latLng: LatLng) {
        if (viewModel.pickTarget == null) return
        coroutineScope.launch {
            val address = ReverseGeocoder.addressFor(context, latLng)
            viewModel.setPointFromMap(PlacePoint(address, latLng), apiKey)
        }
    }

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
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            when {
                !registered -> NameGate(
                    name = viewModel.nameInput,
                    onNameChange = { viewModel.nameInput = it },
                    loading = viewModel.registering,
                    onSubmit = { viewModel.register { registered = true } }
                )
                activeRide == null || activeRide.status == RideStatus.CANCELLED -> OrderForm(
                    viewModel = viewModel,
                    locationGranted = locationGranted,
                    onPickFrom = { launchPicker(fromLauncher) },
                    onPickTo = { launchPicker(toLauncher) },
                    onRequestLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    onMapTap = ::onMapTap
                )
                else -> ActiveRideCard(activeRide, onNewOrder = { viewModel.resetOrder() })
            }
        }
    }
}

@Composable
private fun NameGate(name: String, onNameChange: (String) -> Unit, loading: Boolean, onSubmit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Как к вам обращаться?", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Продолжить")
        }
    }
}

@Composable
private fun OrderForm(
    viewModel: ClientViewModel,
    locationGranted: Boolean,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onRequestLocation: () -> Unit,
    onMapTap: (LatLng) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddressRow(
            label = "Откуда",
            value = viewModel.fromPlace?.address,
            picking = viewModel.pickTarget == PickTarget.FROM,
            onClick = onPickFrom,
            onPickOnMap = { viewModel.startPicking(PickTarget.FROM) }
        )
        AddressRow(
            label = "Куда",
            value = viewModel.toPlace?.address,
            picking = viewModel.pickTarget == PickTarget.TO,
            onClick = onPickTo,
            onPickOnMap = { viewModel.startPicking(PickTarget.TO) }
        )

        RouteMap(
            from = viewModel.fromPlace,
            to = viewModel.toPlace,
            route = viewModel.route,
            pickTarget = viewModel.pickTarget,
            locationGranted = locationGranted,
            onRequestLocation = onRequestLocation,
            onMapTap = onMapTap
        )

        when {
            viewModel.routeLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp))
                Text("Строим маршрут…", style = MaterialTheme.typography.bodySmall)
            }
            viewModel.routeError != null -> Text(
                viewModel.routeError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        val route = viewModel.route
        val price = viewModel.estimate
        if (route != null && price != null) {
            Text("${fmt1(route.distanceKm)} км · ${route.durationMin.toInt()} мин", style = MaterialTheme.typography.bodySmall)
        }

        Text("Оплата", fontWeight = FontWeight.Bold)
        PaymentMethodRow(selected = viewModel.paymentMethod, onSelect = { viewModel.paymentMethod = it })
        if (viewModel.paymentMethod == PaymentMethod.CARD) {
            Text(
                "Оплата картой — прямо водителю при встрече (терминалом/переводом). Номер карты в приложении " +
                    "не запрашивается и не хранится.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (route != null && price != null) {
            HorizontalDivider()
            PriceBreakdownView(price)
        }

        Button(
            onClick = { viewModel.order() },
            enabled = route != null && !viewModel.routeLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(price?.let { "Заказать за ${it.total.toInt()} ₽" } ?: "Выберите откуда и куда")
        }
    }
}

@Composable
private fun AddressRow(label: String, value: String?, picking: Boolean, onClick: () -> Unit, onPickOnMap: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.clickable(onClick = onClick).weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(value ?: "Ввести адрес", style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = onPickOnMap) {
                    Text(if (picking) "Жду тап…" else "На карте")
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentMethod.entries.forEach { method ->
            FilterChip(selected = selected == method, onClick = { onSelect(method) }, label = { Text(method.label) })
        }
    }
}

@Composable
private fun RouteMap(
    from: PlacePoint?,
    to: PlacePoint?,
    route: DirectionsClient.RouteResult?,
    pickTarget: PickTarget?,
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    onMapTap: (LatLng) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(from?.latLng ?: TASHKENT, 12f)
    }
    LaunchedEffect(from, to) {
        val target = to?.latLng ?: from?.latLng ?: TASHKENT
        val zoom = if (from != null && to != null) 12f else if (from != null) 14f else 11f
        cameraPositionState.position = CameraPosition.fromLatLngZoom(target, zoom)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (pickTarget != null) {
            Text(
                "Нажмите на карту, чтобы выбрать точку «${if (pickTarget == PickTarget.FROM) "Откуда" else "Куда"}»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Box {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = locationGranted),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
                onMapClick = onMapTap
            ) {
                from?.let { Marker(state = MarkerState(position = it.latLng), title = "Откуда") }
                to?.let { Marker(state = MarkerState(position = it.latLng), title = "Куда") }
                route?.polyline?.takeIf { it.isNotEmpty() }?.let { Polyline(points = it) }
            }
            if (!locationGranted) {
                FilledIconButton(
                    onClick = onRequestLocation,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Показать моё местоположение")
                }
            }
        }
    }
}

@Composable
private fun PriceBreakdownView(price: PriceBreakdown) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Оценочная цена (окончательную посчитает сервер)", fontWeight = FontWeight.Bold)
        PriceRow("Посадка", price.baseFare)
        PriceRow("За расстояние", price.distanceCost)
        PriceRow("За время в пути", price.timeCost)
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
                Text("Стоимость: ${ride.price.total.toInt()} ₽ · ${ride.paymentMethod.label}")
                if (ride.paymentMethod == PaymentMethod.CLICK &&
                    (ride.status == RideStatus.ACCEPTED || ride.status == RideStatus.IN_PROGRESS)
                ) {
                    Text(
                        "Click для оплаты: ${ride.driverClickHandle ?: "водитель ещё не указал реквизиты"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        when (ride.status) {
            RideStatus.COMPLETED -> {
                Text("Поездка завершена. Спасибо!")
                Button(onClick = onNewOrder, modifier = Modifier.fillMaxWidth()) { Text("Заказать снова") }
            }
            RideStatus.CANCELLED -> {
                Button(onClick = onNewOrder, modifier = Modifier.fillMaxWidth()) { Text("Заказать снова") }
            }
            else -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
