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
import com.example.doilmise.data.DustItem
import com.example.doilmise.data.DustResponse
import com.example.doilmise.data.ImageEntity
import com.example.doilmise.retrofit.NetworkClient
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
    private val api_key =
        BuildConfig.api_key

    private val imageDao = AppDatabase.getInstance(application).imageDao()

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    private val _selectedArea = MutableStateFlow<String?>(null)
    val selectedArea: StateFlow<String?> = _selectedArea.asStateFlow()

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

    init {
        loadSavedImageUris()
        loadSomething()
    }

    private fun loadSomething() = viewModelScope.launch {
        _isLoading.value = true
        delay(1000L)
        _isLoading.value = false
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

    // Load dust information for the area
    fun loadDustInfo(area: String) = viewModelScope.launch {

        _isLoading.value = true
        Log.d("loaddustsifo", "loadDustInfo: $area")
        // execute if the currently selected city is not null
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
                                // Get the first matching dust item
                                Log.d("dustItem ", "loadDustInfo, matchingItems: $items")
                                val dustItem = items.firstOrNull()
                                // Set the acquired dust item to StateFlow
                                _dustData.value = dustItem
                                Log.d("dustItem", "loadDustInfo: $dustItem")

                                // Logging after classifying the value
                                val classification = classifyAirQuality(
                                    pm10Value = dustItem?.pm10Value,
                                    pm25Value = dustItem?.pm25Value,
                                    o3Value = dustItem?.o3Value
                                )
                                // Set the classification result to StateFlow
                                _airQualityClassification.value = classification
                                Log.i("AirQuality", classification)
                                Log.i("AirQuality", "${dustItem?.o3Value}")
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

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()


        } else {
            Log.d("requestLocation", "requestLocation: 권한")
            // 권한 요청 로직 추가
            // requestLocationPermission()을 호출하여 권한을 요청합니다.
        }
    }

    fun requestMedia() {
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            loadSavedImageUris()
        } else {

        }
    }


    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    getAddress(it.latitude, it.longitude)
                } ?: run {
                    _locationAddress.value = "위치를 가져올 수 없습니다."
                }
            }
            .addOnFailureListener { e ->
                _locationAddress.value = e.localizedMessage ?: "위치 오류"
            }
    }

    private fun getAddress(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(getApplication(), Locale.KOREA)
            val addressList: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            addressList?.firstOrNull()?.let { address ->
                val adminArea = address.adminArea
                val cityAbbreviation = when (adminArea) {
                    "충청남도" -> "충남"
                    "충청북도" -> "충북"
                    "전라남도" -> "전남"
                    "전라북도" -> "전북"
                    "경상남도" -> "경남"
                    "경상북도" -> "경북"
                    "강원도" -> "강원"
                    "경기도" -> "경기"
                    else -> adminArea // 변환되지 않으면 원래 값 유지
                }
                _locationAddress.value =
                    "${cityAbbreviation} ${address.locality} ${address.thoroughfare}"
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


fun classifyAirQuality(pm10Value: String?, pm25Value: String?, o3Value: String?): String {
    val pm10Int = pm10Value?.toIntOrNull()
    val pm25Int = pm25Value?.toIntOrNull()
    val o3Double = o3Value?.toDoubleOrNull()

    val pm10Grade = when {
        pm10Int == null -> 0
        pm10Int <= 30 -> 1
        pm10Int <= 80 -> 2
        pm10Int <= 150 -> 3
        else -> 4
    }

    val pm25Grade = when {
        pm25Int == null -> 0
        pm25Int <= 15 -> 1
        pm25Int <= 35 -> 2
        pm25Int <= 75 -> 3
        else -> 4
    }

    val o3Grade = when {
        o3Double == null -> 0
        o3Double <= 0.030 -> 1
        o3Double <= 0.090 -> 2
        o3Double <= 0.150 -> 3
        else -> 4
    }
    val averageGrade = (pm10Grade + pm25Grade + o3Grade) / 3.0

    return when {
        averageGrade <= 1 -> "좋음"
        averageGrade <= 2 -> "보통"
        averageGrade <= 3 -> "나쁨"
        else -> "매우 나쁨"
    }
}

suspend fun fetchDustInfo(
    serviceKey: String,
    city: String,
    area: String
): Response<DustResponse> {
    return NetworkClient.dustNetWork.getDust(
        serviceKey = serviceKey,
        returnType = "json",
        numOfRows = "100",
        pageNo = "1",
        sidoName = city,
        sggName = area,
        stationName = area,
        dataTerm = "daily",
        ver = "1.3"
    )
}