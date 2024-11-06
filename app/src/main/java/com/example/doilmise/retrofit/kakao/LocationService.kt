package com.example.doilmise.retrofit.kakao

import com.example.doilmise.BuildConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface KakaoLocalApiService  {

    @Headers("Authorization: KakaoAK ${BuildConfig.kakao_key}")
    @GET("v2/local/geo/transcoord.json?output_coord=TM")
    suspend fun getTmCoordinates(
        @Query("x") longitude: Double,
        @Query("y") latitude: Double
    ): Response<TmCoordinatesResponse>
}