package com.example.doilmise.retrofit.airo

import com.google.gson.annotations.SerializedName

// Body.kt
data class Body(
    @SerializedName("items")
    val monitoringStations: List<MonitoringStation>?,
    @SerializedName("numOfRows")
    val numOfRows: Int?,
    @SerializedName("pageNo")
    val pageNo: Int?,
    @SerializedName("totalCount")
    val totalCount: Int?
)

// Header.kt
data class Header(
    @SerializedName("resultCode")
    val resultCode: String?,
    @SerializedName("resultMsg")
    val resultMsg: String?
)

// MonitoringStation.kt
data class MonitoringStation(
    @SerializedName("addr")
    val addr: String?,
    @SerializedName("stationName")
    val stationName: String?,
    @SerializedName("tm")
    val tm: Double?
)

// MonitoringStationsResponse.kt
data class MonitoringStationsResponse(
    @SerializedName("response")
    val response: Response?
)

// Response.kt
data class Response(
    @SerializedName("body")
    val body: Body?,
    @SerializedName("header")
    val header: Header?
)
