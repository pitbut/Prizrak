package com.pit.bahromtaxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pit.bahromtaxi.ui.BahromTaxiTheme
import com.pit.bahromtaxi.ui.RoleSelectScreen
import com.pit.bahromtaxi.ui.client.ClientScreen
import com.pit.bahromtaxi.ui.client.ClientViewModel
import com.pit.bahromtaxi.ui.driver.DriverScreen
import com.pit.bahromtaxi.ui.driver.DriverViewModel

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
                        ClientScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable("driver") {
                        val vm: DriverViewModel = viewModel()
                        DriverScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
