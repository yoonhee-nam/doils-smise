package com.example.doilmise

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.doilmise.data.AppDatabase
import com.example.doilmise.retrofit.airo.DustItem
import com.example.doilmise.retrofit.airo.DustResponse
import com.example.doilmise.data.ImageEntity
import com.example.doilmise.retrofit.Repository
import com.example.doilmise.retrofit.airo.AirKoreaApiService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val api_key = BuildConfig.api_key
    private val imageDao = AppDatabase.getInstance(application).imageDao()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _dustData = MutableStateFlow<DustItem?>(null)
    val dustData: StateFlow<DustItem?> = _dustData.asStateFlow()

    private val _airQualityClassification = MutableStateFlow("")
    val airQualityClassification: StateFlow<String> = _airQualityClassification.asStateFlow()

    private val _locationDistance = MutableLiveData<String>()
    val locationDistance: LiveData<String> get() = _locationDistance

    private val _locationAddress = MutableLiveData<String>()
    val locationAddress: LiveData<String> get() = _locationAddress

    private val _imageUris = MutableStateFlow<Map<String, Uri?>>(emptyMap())
    val imageUris: StateFlow<Map<String, Uri?>> = _imageUris

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted


    init {
        loadSavedImageUris()
        loadSomething()
    }

    private fun loadSomething() = viewModelScope.launch {
        _isLoading.value = true
        delay(1000L)
        _isLoading.value = false
    }
    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            _locationPermissionGranted.value = true // 권한이 있을 경우 상태 업데이트
            getLocation()

        } else {
            _locationPermissionGranted.value = false // 권한이 없을 경우 상태 업데이트
            // 권한이 없으면 권한 요청
            requestPermission()
        }
    }

    // 권한 요청
    private fun requestPermission() {
        // 권한 요청 로직을 Compose에서 처리하도록 위임
    }

    // 위치 가져오기
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {

                    getAddress(it.latitude, it.longitude)
                    fetchAirQualityData()

                } ?: run {
                    _locationAddress.value = "위치를 가져올 수 없습니다."
                }
            }
            .addOnFailureListener { e ->
                _locationAddress.value = e.localizedMessage ?: "위치 오류"
            }
    }

    @SuppressLint("MissingPermission")
    fun fetchAirQualityData() {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let { loc ->
                    viewModelScope.launch {
                        val monitoringStation = Repository.getNearbyMonitoringStation(loc.latitude, loc.longitude)
                        val measuredValue = Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

                        // 데이터 업데이트
                        _stationName.value = monitoringStation.stationName
                        _stationAddress.value = monitoringStation.addr
                        _airQualityGrade.value = measuredValue?.khaiGrade ?: Grade.UNKNOWN
                        _dustData.value = "미세먼지: ${measuredValue?.pm10Value ?: "N/A"} ㎍/㎥ ${measuredValue?.pm10Grade?.emoji ?: ""}"
                    }
                }
            }
            .addOnFailureListener { e ->
                // 위치 가져오기 실패 시 처리
                _stationName.value = "위치 오류"
                _stationAddress.value = e.localizedMessage ?: "위치를 가져올 수 없습니다."
            }
    }



    fun updateImageUri(classification: String, uri: Uri) = viewModelScope.launch {
        val currentMap = _imageUris.value.toMutableMap()
        currentMap[classification] = uri
        _imageUris.value = currentMap

        // 영구 URI 권한 요청, 예외 처리 추가
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Error taking persistable URI permission for $uri", e)
        }

        imageDao.insertImage(ImageEntity(classification, uri.toString()))
    }


    private fun loadSavedImageUris() = viewModelScope.launch {
        val imageList = imageDao.getAllImageUris()
        val imageMap = imageList.associate { it.classification to Uri.parse(it.uri) }
        _imageUris.value = imageMap
        Log.d("MainViewModel", "Loaded image URIs from Room: $imageMap")
    }

    fun loadDustInfo(area: String) = viewModelScope.launch {
        _isLoading.value = true
        Log.d("loaddustsifo", "loadDustInfo: $area")

        selectedCity.value?.let { city ->
            try {
                Log.d("loadDustSuc", "city: $city")
                // call the API
                val response = fetchDustInfo(api_key, city, area)

                // Successful API response
                if (response.isSuccessful) {
                    // Response body
                    response.body()?.let { dustResponse ->
                        Log.d("loadDustSuc", "loadDustInfo: $dustResponse")
                        dustResponse.response.body.let { body ->
                            // Load the dust items from the response
                            val items = body.dustItem

                            // execute if the dust item is not empty
                            if (!items.isNullOrEmpty()) {
                                // Check if the first 2 characters of area and stationName match
                                val dustItem = items.firstOrNull { item ->
                                    item.stationName.take(2) == area.take(2)
                                }

                                if (dustItem != null) {
                                    // Set the acquired dust item to StateFlow
                                    _dustData.value = dustItem
                                    Log.d("dustItem", "loadDustInfo: $dustItem")

                                    // Logging after classifying the value
                                    val classification = classifyAirQuality(
                                        pm10Value = dustItem.pm10Value,
                                        pm25Value = dustItem.pm25Value,
                                        o3Value = dustItem.o3Value
                                    )
                                    // Set the classification result to StateFlow
                                    _airQualityClassification.value = classification.toString()
                                    Log.i("AirQuality", classification.toString())
                                    Log.i("AirQuality", "${dustItem.pm10Value}")
                                } else {
                                    Log.e("MainViewModel", "No matching dust item found for $area")
                                }
                            } else {
                                // Failed to get dust information
                                Log.e("MainViewModel", "Error: ${response.errorBody()?.string()}")
                            }
                        }
                    }
                } else {
                    Log.e("TAG", "loadDustInfo: ${error(message = "?")}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching dust info for $area, $city", e)
            } finally {
                // Set the loading state to false to notify the UI that loading is complete
                _isLoading.value = false
            }
        }
    }



}
//
//    private suspend fun getNearbyCenter(latitude: Double, longitude: Double): Station? {
//        val tmCoordinates = kakaoLocalAPI.getTmCoordinates(longitude, latitude)
//            .body()?.documents
//            ?.firstOrNull() // 첫번째 값 없으면 null
//        val tmX = tmCoordinates?.x
//        val tmY = tmCoordinates?.y
//
//        Log.d("NearbyCenter", "tmX: $tmX, tmY: $tmY")
//
//        return if (tmX != null && tmY != null) {
//            airKoreaAPIService
//                .getNearbyCenter(tmX, tmY)
//                .body()
//                ?.response
//                ?.body
//                ?.stations
//                ?.minByOrNull { it.tm ?: Double.MAX_VALUE }
//        } else {
//            null
//        }
//    }

    private fun getAddress(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(getApplication(), Locale.KOREA)
            val addressList: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            addressList?.firstOrNull()?.let { address ->
                val cityAbbreviation = when (val adminArea = address.adminArea) {
                    "충청남도" -> "충남"
                    "충청북도" -> "충북"
                    "전라남도" -> "전남"
                    "전라북도" -> "전북"
                    "경상남도" -> "경남"
                    "경상북도" -> "경북"
                    "강원도" -> "강원"
                    "경기도" -> "경기"
                    else -> adminArea
                }
                _locationAddress.value =
                    "$cityAbbreviation ${address.locality} ${address.thoroughfare}"
                _selectedCity.value = cityAbbreviation
                Log.d("getAddress", "getAddress: ${_locationAddress.value}")
                _locationAddress.value = _locationAddress.value.toString()
                loadDustInfo(address.thoroughfare)
            } ?: run {
                _locationAddress.value = "주소를 가져 올 수 없습니다."
            }
        } catch (e: IOException) {
            _locationAddress.value = "주소를 가져 올 수 없습니다."
        }
    }
}


fun classifyAirQuality(
    pm10Value: String?,
    pm25Value: String?,
    o3Value: String?
): Map<String, String> {
    val pm10Int = pm10Value?.toIntOrNull()
    val pm25Int = pm25Value?.toIntOrNull()
    val o3Double = o3Value?.toDoubleOrNull()

    val pm10Grade = when {
        pm10Int == null -> 0
        pm10Int <= 15 -> 1
        pm10Int <= 30 -> 2
        pm10Int <= 40 -> 3
        pm10Int <= 50 -> 4
        pm10Int <= 76 -> 5
        pm10Int <= 100 -> 6
        pm10Int <= 150 -> 7
        else -> 8
    }

    val pm25Grade = when {
        pm25Int == null -> 0
        pm25Int <= 8 -> 1
        pm25Int <= 15 -> 2
        pm25Int <= 20 -> 3
        pm25Int <= 25 -> 4
        pm25Int <= 37 -> 5
        pm25Int <= 50 -> 6
        pm25Int <= 75 -> 7
        else -> 8
    }

    val o3Grade = when {
        o3Double == null -> 0
        o3Double <= 0.020 -> 1
        o3Double <= 0.030 -> 2
        o3Double <= 0.060 -> 3
        o3Double <= 0.090 -> 4
        o3Double <= 0.120 -> 5
        o3Double <= 0.150 -> 6
        o3Double <= 0.380 -> 7
        else -> 8
    }
    val gradeDescriptions = mapOf(
        1 to "최고 좋음",
        2 to "좋음",
        3 to "양호",
        4 to "보통",
        5 to "나쁨",
        6 to "상당히 나쁨",
        7 to "매우 매우 나쁨",
        8 to "최악"
    )

    return mapOf(
        Pair("PM10", gradeDescriptions[pm10Grade] ?: "알 수 없음"),
        Pair("PM2.5", gradeDescriptions[pm25Grade] ?: "알 수 없음"),
        Pair("O3", gradeDescriptions[o3Grade] ?: "알 수 없음")
    )
}
//    val averageGrade = (pm10Grade + pm25Grade + o3Grade) / 3.0
//
//    return when {
//        averageGrade <= 1 -> "좋음"
//        averageGrade <= 2 -> "보통"
//        averageGrade <= 3 -> "나쁨"
//        else -> "매우 나쁨"
//    }


suspend fun fetchDustInfo(
    stationName:String
): Response<DustResponse> {
    return AirKoreaApiService.getRealtimeAirQualities(
        stationName = stationName,
    )
}