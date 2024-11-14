package com.example.doilmise.screen

import android.Manifest
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.doilmise.MainViewModel
import com.example.doilmise.R
import com.example.doilmise.data.Grade
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.requestLocation() // 권한이 허용된 경우 위치 요청
        } else {
            // 권한이 거부된 경우 처리할 작업 추가 가능
            Log.d("MainScreen", "Location per mission denied")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeData()
    }

    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var showDialog by remember { mutableStateOf(false) }

    val highestGrade = remember(pm10Grade, pm25Grade, o3Grade) {
        listOf(pm10Grade, pm25Grade, o3Grade)
            .filterNot { it == Grade.UNKNOWN }
            .maxByOrNull { it.ordinal } ?: Grade.NORMAL
    }


    // 이미지 리소스 설정


    val airQualityInfo = when (highestGrade) {
        Grade.BEST -> Pair(R.drawable.best, Pair(Color(0xFF2b75bb), "산책가도 좋을 날씨네요!"))
        Grade.GOOD -> Pair(R.drawable.good, Pair(Color(0xFF2899d4), "좋음가도 좋을 날씨네요!"))
        Grade.FAIR -> Pair(R.drawable.fair, Pair(Color(0xFF16adc2), "양호가도 좋을 날씨네요!"))
        Grade.NORMAL -> Pair(R.drawable.normal, Pair(Color(0xFF349043), "보통가도 좋을 날씨네요!"))
        Grade.BAD -> Pair(R.drawable.bad, Pair(Color(0xFFf68d1e), "민감하신 분들은 주의하세요."))
        Grade.VERY_BAD -> Pair(R.drawable.very_bad, Pair(Color(0xFFe74d25), "마스크 챙기셨죠?"))
        Grade.EXTREMELY_BAD -> Pair(
            R.drawable.extreamly_bad,
            Pair(Color(0xFFd52e2f), "외출은 최대한 피해주세요 ㅠㅠ")
        )

        Grade.WORST -> Pair(R.drawable.worst, Pair(Color(0xFF212121), "외출은 최대한 피해주세요 ㅠㅠ"))
        else -> Pair(R.drawable.normal, Pair(Color(0xFFa475d5), "")) // 기본 색상
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

    val swipeRefreshState =
        rememberSwipeRefreshState(isRefreshing = viewModel.isLoading.collectAsState().value)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = {
            viewModel.getLocation() // 스와이프 시 데이터 로드
        }
    ) {
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
                                .padding(start = 20.dp, end = 20.dp),
                        ) {
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
                                        Grade.NORMAL -> Pair(R.drawable.normal, Grade.NORMAL.label)
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
                                        Grade.NORMAL -> Pair(R.drawable.normal, Grade.NORMAL.label)
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
                                        Grade.NORMAL -> Pair(R.drawable.normal, Grade.NORMAL.label)
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
                    }
                }
            }
        }
    }
}
