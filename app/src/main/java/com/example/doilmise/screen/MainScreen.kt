package com.example.doilmise.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.doilmise.MainViewModel
import com.example.doilmise.R
import com.example.doilmise.data.Grade
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import android.app.Activity
import android.content.ContextWrapper
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.core.app.ActivityCompat

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val pm10Grade by viewModel.pm10Grade.collectAsState()
    val pm25Grade by viewModel.pm25Grade.collectAsState()
    val o3Grade by viewModel.o3Grade.collectAsState()

    val dustData by viewModel.dustData.collectAsState()
    val locationText by viewModel.locationAddress.observeAsState("위치 정보를 로딩 중입니다...")
    val imageUris by viewModel.imageUris.collectAsState()

    val context = LocalContext.current

    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainScreen", "Permission granted by user, initiating data fetch")
            viewModel.updateLocationPermissionState(true)
            // 권한이 허용되면 즉시 데이터 요청
            viewModel.handlePermissionGranted()
        } else {
            Log.d("MainScreen", "Permission denied by user")
            viewModel.updateLocationPermissionState(false)
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
    fun requestLocationPermission() {
        when {
            // 권한이 이미 허용된 경우
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.updateLocationPermissionState(true)
                viewModel.fetchAirQualityData()
            }
            // 권한 요청을 명시적으로 거부한 경우 (설정으로 이동)
            !ActivityCompat.shouldShowRequestPermissionRationale(
                context as ComponentActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // 설정으로 이동하는 Intent 실행
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
            // 처음 권한 요청하는 경우
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Log.d("MainScreen", "Permission already granted, initializing...")
                viewModel.onPermissionGranted()
            }

            else -> {
                Log.d("MainScreen", "Requesting permission...")
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        Log.d("MainScreen", "Permission state changed: $locationPermissionGranted")
        if (locationPermissionGranted) {
            viewModel.getLocation()
        }
    }

    var showDialog by remember { mutableStateOf(false) }

    val highestGrade = remember(pm10Grade, pm25Grade) {
        listOf(pm10Grade, pm25Grade)
            .filterNot { it == Grade.UNKNOWN }
            .maxByOrNull { it.ordinal } ?: Grade.NORMAL
    }

    val airQualityInfo = when (highestGrade) {
        Grade.BEST -> Triple(
            R.drawable.best,
            Pair(Color(0xFF2b75bb), "오늘은 산책 하기 좋은 날씨예요!"),
            Color(0xFF1a5c9e)  // Row 배경색
        )
        Grade.GOOD -> Triple(
            R.drawable.good,
            Pair(Color(0xFF2899d4), "공기가 좋아 실외 활동 하기 좋습니다."),
            Color(0xFF1980b9)  // Row 배경색
        )
        Grade.FAIR -> Triple(
            R.drawable.fair,
            Pair(Color(0xFF16adc2), "공기 상태가 양호 하니 편하게 외출할 수 있습니다."),
            Color(0xFF0f94a7)  // Row 배경색
        )
        Grade.NORMAL -> Triple(
            R.drawable.normal,
            Pair(Color(0xFF349043), "평소와 같이 편하게 활동하실 수 있습니다."),
            Color(0xFF277734)  // Row 배경색
        )
        Grade.BAD -> Triple(
            R.drawable.bad,
            Pair(Color(0xFFf68d1e), "실외 활동을 자제 하는 것이 좋겠습니다."),
            Color(0xFFd97915)  // Row 배경색
        )
        Grade.VERY_BAD -> Triple(
            R.drawable.very_bad,
            Pair(Color(0xFFe74d25), "마스크 착용이 필요한 상황입니다."),
            Color(0xFFc93c16)  // Row 배경색
        )
        Grade.EXTREMELY_BAD -> Triple(
            R.drawable.extreamly_bad,
            Pair(Color(0xFFd52e2f), "실외 활동을 최대한 자제해 주시기 바랍니다."),
            Color(0xFFb81d1e)  // Row 배경색
        )
        Grade.WORST -> Triple(
            R.drawable.worst,
            Pair(Color(0xFF212121), "실외 활동을 최대한 자제해 주시기 바랍니다."),
            Color(0xFF141414)  // Row 배경색
        )
        else -> Triple(
            R.drawable.normal,
            Pair(Color(0xFFa475d5), ""),
            Color(0xFF8c5cbc)  // Row 배경색
        )
    }
    Log.d("highestGrade", "MainScreen:$highestGrade ")

    val backgroundColor = airQualityInfo.second.first
    val subscriptions = airQualityInfo.second.second

    val imageUri = imageUris[highestGrade.name] ?: airQualityInfo.first
    Log.d("MainScreen", "Current imageUri: $imageUri")
    val launcherForBest =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.BEST.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForGood =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.GOOD.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForFair =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.FAIR.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForNormal =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.NORMAL.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForBad =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.BAD.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForVeryBad =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.VERY_BAD.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForExtremelyBad =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.EXTREMELY_BAD.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForWorst =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.updateImageUri(Grade.WORST.name, it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "공기질 상태 선택") },
            text = { Text("이미지를 저장할 공기질 상태를 선택하세요.") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        launcherForBest.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("최고 좋음") }

                    TextButton(onClick = {
                        launcherForGood.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("좋음") }

                    TextButton(onClick = {
                        launcherForFair.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("양호") }

                    TextButton(onClick = {
                        launcherForNormal.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("보통") }

                    TextButton(onClick = {
                        launcherForBad.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("나쁨") }

                    TextButton(onClick = {
                        launcherForVeryBad.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("상당히 나쁨") }

                    TextButton(onClick = {
                        launcherForExtremelyBad.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("매우 매우 나쁨") }

                    TextButton(onClick = {
                        launcherForWorst.launch(arrayOf("image/*"))
                        showDialog = false
                    }) { Text("최악") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isLoading),
        onRefresh = {
            if (locationPermissionGranted) {
                viewModel.fetchAirQualityData()
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    ) {
        if (!locationPermissionGranted) {
            // 권한이 없는 경우 UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "위치 권한이 필요합니다",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            requestLocationPermission()
                        }
                    ) {
                        Text("시작하기 및 권한설정하기")
                    }
                }
            }
        } else {
            // 로딩 상태를 표시하기 위한 UI 추가
            if (viewModel.isLoading.collectAsState().value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(backgroundColor),
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        {
                            val dateTimeText = dustData?.dataTime ?: "데이터를 불러오는 중입니다..."
                            //TODO{change font style }

                            Text(
                                text = locationText,
                                fontSize = 28.sp,
                                color = Color.White,
                                modifier = Modifier.padding(top = 60.dp, bottom = 5.dp)
                            )

                            Text(
                                text = dateTimeText,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp) // 아래쪽 패딩 추가
                            )

                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(300.dp)
                                    .clip(CircleShape)
                                    .clickable { showDialog = true },

                                onError = { error ->
                                    Log.e("ImageLoadError", "Error loading image: $error")
                                }
                            )


                            Text(
                                text = highestGrade.toString(),
                                fontSize = 43.sp,
                                color = Color.White,
                                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                            )

                            Text(
                                text = subscriptions,
                                fontSize = 20.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .background(
                                        color = airQualityInfo.third,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    // 그림자 효과 추가
                                    .drawBehind {
                                        drawIntoCanvas {
                                            val shadowColor = Color.Black
                                            val transparentColor = Color.Transparent
                                            val shadowRadius = 16.dp.toPx()
                                            val shadowOffset = 2.dp.toPx()

                                            // 위쪽 그림자
                                            val topGradient = Brush.verticalGradient(
                                                colors = listOf(shadowColor.copy(alpha = 0.1f), transparentColor),
                                                startY = 0f,
                                                endY = shadowRadius
                                            )
                                            drawRect(
                                                brush = topGradient,
                                                topLeft = Offset(0f, -shadowOffset),
                                                size = Size(size.width, shadowRadius)
                                            )

                                            // 아래쪽 그림자
                                            val bottomGradient = Brush.verticalGradient(
                                                colors = listOf(transparentColor, shadowColor.copy(alpha = 0.1f)),
                                                startY = size.height - shadowRadius,
                                                endY = size.height
                                            )
                                            drawRect(
                                                brush = bottomGradient,
                                                topLeft = Offset(0f, size.height - shadowRadius + shadowOffset),
                                                size = Size(size.width, shadowRadius)
                                            )

                                            // 왼쪽 그림자
                                            val leftGradient = Brush.horizontalGradient(
                                                colors = listOf(shadowColor.copy(alpha = 0.1f), transparentColor),
                                                startX = 0f,
                                                endX = shadowRadius
                                            )
                                            drawRect(
                                                brush = leftGradient,
                                                topLeft = Offset(-shadowOffset, 0f),
                                                size = Size(shadowRadius, size.height)
                                            )

                                            // 오른쪽 그림자
                                            val rightGradient = Brush.horizontalGradient(
                                                colors = listOf(transparentColor, shadowColor.copy(alpha = 0.1f)),
                                                startX = size.width - shadowRadius,
                                                endX = size.width
                                            )
                                            drawRect(
                                                brush = rightGradient,
                                                topLeft = Offset(size.width - shadowRadius + shadowOffset, 0f),
                                                size = Size(shadowRadius, size.height)
                                            )
                                        }
                                    }
                                    .padding(16.dp) // 내부 패딩 추가
                            )  {
                                Box(
                                    modifier = Modifier
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "미세먼지",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )

                                        val pm10AirQualityInfo = when (pm10Grade) {
                                            Grade.BEST -> Pair(R.drawable.best, Grade.BEST.label)
                                            Grade.GOOD -> Pair(R.drawable.good, Grade.GOOD.label)
                                            Grade.FAIR -> Pair(R.drawable.fair, Grade.FAIR.label)
                                            Grade.NORMAL -> Pair(
                                                R.drawable.normal,
                                                Grade.NORMAL.label
                                            )

                                            Grade.BAD -> Pair(R.drawable.bad, Grade.BAD.label)
                                            Grade.VERY_BAD -> Pair(
                                                R.drawable.very_bad,
                                                Grade.VERY_BAD.label
                                            )

                                            Grade.EXTREMELY_BAD -> Pair(
                                                R.drawable.extreamly_bad,
                                                Grade.EXTREMELY_BAD.label
                                            )

                                            Grade.WORST -> Pair(R.drawable.worst, Grade.WORST.label)
                                            else -> Pair(R.drawable.normal, Grade.UNKNOWN.label)
                                        }

                                        Image(
                                            painter = painterResource(id = pm10AirQualityInfo.first),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(5.dp)
                                        )

                                        Text(

                                            text = pm10AirQualityInfo.second,
                                            fontSize = 16.sp,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "${dustData?.pm10Value}㎍/㎥",
                                            fontSize = 13.sp,
                                            color = Color.White,
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "초미세먼지",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        // 초미세먼지 (pm25) 관련 정보
                                        val pm25AirQualityInfo = when (pm25Grade) {
                                            Grade.BEST -> Pair(R.drawable.best, Grade.BEST.label)
                                            Grade.GOOD -> Pair(R.drawable.good, Grade.GOOD.label)
                                            Grade.FAIR -> Pair(R.drawable.fair, Grade.FAIR.label)
                                            Grade.NORMAL -> Pair(
                                                R.drawable.normal,
                                                Grade.NORMAL.label
                                            )

                                            Grade.BAD -> Pair(R.drawable.bad, Grade.BAD.label)
                                            Grade.VERY_BAD -> Pair(
                                                R.drawable.very_bad,
                                                Grade.VERY_BAD.label
                                            )

                                            Grade.EXTREMELY_BAD -> Pair(
                                                R.drawable.extreamly_bad,
                                                Grade.EXTREMELY_BAD.label
                                            )

                                            Grade.WORST -> Pair(R.drawable.worst, Grade.WORST.label)
                                            else -> Pair(R.drawable.normal, Grade.UNKNOWN.label)
                                        }

                                        Image(
                                            painter = painterResource(id = pm25AirQualityInfo.first),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(5.dp)

                                        )

                                        Text(

                                            text = pm25AirQualityInfo.second,
                                            fontSize = 16.sp,
                                            color = Color.White,
                                        )

                                        Text(
                                            text = "${dustData?.pm25Value}㎍/㎥",
                                            fontSize = 13.sp,
                                            color = Color.White,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "오존",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )

                                        // 오존 (o3) 관련 정보
                                        val o3AirQualityInfo = when (o3Grade) {
                                            Grade.BEST -> Pair(R.drawable.best, Grade.BEST.label)
                                            Grade.GOOD -> Pair(R.drawable.good, Grade.GOOD.label)
                                            Grade.FAIR -> Pair(R.drawable.fair, Grade.FAIR.label)
                                            Grade.NORMAL -> Pair(
                                                R.drawable.normal,
                                                Grade.NORMAL.label
                                            )

                                            Grade.BAD -> Pair(R.drawable.bad, Grade.BAD.label)
                                            Grade.VERY_BAD -> Pair(
                                                R.drawable.very_bad,
                                                Grade.VERY_BAD.label
                                            )

                                            Grade.EXTREMELY_BAD -> Pair(
                                                R.drawable.extreamly_bad,
                                                Grade.EXTREMELY_BAD.label
                                            )

                                            Grade.WORST -> Pair(R.drawable.worst, Grade.WORST.label)
                                            else -> Pair(R.drawable.normal, Grade.UNKNOWN.label)
                                        }

                                        Image(
                                            painter = painterResource(id = o3AirQualityInfo.first),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(5.dp)
                                        )
                                        Text(
                                            text = o3AirQualityInfo.second,
                                            fontSize = 16.sp,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "${dustData?.o3Value}ppm",
                                            fontSize = 13.sp,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                            BannersAds()
                        }
                    }
                }
            }
        }
    }
}
