package com.pit.bahromtaxi.ui.client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.domain.OrderType
import com.pit.bahromtaxi.domain.PaymentMethod
import com.pit.bahromtaxi.domain.PriceBreakdown
import com.pit.bahromtaxi.domain.Ride
import com.pit.bahromtaxi.domain.RideStatus
import com.pit.bahromtaxi.domain.label
import com.pit.bahromtaxi.maps.Coordinate
import com.pit.bahromtaxi.maps.NominatimClient
import com.pit.bahromtaxi.maps.OsrmClient
import com.pit.bahromtaxi.maps.PickTarget
import com.pit.bahromtaxi.maps.PlacePoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

private val TASHKENT = Coordinate(41.2995, 69.2401)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current

    var registered by remember { mutableStateOf(viewModel.isRegistered) }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var searchTarget by remember { mutableStateOf<PickTarget?>(null) }

    val rides by viewModel.rides.collectAsState()
    val error by viewModel.lastError.collectAsState()
    val activeRide = rides.find { it.id == viewModel.activeRideId }

    LaunchedEffect(registered) {
        if (registered) RideRepository.ensureConnected()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> locationGranted = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пассажир") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (registered) {
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Filled.History, contentDescription = "История поездок")
                        }
                        IconButton(onClick = onOpenProfile) {
                            Icon(Icons.Filled.Person, contentDescription = "Кабинет")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            when {
                !registered -> PhoneAuthGate(viewModel = viewModel, onDone = { registered = true })
                activeRide == null || activeRide.status == RideStatus.CANCELLED -> OrderForm(
                    viewModel = viewModel,
                    locationGranted = locationGranted,
                    onSearchFrom = { searchTarget = PickTarget.FROM },
                    onSearchTo = { searchTarget = PickTarget.TO },
                    onRequestLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
                else -> ActiveRideCard(activeRide, onNewOrder = { viewModel.resetOrder() }, onOpenChat = { onOpenChat(activeRide.id) })
            }
        }
    }

    searchTarget?.let { target ->
        AddressSearchDialog(
            title = if (target == PickTarget.FROM) "Откуда" else "Куда",
            onDismiss = { searchTarget = null },
            onSelect = { place ->
                if (target == PickTarget.FROM) viewModel.chooseFromPlace(place) else viewModel.chooseToPlace(place)
                searchTarget = null
            }
        )
    }
}

@Composable
private fun PhoneAuthGate(viewModel: ClientViewModel, onDone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        viewModel.authError?.let {
            SelectionContainer {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        when (viewModel.authStep) {
            AuthStep.PHONE -> {
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
                        onClick = { viewModel.sendCode() },
                        enabled = !viewModel.authLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (viewModel.authLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Получить код")
                    }
                    TextButton(onClick = { viewModel.showEmailForm = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Войти по email вместо телефона")
                    }
                } else {
                    val isRegister = viewModel.emailAuthMode == EmailAuthMode.REGISTER
                    Text(if (isRegister) "Регистрация по email" else "Вход по email", fontWeight = FontWeight.Bold)
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
                        if (viewModel.authLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        else Text(if (isRegister) "Зарегистрироваться" else "Войти")
                    }
                    TextButton(onClick = { viewModel.toggleEmailAuthMode() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isRegister) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться")
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
            AuthStep.CODE -> {
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
            AuthStep.VERIFY_EMAIL -> {
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
            AuthStep.PROFILE -> {
                Text("Как к вам обращаться?", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = viewModel.nameInput,
                    onValueChange = { viewModel.nameInput = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
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
private fun AddressSearchDialog(title: String, onDismiss: () -> Unit, onSelect: (PlacePoint) -> Unit) {
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
        NominatimClient.search(trimmed)
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

@Composable
private fun OrderForm(
    viewModel: ClientViewModel,
    locationGranted: Boolean,
    onSearchFrom: () -> Unit,
    onSearchTo: () -> Unit,
    onRequestLocation: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderType.entries.forEach { type ->
                FilterChip(
                    selected = viewModel.orderType == type,
                    onClick = { viewModel.orderType = type },
                    label = { Text(type.label) }
                )
            }
        }

        AddressRow(
            label = "Откуда",
            value = viewModel.fromPlace?.address,
            picking = viewModel.pickTarget == PickTarget.FROM,
            onClick = onSearchFrom,
            onPickOnMap = { viewModel.startPicking(PickTarget.FROM) }
        )
        AddressRow(
            label = "Куда",
            value = viewModel.toPlace?.address,
            picking = viewModel.pickTarget == PickTarget.TO,
            onClick = onSearchTo,
            onPickOnMap = { viewModel.startPicking(PickTarget.TO) }
        )

        if (viewModel.orderType == OrderType.DELIVERY) {
            OutlinedTextField(
                value = viewModel.senderPhoneInput,
                onValueChange = { viewModel.senderPhoneInput = it },
                label = { Text("Телефон отправителя") },
                placeholder = { Text("+998901234567") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = viewModel.receiverPhoneInput,
                onValueChange = { viewModel.receiverPhoneInput = it },
                label = { Text("Телефон получателя") },
                placeholder = { Text("+998901234567") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (viewModel.orderType == OrderType.CARGO) {
            OutlinedTextField(
                value = viewModel.cargoDescriptionInput,
                onValueChange = { viewModel.cargoDescriptionInput = it },
                label = { Text("Что нужно перевезти") },
                placeholder = { Text("Например: диван, 2 коробки") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.cargoWeightInput,
                onValueChange = { viewModel.cargoWeightInput = it },
                label = { Text("Вес, кг (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        viewModel.orderError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        OsmRouteMap(
            from = viewModel.fromPlace,
            to = viewModel.toPlace,
            route = viewModel.route,
            pickTarget = viewModel.pickTarget,
            locationGranted = locationGranted,
            onRequestLocation = onRequestLocation,
            onMapTap = { coordinate ->
                if (viewModel.pickTarget == null) return@OsmRouteMap
                coroutineScope.launch {
                    val address = NominatimClient.reverse(coordinate).getOrElse {
                        "Точка на карте (%.5f, %.5f)".format(Locale.US, coordinate.lat, coordinate.lng)
                    }
                    viewModel.setPointFromMap(PlacePoint(address, coordinate))
                }
            }
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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

@Composable
private fun PaymentMethodRow(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentMethod.entries.forEach { method ->
            FilterChip(selected = selected == method, onClick = { onSelect(method) }, label = { Text(method.label) })
        }
    }
}

@Composable
private fun OsmRouteMap(
    from: PlacePoint?,
    to: PlacePoint?,
    route: OsrmClient.RouteResult?,
    pickTarget: PickTarget?,
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    onMapTap: (Coordinate) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Зум щипком и так работает — встроенные +/- кнопки только мешают и
            // налезают на кнопку "моё местоположение".
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(TASHKENT.lat, TASHKENT.lng))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    // Пересчитываем центр/зум только когда правда меняются точки — а не на каждой
    // перерисовке экрана (иначе карта дёргается и сбрасывает ручной поворот/зум).
    LaunchedEffect(from, to) {
        val target = to?.coordinate ?: from?.coordinate
        if (target != null) {
            mapView.controller.animateTo(GeoPoint(target.lat, target.lng))
            mapView.controller.setZoom(if (from != null && to != null) 12.0 else 14.0)
        }
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
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxWidth().height(220.dp).clipToBounds(),
                update = { map ->
                    map.overlays.clear()

                    map.overlays.add(
                        MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                onMapTap(Coordinate(p.latitude, p.longitude))
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint) = false
                        })
                    )

                    from?.let {
                        map.overlays.add(
                            Marker(map).apply {
                                position = GeoPoint(it.coordinate.lat, it.coordinate.lng)
                                title = "Откуда"
                            }
                        )
                    }
                    to?.let {
                        map.overlays.add(
                            Marker(map).apply {
                                position = GeoPoint(it.coordinate.lat, it.coordinate.lng)
                                title = "Куда"
                            }
                        )
                    }
                    route?.polyline?.takeIf { it.isNotEmpty() }?.let { pts ->
                        map.overlays.add(
                            Polyline().apply { setPoints(pts.map { GeoPoint(it.lat, it.lng) }) }
                        )
                    }
                    if (locationGranted) {
                        map.overlays.add(
                            MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply { enableMyLocation() }
                        )
                    }

                    map.invalidate()
                }
            )
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
private fun ActiveRideCard(ride: Ride, onNewOrder: () -> Unit, onOpenChat: () -> Unit) {
    val context = LocalContext.current
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
                if (ride.status == RideStatus.ACCEPTED || ride.status == RideStatus.IN_PROGRESS) {
                    if (ride.driverPhone != null) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ride.driverPhone}")))
                        }) {
                            Text("Позвонить водителю: ${ride.driverPhone}")
                        }
                    }
                    TextButton(onClick = onOpenChat) {
                        Text("Чат с водителем")
                    }
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
