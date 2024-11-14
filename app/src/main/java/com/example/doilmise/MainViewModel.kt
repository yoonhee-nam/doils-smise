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
    }

    fun handlePermissionGranted() {
        viewModelScope.launch {
            Log.d("MainViewModel", "Permission granted, starting data fetch")
            _locationPermissionGranted.value = true
            _isLoading.value = true

            try {
                fetchAirQualityData()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in handlePermissionGranted", e)
                _isLoading.value = false
            }
        }
    }


    fun onPermissionGranted() {
        viewModelScope.launch {
            Log.d("MainViewModel", "Permission granted, starting data fetch process")
            _locationPermissionGranted.value = true
            _isLoading.value = true
            try {
                Log.d("MainViewModel", "Requesting location update")
                getLocation()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in permission granted flow", e)
                _isLoading.value = false
                _locationAddress.value = "데이터를 가져올 수 없습니다."
            }
        }
    }


    fun updateLocationPermissionState(granted: Boolean) {
        viewModelScope.launch {
            Log.d("MainViewModel", "Updating permission state to: $granted")
            _locationPermissionGranted.value = granted
            if (granted) {
                _isLoading.value = true
            }
        }
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
        if (!_locationPermissionGranted.value) {
            Log.d("MainViewModel", "getLocation: No permission")
            return
        }
        _isLoading.value = true
        Log.d("MainViewModel", "getLocation: Starting location request")

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                Log.d("MainViewModel", "Location result received: ${location != null}")
                location?.let { loc ->
                    viewModelScope.launch {
                        try {
                            getAddress(loc.latitude, loc.longitude)
                            fetchAirQualityData()
                            Log.d("MainViewModel", "Location and air quality data fetch initiated")
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error processing location", e)
                            _isLoading.value = false
                            _locationAddress.value = "데이터를 가져올 수 없습니다."
                        }
                    }
                } ?: run {
                    Log.d("MainViewModel", "Location is null")
                    _isLoading.value = false
                    _locationAddress.value = "위치를 가져올 수 없습니다."
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "Failed to get location", e)
                _isLoading.value = false
                _locationAddress.value = "위치 오류: ${e.localizedMessage}"
            }
    }


    @SuppressLint("MissingPermission")
    fun fetchAirQualityData() {
        if (!_locationPermissionGranted.value) {
            Log.d("MainViewModel", "fetchAirQualityData: No permission")
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        Log.d("MainViewModel", "fetchAirQualityData: Starting to fetch data")

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let { loc ->
                    viewModelScope.launch {
                        try {
                            Log.d("MainViewModel", "fetchAirQualityData: Getting station for location ${loc.latitude}, ${loc.longitude}")
                            val monitoringStation = Repository.getNearbyMonitoringStation(loc.latitude, loc.longitude)

                            Log.d("MainViewModel", "fetchAirQualityData: Found station ${monitoringStation?.stationName}")
                            // Item을 직접 받아오도록 수정
                            val airQualityData = Repository.getLatestAirQualityData(monitoringStation?.stationName ?: return@launch)
                            updateAirQualityData(airQualityData)
                            Log.d("MainViewModel", "fetchAirQualityData: Data update complete")

                        } catch (e: Exception) {
                            Log.e("MainViewModel", "fetchAirQualityData: Error", e)
                            _dustData.value = null
                            _pm10Grade.value = Grade.UNKNOWN
                            _pm25Grade.value = Grade.UNKNOWN
                            _o3Grade.value = Grade.UNKNOWN
                            _locationAddress.value = "데이터를 가져올 수 없습니다."
                        } finally {
                            _isLoading.value = false
                        }
                    }
                } ?: run {
                    Log.d("MainViewModel", "fetchAirQualityData: Location is null")
                    _isLoading.value = false
                    _dustData.value = null
                    _pm10Grade.value = Grade.UNKNOWN
                    _pm25Grade.value = Grade.UNKNOWN
                    _o3Grade.value = Grade.UNKNOWN
                    _locationAddress.value = "위치를 가져올 수 없습니다."
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "fetchAirQualityData: Failed to get location", e)
                _isLoading.value = false
                _dustData.value = null
                _pm10Grade.value = Grade.UNKNOWN
                _pm25Grade.value = Grade.UNKNOWN
                _o3Grade.value = Grade.UNKNOWN
                _locationAddress.value = "위치 정보 오류: ${e.localizedMessage}"
            }
    }

    private fun updateAirQualityData(airQualityData: Item?) {
        airQualityData?.let {
            _dustData.value = it

            // Grade 업데이트
            val pm10Int = it.pm10Value.toIntOrNull()
            val pm25Int = it.pm25Value.toIntOrNull()
            val o3Double = it.o3Value.toDoubleOrNull()

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

            Log.d("MainViewModel", "Updated PM10 Grade: ${_pm10Grade.value}")
            Log.d("MainViewModel", "Updated PM25 Grade: ${_pm25Grade.value}")
            Log.d("MainViewModel", "Updated O3 Grade: ${_o3Grade.value}")
            Log.d("MainViewModel", "Air quality data updated successfully")
        } ?: run {
            Log.d("MainViewModel", "Air quality data is null, cannot update")
            _dustData.value = null
            _pm10Grade.value = Grade.UNKNOWN
            _pm25Grade.value = Grade.UNKNOWN
            _o3Grade.value = Grade.UNKNOWN
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
