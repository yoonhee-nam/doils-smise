@file:Suppress("DEPRECATION")

package com.example.doilmise

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import coil.compose.rememberAsyncImagePainter

import coil.compose.rememberImagePainter
import com.example.doilmise.location.LocationApp
import com.example.doilmise.location.RequestPermissionsUtil
import com.example.doilmise.ui.theme.DoilmiseTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RequestPermissionsUtil(this).requestLocation()

        setContent {
            viewModel.requestLocation()
            DoilmiseTheme {
                LocationApp(viewModel)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF9ED2EC)
                ) {
                    MainInfo(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainInfo(viewModel: MainViewModel) {
    val airQualityClassification by viewModel.airQualityClassification.collectAsState()
    val dustData by viewModel.dustData.collectAsState()
    val locationText by viewModel.locationAddress.observeAsState("위치 정보를 로딩 중입니다...")
    val imageUris by viewModel.imageUris.collectAsState()

    // 이미지 선택기를 설정
    val launcherForGood =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.updateImageUri("좋음", it) }
        }
    val launcherForNormal =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.updateImageUri("보통", it) }
        }
    val launcherForBad =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.updateImageUri("나쁨", it) }
        }
    val launcherForVeryBad =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.updateImageUri("매우 나쁨", it) }
        }

    var showDialog by remember { mutableStateOf(false) }

    // 이미지 리소스와 배경색을 설정하는 함수
    val imageUri = imageUris[airQualityClassification]
    val defaultImageResId = when (airQualityClassification) {
        "좋음" -> R.drawable.good
        "보통" -> R.drawable.soso
        "나쁨" -> R.drawable.bad
        "매우 나쁨" -> R.drawable.terrible
        else -> R.drawable.base
    }
    val subscriptions = when (airQualityClassification) {
        "좋음" -> "산책가도 좋을 날씨네요!"
        "보통" -> "민감하신 분들은 주의하세요."
        "나쁨" -> "마스크 챙기셨죠?"
        "매우 나쁨" -> "외출은 최대한 피해주세요 ㅠㅠ"
        else -> ""
    }


    // 이미지 선택 다이얼로그
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "공기질 상태 선택") },
            text = { Text("이미지를 저장할 공기질 상태를 선택하세요.") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        launcherForGood.launch("image/*")
                        showDialog = false
                    }) { Text("좋음") }
                    TextButton(onClick = {
                        launcherForNormal.launch("image/*")
                        showDialog = false
                    }) { Text("보통") }
                    TextButton(onClick = {
                        launcherForBad.launch("image/*")
                        showDialog = false
                    }) { Text("나쁨") }
                    TextButton(onClick = {
                        launcherForVeryBad.launch("image/*")
                        showDialog = false
                    }) { Text("매우 나쁨") }
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
                .background(Color.White)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, shape = RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally

                )
                {

                    val dateTimeText = dustData?.dataTime ?: "데이터를 불러오는 중입니다..."
                    val dataText = dustData?.khaiValue ?: "데이터를 불러오는 중입니다..."

                    Text(
                        text = locationText,
                        fontSize = 24.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 80.dp,bottom = 8.dp)
                    )

                    Text(
                        text = dateTimeText,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp) // 아래쪽 패딩 추가
                    )

                    Image(
                        painter = rememberImagePainter(data = imageUri ?: defaultImageResId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .clickable { showDialog = true } // 클릭 시 다이얼로그 표시
                            .padding(bottom = 10.dp) // 아래쪽 패딩 추가
                    )

                    Text(
                        text = airQualityClassification,
                        fontSize = 50.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 30.dp) // 위쪽 패딩 추가
                    )

                    Text(
                        text = subscriptions,
                        fontSize = 23.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 10.dp) // 위쪽 패딩 추가
                    )

                    Text(
                        text = "$dataText ㎍/㎥",
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp) // 아래쪽 패딩 추가
                    )


                }
            }
        }
    }
}

