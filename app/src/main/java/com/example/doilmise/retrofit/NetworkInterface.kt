package com.example.doilmise.retrofit

import com.example.doilmise.data.DustResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NetworkInterface {

    @GET("getCtprvnRltmMesureDnsty")
    suspend fun getDust(
        @Query("serviceKey") serviceKey: String,
        @Query("returnType") returnType: String,
        @Query("numOfRows") numOfRows: String,
        @Query("pageNo") pageNo: String,
        @Query("sidoName") sidoName: String,
        @Query("stationName") stationName: String,
        @Query("dataTerm") dataTerm: String,
        @Query("ver") ver: String
    ): Response<DustResponse>

}