package com.example.doilmise

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout

import coil.compose.rememberImagePainter
import com.example.doilmise.ui.theme.DoilmiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoilmiseTheme {
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF9ED2EC)) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel = MainViewModel()) {
    val airQualityClassification by viewModel.airQualityClassification.collectAsState()

    // 이미지 리소스와 배경색을 설정하는 함수
    val (imageResId, backgroundColor) = when (airQualityClassification) {
        "좋음" -> R.drawable.good to Color(0xFF31A4DD) // 좋음
        "보통" -> R.drawable.soso to Color(0xFF2A612C) // 보통
        "나쁨" -> R.drawable.bad to Color(0xFFFF9800) // 나쁨
        "매우 나쁨" -> R.drawable.terrible to Color(0xFFF44336) // 매우 나쁨
        else -> R.drawable.base to Color(0xFF5B60A0) // 기본값
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor // 화면 배경색
    ) {
        // 도시와 구역 스피너를 수평으로 배열
        Column( // vertical layout
            modifier = Modifier
                .fillMaxSize() // column 이 전체 화면을 차지하도록
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CityAreaSpinners(viewModel)
            Spacer(modifier = Modifier.weight(1f)) // 빈 공간 추가하여 레이아웃 조정
            Box(// 컴포넌트 겹쳐서 배치하는 레이아웃
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MainInfo(viewModel)
            }
            Spacer(Modifier.weight(1f)) // Imoge 아래쪽에 추가 공간을 제공
        }
    }
}

@Composable
fun CityAreaSpinners(viewModel: MainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()) { // 수평으로 배열
        Spinner(
            items = viewModel.cities, // 스피너에 표시될 항목 리스트
            label = "도시 선택", // 스피너 기본 텍스트
            onItemSelected = viewModel::setSelectedCity, // 항목 선택 시 호출할 함수
            modifier = Modifier.weight(1f) // Row 내에서 공간을 공평하게 나누기
        )

        Spinner(
            items = viewModel.areas.collectAsState().value,
            label = "지역 선택",
            onItemSelected = viewModel::setSelectedArea,
            modifier = Modifier.weight(1f) // Row 내에서 공간을 공평하게 나누기
        )
    }
}

@Composable
fun Spinner(
    items: List<String>,
    label: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) } // 드롭다운 메뉴 확장?
    var selectedOptionText by remember { mutableStateOf(label) } // 선택 항목 저장

    BoxWithConstraints( // 드롭다운 메뉴의 컨테이너
        modifier = modifier
            .fillMaxWidth()  // 필수: weight가 올바르게 동작하려면 필요
            .padding(10.dp)
            .clickable(onClick = { expanded = true })
            .background(Color.Transparent)
            .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val boxWidth = constraints.maxWidth

        Text(
            text = selectedOptionText,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier
                .padding(10.dp) // 선택 항목 표시
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { boxWidth.toDp() }) // 드롭다운 메뉴의 너비를 Box의 너비와 같게 설정
        ) {
            items.forEach { item ->
                DropdownMenuItem(onClick = {
                    selectedOptionText = item
                    expanded = false
                    onItemSelected(item)
                }) {
                    Text(
                        text = item,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun Imoji(@DrawableRes drawableResId: Int, modifier: Modifier = Modifier) {
    Image(
        painter = rememberImagePainter(data = drawableResId),
        contentDescription = null,
        modifier = modifier
    )
}


@Composable
fun MainInfo(viewModel: MainViewModel) {
    val airQualityClassification by viewModel.airQualityClassification.collectAsState()
    val dustData by viewModel.dustData.collectAsState()
    val selectedArea by viewModel.selectedArea.collectAsState()

    // 이미지 리소스와 배경색을 설정하는 함수
    val imageResId = when (airQualityClassification) {
        "좋음" -> R.drawable.good
        "보통" -> R.drawable.soso
        "나쁨" -> R.drawable.bad
        "매우 나쁨" -> R.drawable.terrible
        else -> R.drawable.base
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        val (location, date, data, level, image) = createRefs()

        // location 업데이트
        val locationText = dustData?.let { "${it.sidoName} $selectedArea" } ?: "지역을 선택해주세요."
        // dataTime 업데이트
        val dateText = dustData?.dataTime ?: ""
        // khaiValue 업데이트
        val dataText = dustData?.khaiValue ?: "0"

        Log.d("MainInfo", "LocationText: $locationText, DateText: $dateText, DataText: $dataText")
        //TODO: LOG 2중으로 찍힘

        Text(
            text = locationText,
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(location) {
                top.linkTo(parent.top, margin = 60.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Text(
            text = dateText,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(date) {
                top.linkTo(location.bottom, margin = 30.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Imoji(
            drawableResId = imageResId,
            modifier = Modifier
                .size(200.dp)
                .constrainAs(image) {
                    centerHorizontallyTo(parent)
                    centerVerticallyTo(parent)
                }
        )

        Text(
            text = "$dataText ㎍/㎥",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(data) {
                bottom.linkTo(parent.bottom, margin = 160.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 수치 레벨 결과 표시
        Text(
            text = airQualityClassification,
            fontSize = 28.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(level) {
                top.linkTo(data.bottom, margin = 30.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    DoilmiseTheme {
        MainContent()
    }
}