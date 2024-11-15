package com.custommise.doilmise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.custommise.doilmise.location.LocationApp
import com.custommise.doilmise.location.RequestPermissionsUtil
import com.custommise.doilmise.screen.MainScreen
import com.custommise.doilmise.ui.theme.DoilmiseTheme

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