@file:Suppress("DEPRECATION")

package com.example.doilmise


import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val permissionsUtil = RequestPermissionsUtil(this)

        if (!permissionsUtil.isLocationPermitted()) {
            permissionsUtil.requestLocation()
        }

        if (!permissionsUtil.isMediaImagesPermitted()) {
            permissionsUtil.requestMediaImages()
        }
        setContent {
            viewModel.requestLocation()
            viewModel.requestMedia()

            DoilmiseTheme {
                LocationApp(viewModel)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF9ED2EC)
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}



