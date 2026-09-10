package com.pit.bahromtaxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pit.bahromtaxi.ui.BahromTaxiTheme
import com.pit.bahromtaxi.ui.RoleSelectScreen
import com.pit.bahromtaxi.ui.chat.ChatScreen
import com.pit.bahromtaxi.ui.chat.ChatViewModel
import com.pit.bahromtaxi.ui.client.ClientScreen
import com.pit.bahromtaxi.ui.client.ClientViewModel
import com.pit.bahromtaxi.ui.driver.DriverScreen
import com.pit.bahromtaxi.ui.driver.DriverViewModel
import com.pit.bahromtaxi.ui.history.RideHistoryScreen
import com.pit.bahromtaxi.ui.history.RideHistoryViewModel
import com.pit.bahromtaxi.ui.profile.ProfileScreen
import com.pit.bahromtaxi.ui.profile.ProfileViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BahromTaxiTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "role") {
                    composable("role") {
                        RoleSelectScreen(
                            onSelectClient = { navController.navigate("client") },
                            onSelectDriver = { navController.navigate("driver") }
                        )
                    }
                    composable("client") {
                        val vm: ClientViewModel = viewModel()
                        ClientScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onOpenProfile = { navController.navigate("profile") },
                            onOpenHistory = { navController.navigate("history") },
                            onOpenChat = { rideId -> navController.navigate("chat/$rideId") }
                        )
                    }
                    composable("driver") {
                        val vm: DriverViewModel = viewModel()
                        DriverScreen(
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
}
