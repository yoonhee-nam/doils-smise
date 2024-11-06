package com.example.doilmise.screen

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.doilmise.MainViewModel
import com.example.doilmise.R
import com.example.doilmise.classifyAirQuality
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState



@Composable
fun MainScreen(viewModel: MainViewModel) {
    val airQualityClassification by viewModel.airQualityClassification.collectAsState()
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
            Log.d("MainScreen", "Location permission denied")
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val launcherForBest =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("최고 좋음", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForGood =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("좋음", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForFair =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("양호", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForNormal =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    viewModel.updateImageUri("보통", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForBad =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("나쁨", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForVeryBad =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("상당히 나쁨", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForExtremelyBad =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("매우 매우 나쁨", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    val launcherForWorst =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateImageUri("최악", it)
                } catch (e: SecurityException) {
                    Log.e("MainScreen", "Failed to take persistable URI permission", e)
                }
            }
        }

    var showDialog by remember { mutableStateOf(false) }

    // 이미지 리소스와 배경색을 설정하는 함수
    val imageUri = imageUris[airQualityClassification]?: R.drawable.base
    Log.d("MainScreen", "Current imageUri: $imageUri")


    val airQualityInfo = when (airQualityClassification) {
        "최고 좋음" -> Pair(R.drawable.good, Pair(Color(0xFF00FF9C), "산책가도 좋을 날씨네요!"))
        "좋음" -> Pair(R.drawable.good, Pair(Color(0xFF00FF9C), "좋음가도 좋을 날씨네요!"))
        "양호" -> Pair(R.drawable.good, Pair(Color(0xFF00FF9C), "양호가도 좋을 날씨네요!"))
        "보통" -> Pair(R.drawable.good, Pair(Color(0xFF00FF9C), "보통가도 좋을 날씨네요!"))
        "나쁨" -> Pair(R.drawable.good, Pair(Color(0xFF00FF9C), "나쁨가도 좋을 날씨네요!"))
        "상당히 나쁨" -> Pair(R.drawable.soso, Pair(Color(0xFFB7E0FF), "민감하신 분들은 주의하세요."))
        "매우 매우 나쁨" -> Pair(R.drawable.bad, Pair(Color(0xFFFFB2B2), "마스크 챙기셨죠?"))
        "최악" -> Pair(R.drawable.terrible, Pair(Color(0xFF4F1787), "외출은 최대한 피해주세요 ㅠㅠ"))
        else -> Pair(R.drawable.base, Pair(Color.Black, "")) // 기본 색상
    }

    val defaultImageResId = airQualityInfo.first
    val backgroundColor = airQualityInfo.second.first
    val subscriptions = airQualityInfo.second.second

    val airQualityData = classifyAirQuality("45", "25", "0.05")
    val pm10Value = (airQualityData["PM10"] as? Map<*, *>)?.get("value") ?: "데이터 없음"

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
            viewModel.loadDustInfo(viewModel.selectedArea.value ?: "") // 스와이프 시 데이터 로드
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor, shape = RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally

                )
                {

                    val dateTimeText = dustData?.dataTime ?: "데이터를 불러오는 중입니다..."
                    val dataText = dustData?.khaiValue ?: "데이터를 불러오는 중입니다..."

                    Text(
                        text = locationText,
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 80.dp,bottom = 8.dp)
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
                        text = airQualityClassification,
                        fontSize = 50.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 20.dp,bottom = 8.dp) // 위쪽 패딩 추가
                    )

                    Text(
                        text = subscriptions,
                        fontSize = 23.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 10.dp) // 위쪽 패딩 추가
                    )

                    Text(
                        text = "PM10: ${pm10Value}㎍/㎥",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp) // 아래쪽 패딩 추가
                    )
                }
            }
        }
    }
}