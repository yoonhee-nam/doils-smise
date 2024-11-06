package com.example.doilmise.retrofit


import com.example.doilmise.BuildConfig
import com.example.doilmise.retrofit.airo.AirKoreaApiService
import com.example.doilmise.retrofit.airo.MonitoringStation
import com.example.doilmise.retrofit.kakao.KakaoLocalApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object Repository {

    private val kakaoLocalApiService: KakaoLocalApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildHttpClient())
            .build()
            .create()
    }

    private fun buildHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        // DEBUG일때만 다 보여주기
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        // NONE : 보여주지 않음
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()

    suspend fun getNearbyMonitoringStation(latitude: Double, longitude: Double): MonitoringStation? {
        val tmCoordinates = kakaoLocalApiService
            .getTmCoordinates(longitude, latitude)
            .body()
            ?.documents
            ?.firstOrNull()

        val tmX = tmCoordinates?.x
        val tmY = tmCoordinates?.y

        return airKoreaApiService
            .getNearbyMonitoringStation(tmX!!, tmY!!)
            .body()
            ?.response
            ?.body
            ?.monitoringStations
            // 선택한 요소를 비교해 가장 작은 값을 전달하고 null인 값은 자동으로 후순위로 밀림
            // => 가장 가까운 측정소 하나만 받아오게 됨
            ?.minByOrNull { it.tm ?: Double.MAX_VALUE }
    }

    private val airKoreaApiService: AirKoreaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildHttpClient())
            .build()
            .create()
    }

    suspend fun getLatestAirQualityData(stationName: String): MeasuredValue? =
        airKoreaApiService
            .getRealtimeAirQualities(stationName)
            .body()
            ?.response
            ?.body
            ?.measuredValues
            ?.firstOrNull()
}