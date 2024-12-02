package com.custommise.doilmise

import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.custommise.doilmise.location.LocationApp
import com.custommise.doilmise.location.RequestPermissionsUtil
import com.custommise.doilmise.screen.MainScreen
import com.custommise.doilmise.ui.theme.DoilmiseTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var permissionsUtil: RequestPermissionsUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionsUtil = RequestPermissionsUtil(this)

        viewModel.initialize(applicationContext)

        viewModel.updateNotificationPermissionState(permissionsUtil.isNotificationPermitted())

        permissionsUtil.requestAllPermissions()

        if (!permissionsUtil.isLocationPermitted()) {
            permissionsUtil.requestLocation()
        }

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            viewModel.requestLocation()
            DoilmiseTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    LocationApp(viewModel)
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }

            // Snackbar
            LaunchedEffect(snackbarHostState) {
                viewModel.showSnackbar.collect { show ->
                    if (show) {
                        val result = snackbarHostState.showSnackbar(
                            message = "알림이 거부되어 대기질 알림을 받을 수 없습니다.",
                            actionLabel = "권한 설정 하기",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            permissionsUtil.requestNotification()
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> { // 위치 권한
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.updateLocationPermissionState(true)
                    viewModel.requestLocation()
                }
            }
            2 -> { // 알림 권한
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.updateNotificationPermissionState(true)
                } else {
                    viewModel.updateNotificationPermissionState(false)
                    viewModel.showSnackbarMessage()
                }
            }
        }
    }
}