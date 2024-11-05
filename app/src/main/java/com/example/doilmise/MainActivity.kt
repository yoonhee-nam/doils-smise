package com.example.doilmise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.doilmise.location.LocationApp
import com.example.doilmise.location.RequestPermissionsUtil
import com.example.doilmise.screen.MainScreen
import com.example.doilmise.ui.theme.DoilmiseTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val permissionsUtil = RequestPermissionsUtil(this)

        if (!permissionsUtil.isLocationPermitted()) {
            permissionsUtil.requestLocation()
        }
        setContent {
            viewModel.requestLocation()
            DoilmiseTheme {
                LocationApp(viewModel)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}