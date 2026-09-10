package com.pit.bahromtaxi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pit.bahromtaxi.maps.Coordinate
import com.pit.bahromtaxi.maps.GeoUri
import com.pit.bahromtaxi.ui.BahromTaxiTheme
import com.pit.bahromtaxi.ui.RoleSelectScreen
import com.pit.bahromtaxi.ui.carrier.CarrierScreen
import com.pit.bahromtaxi.ui.carrier.CarrierViewModel
import com.pit.bahromtaxi.ui.chat.ChatScreen
import com.pit.bahromtaxi.ui.chat.ChatViewModel
import com.pit.bahromtaxi.ui.client.ClientScreen
import com.pit.bahromtaxi.ui.client.ClientViewModel
import com.pit.bahromtaxi.ui.driver.DriverScreen
import com.pit.bahromtaxi.ui.driver.DriverViewModel
import com.pit.bahromtaxi.ui.history.RideHistoryScreen
import com.pit.bahromtaxi.ui.history.RideHistoryViewModel
import com.pit.bahromtaxi.ui.intercity.IntercityDriverScreen
import com.pit.bahromtaxi.ui.intercity.IntercityDriverViewModel
import com.pit.bahromtaxi.ui.intercity.IntercityPassengerScreen
import com.pit.bahromtaxi.ui.intercity.IntercityPassengerViewModel
import com.pit.bahromtaxi.ui.grouptrip.GroupTripDriverScreen
import com.pit.bahromtaxi.ui.grouptrip.GroupTripDriverViewModel
import com.pit.bahromtaxi.ui.grouptrip.GroupTripPassengerScreen
import com.pit.bahromtaxi.ui.grouptrip.GroupTripPassengerViewModel
import com.pit.bahromtaxi.ui.profile.ProfileScreen
import com.pit.bahromtaxi.ui.profile.ProfileViewModel

class MainActivity : ComponentActivity() {

    private var pendingLocation by mutableStateOf<Coordinate?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Без этого разрешения (Android 13+) напоминания-будильники перед отправлением поездки
        // молча не покажутся — сами будильники (AlarmManager) при этом всё равно сработают.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        pendingLocation = intent?.data?.let(GeoUri::parse)
        setContent {
            BahromTaxiTheme {
                val navController = rememberNavController()
                // Локация, которой поделились из другого приложения (Telegram и т.п.), — сразу
                // ведём в экран пассажира, чтобы предложить использовать её как "Откуда"/"Куда".
                LaunchedEffect(pendingLocation) {
                    if (pendingLocation != null) {
                        navController.navigate("client") { launchSingleTop = true }
                    }
                }
                NavHost(navController = navController, startDestination = "role") {
                    composable("role") {
                        RoleSelectScreen(
                            onSelectClient = { navController.navigate("client") },
                            onSelectDriver = { navController.navigate("driver") },
                            onSelectCarrier = { navController.navigate("carrier") }
                        )
                    }
                    composable("client") {
                        val vm: ClientViewModel = viewModel()
                        ClientScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onOpenProfile = { navController.navigate("profile") },
                            onOpenHistory = { navController.navigate("history") },
                            onOpenChat = { rideId -> navController.navigate("chat/$rideId") },
                            onOpenIntercity = { navController.navigate("intercity_passenger") },
                            onOpenGroupTrip = { navController.navigate("group_trip_passenger") },
                            pendingLocation = pendingLocation,
                            onPendingLocationConsumed = { pendingLocation = null }
                        )
                    }
                    composable("driver") {
                        val vm: DriverViewModel = viewModel()
                        DriverScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onOpenProfile = { navController.navigate("profile") },
                            onOpenHistory = { navController.navigate("history") },
                            onOpenChat = { rideId -> navController.navigate("chat/$rideId") },
                            onOpenIntercity = { navController.navigate("intercity_driver") },
                            onOpenGroupTrip = { navController.navigate("group_trip_driver") }
                        )
                    }
                    composable("intercity_passenger") {
                        val vm: IntercityPassengerViewModel = viewModel()
                        IntercityPassengerScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable("intercity_driver") {
                        val vm: IntercityDriverViewModel = viewModel()
                        IntercityDriverScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable("group_trip_passenger") {
                        val vm: GroupTripPassengerViewModel = viewModel()
                        GroupTripPassengerScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable("group_trip_driver") {
                        val vm: GroupTripDriverViewModel = viewModel()
                        GroupTripDriverScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable("carrier") {
                        val vm: CarrierViewModel = viewModel()
                        CarrierScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onOpenProfile = { navController.navigate("profile") },
                            onOpenHistory = { navController.navigate("history") },
                            onOpenChat = { rideId -> navController.navigate("chat/$rideId") }
                        )
                    }
                    composable("profile") {
                        val vm: ProfileViewModel = viewModel()
                        ProfileScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onAccountDeleted = {
                                navController.navigate("role") {
                                    popUpTo("role") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("history") {
                        val vm: RideHistoryViewModel = viewModel()
                        RideHistoryScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable(
                        "chat/{rideId}",
                        arguments = listOf(navArgument("rideId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val rideId = backStackEntry.arguments?.getString("rideId") ?: return@composable
                        val vm: ChatViewModel = viewModel()
                        ChatScreen(viewModel = vm, rideId = rideId, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(GeoUri::parse)?.let { pendingLocation = it }
    }
}
