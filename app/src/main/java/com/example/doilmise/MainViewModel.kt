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
import com.example.doilmise.data.Grade
import com.example.doilmise.data.ImageEntity
import com.example.doilmise.data.airqualitypackage.Item
import com.example.doilmise.retrofit.Repository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val imageDao = AppDatabase.getInstance(application).imageDao()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _dustData = MutableStateFlow<Item?>(null)
    val dustData: StateFlow<Item?> = _dustData.asStateFlow()

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

    private val _pm10Grade = MutableStateFlow(Grade.UNKNOWN)
    val pm10Grade: StateFlow<Grade> = _pm10Grade.asStateFlow()

    private val _pm25Grade = MutableStateFlow(Grade.UNKNOWN)
    val pm25Grade: StateFlow<Grade> = _pm25Grade.asStateFlow()

    private val _o3Grade = MutableStateFlow(Grade.UNKNOWN)
    val o3Grade: StateFlow<Grade> = _o3Grade.asStateFlow()

    init {
        loadSavedImageUris()
        requestLocation()
    }

    fun initializeData() {
        _isLoading.value = true // 초기화 시 로딩 상태 true로 설정
        if (_locationPermissionGranted.value) {
            getLocation()
        } else {
            requestLocation()
        }
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
    fun getLocation() {
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
        _isLoading.value = true
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let { loc ->
                    viewModelScope.launch {
                        try {
                            // 위치를 기반으로 가장 가까운 측정소를 가져오기
                            val monitoringStation =
                                Repository.getNearbyMonitoringStation(loc.latitude, loc.longitude)

                            // 측정소에서 공기질 데이터
                            val measuredValue =
                                Repository.getLatestAirQualityData(monitoringStation!!.stationName)

                            // 데이터를 Item 객체로 매핑하여 StateFlow에 업데이트
                            val airQualityData = Item(
                                coFlag = measuredValue?.coFlag ?: "데이터 없음",
                                coGrade = measuredValue?.coGrade ?: "데이터 없음",
                                coValue = measuredValue?.coValue ?: "데이터 없음",
                                dataTime = measuredValue?.dataTime ?: "데이터 없음",
                                khaiGrade = measuredValue?.khaiGrade ?: "데이터 없음",
                                khaiValue = measuredValue?.khaiValue ?: "데이터 없음",
                                no2Flag = measuredValue?.no2Flag ?: "데이터 없음",
                                no2Grade = measuredValue?.no2Grade ?: "데이터 없음",
                                no2Value = measuredValue?.no2Value ?: "데이터 없음",
                                o3Flag = measuredValue?.o3Flag ?: "데이터 없음",
                                o3Grade = measuredValue?.o3Grade ?: "데이터 없음",
                                o3Value = measuredValue?.o3Value ?: "데이터 없음",
                                pm10Flag = measuredValue?.pm10Flag ?: "데이터 없음",
                                pm10Grade = measuredValue?.pm10Grade ?: "데이터 없음",
                                pm10Value = measuredValue?.pm10Value ?: "데이터 없음",
                                pm25Flag = measuredValue?.pm25Flag ?: "데이터 없음",
                                pm25Grade = measuredValue?.pm25Grade ?: "데이터 없음",
                                pm25Value = measuredValue?.pm25Value ?: "데이터 없음",
                                so2Flag = measuredValue?.so2Flag ?: "데이터 없음",
                                so2Grade = measuredValue?.so2Grade ?: "데이터 없음",
                                so2Value = measuredValue?.so2Value ?: "데이터 없음"
                            )

                            val pm10Int = measuredValue?.pm10Value?.toIntOrNull()
                            val pm25Int = measuredValue?.pm25Value?.toIntOrNull()
                            val o3Double = measuredValue?.o3Value?.toDoubleOrNull()

                            _pm10Grade.value = when {
                                pm10Int == null -> Grade.UNKNOWN
                                pm10Int <= 15 -> Grade.BEST
                                pm10Int <= 30 -> Grade.GOOD
                                pm10Int <= 40 -> Grade.FAIR
                                pm10Int <= 50 -> Grade.NORMAL
                                pm10Int <= 76 -> Grade.BAD
                                pm10Int <= 100 -> Grade.VERY_BAD
                                pm10Int <= 150 -> Grade.EXTREMELY_BAD
                                else -> Grade.WORST
                            }

                            _pm25Grade.value = when {
                                pm25Int == null -> Grade.UNKNOWN
                                pm25Int <= 8 -> Grade.BEST
                                pm25Int <= 15 -> Grade.GOOD
                                pm25Int <= 20 -> Grade.FAIR
                                pm25Int <= 25 -> Grade.NORMAL
                                pm25Int <= 37 -> Grade.BAD
                                pm25Int <= 50 -> Grade.VERY_BAD
                                pm25Int <= 75 -> Grade.EXTREMELY_BAD
                                else -> Grade.WORST
                            }

                            _o3Grade.value = when {
                                o3Double == null -> Grade.UNKNOWN
                                o3Double <= 0.020 -> Grade.BEST
                                o3Double <= 0.030 -> Grade.GOOD
                                o3Double <= 0.060 -> Grade.FAIR
                                o3Double <= 0.090 -> Grade.NORMAL
                                o3Double <= 0.120 -> Grade.BAD
                                o3Double <= 0.150 -> Grade.VERY_BAD
                                o3Double <= 0.380 -> Grade.EXTREMELY_BAD
                                else -> Grade.WORST
                            }
                            Log.d("MainViewModel GradeCalculation", "PM10 Grade: ${_pm10Grade.value}")
                            Log.d("MainViewModel GradeCalculation", "PM25 Grade: ${_pm25Grade.value}")
                            Log.d("MainViewModel GradeCalculation", "O3 Grade: ${_o3Grade.value}")
//                            // 상태 업데이트
                            _dustData.value = airQualityData
                            _isLoading.value = false
                            Log.d("MainViewModel fetchAirQualityData", "가장 가까운 측정소: ${monitoringStation.stationName}")
                            Log.d("MainViewModel fetchAirQualityData", "받아온 공기질 데이터: $measuredValue")
                        } catch (e: Exception) {
                            // 오류 발생 시 처리
                            _dustData.value = null
                            _isLoading.value = false
                            Log.e("MainViewModel fetchAirQualityData", "Error fetching air quality data", e)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // 위치 정보를 가져오지 못한 경우 처리
                _dustData.value = null
                _isLoading.value = false
                Log.e("MainViewModel fetchAirQualityData", "Failed to get location", e)
            }
    }


    fun updateImageUri(classification: String, uri: Uri) = viewModelScope.launch {
        val currentMap = _imageUris.value.toMutableMap()
        currentMap[classification] = uri
        _imageUris.value = currentMap
        Log.d("MainViewModel", "updateImageUri image URIs from Room: $currentMap")

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
                    "${address.locality} ${address.thoroughfare}"
                Log.d("getAddress", "getAddress: ${_locationAddress.value}")
                _locationAddress.value = _locationAddress.value.toString()
            } ?: run {
                _locationAddress.value = "주소를 가져 올 수 없습니다."
            }
        } catch (e: IOException) {
            _locationAddress.value = "주소를 가져 올 수 없습니다."
        }
    }
}
