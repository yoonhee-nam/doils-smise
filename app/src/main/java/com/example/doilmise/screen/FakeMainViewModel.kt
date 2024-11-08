package com.example.doilmise.screen


import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doilmise.data.Grade
import com.example.doilmise.data.airqualitypackage.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMainViewModel : ViewModel() {

    // 기본 상태를 제공하기 위한 StateFlows와 LiveDatas 정의
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _dustData = MutableStateFlow(
        Item(
            dataTime = "2024-11-08 12:00",
            khaiValue = "85",
            pm10Value = "45",
            pm25Value = "20",
            coFlag = "데이터 없음",
            coGrade = "데이터 없음",
            coValue = "0.4",
            khaiGrade = "2",
            no2Flag = "데이터 없음",
            no2Grade = "데이터 없음",
            no2Value = "0.05",
            o3Flag = "데이터 없음",
            o3Grade = "데이터 없음",
            o3Value = "0.03",
            pm10Flag = "데이터 없음",
            pm10Grade = "2",
            pm25Flag = "데이터 없음",
            pm25Grade = "2",
            so2Flag = "데이터 없음",
            so2Grade = "데이터 없음",
            so2Value = "0.005"
        )
    )
    val dustData: StateFlow<Item?> = _dustData.asStateFlow()

    private val _locationAddress = MutableLiveData("서울특별시 강남구")
    val locationAddress: LiveData<String> get() = _locationAddress

    private val _imageUris = MutableStateFlow(
        mapOf(
            Grade.BEST.name to Uri.parse("android.resource://com.example.app/drawable/good"),
            Grade.GOOD.name to Uri.parse("android.resource://com.example.app/drawable/good")
            // 다른 등급에 대한 가상 URI 추가 가능
        )
    )
    val imageUris: StateFlow<Map<String, Uri?>> = _imageUris.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(true)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    private val _airQualityGrade = MutableStateFlow(Grade.GOOD)
    val airQualityGrade: StateFlow<Grade> = _airQualityGrade.asStateFlow()

    // 메서드를 호출할 때 로그를 출력하여 어떤 메서드가 호출되었는지 확인할 수 있도록 합니다.
    fun fetchAirQualityData() {
        _isLoading.value = true
        println("FakeMainViewModel: fetchAirQualityData called.")
        // 데이터를 로드했다고 가정하고 로딩 상태 해제
        _isLoading.value = false
    }

    fun updateImageUri(classification: String, uri: Uri) {
        val currentMap = _imageUris.value.toMutableMap()
        currentMap[classification] = uri
        _imageUris.value = currentMap
        println("FakeMainViewModel: updateImageUri called for $classification")
    }

    fun requestLocation() {
        println("FakeMainViewModel: requestLocation called.")
        // 위치 권한이 허용되었다고 가정
        _locationPermissionGranted.value = true
        _locationAddress.value = "서울특별시 강남구 삼성동"
    }
}
