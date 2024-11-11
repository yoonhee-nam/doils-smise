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


    private val _stationName = MutableStateFlow<String?>(null)
    val stationName: StateFlow<String?> = _stationName.asStateFlow()

    private val _airQualityGrade = MutableStateFlow(Grade.UNKNOWN)
    val airQualityGrade: StateFlow<Grade> = _airQualityGrade.asStateFlow()

    private val _10pmvalue = MutableStateFlow<Int?>(null)
    val a10pmvalue : StateFlow<Int?> = _10pmvalue.asStateFlow()

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

                            val grade = when (measuredValue?.khaiGrade) {
                                "1" -> Grade.BEST
                                "2" -> Grade.GOOD
                                "3" -> Grade.FAIR
                                "4" -> Grade.NORMAL
                                "5" -> Grade.BAD
                                "6" -> Grade.VERY_BAD
                                "7" -> Grade.EXTREMELY_BAD
                                "8" -> Grade.WORST
                                else -> Grade.UNKNOWN
                            }
                            _airQualityGrade.value = grade
                            // 상태 업데이트
                            _dustData.value = airQualityData

                            Log.d("fetchAirQualityData", "가장 가까운 측정소: ${monitoringStation.stationName}")
                            Log.d("fetchAirQualityData", "받아온 공기질 데이터: $measuredValue")
                        } catch (e: Exception) {
                            // 오류 발생 시 처리
                            _dustData.value = null
                            Log.e("fetchAirQualityData", "Error fetching air quality data", e)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // 위치 정보를 가져오지 못한 경우 처리
                _dustData.value = null
                Log.e("fetchAirQualityData", "Failed to get location", e)
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
