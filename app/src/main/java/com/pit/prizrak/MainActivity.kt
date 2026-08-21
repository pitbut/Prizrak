package com.pit.prizrak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pit.prizrak.ui.CameraScreen
import com.pit.prizrak.ui.PrizrakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrizrakTheme {
                CameraScreen()
            }
        }
    }
}
