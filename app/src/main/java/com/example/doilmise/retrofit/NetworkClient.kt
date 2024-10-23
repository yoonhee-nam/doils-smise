package com.example.doilmise.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val dustNetWork: NetworkInterface = retrofit.create(NetworkInterface::class.java)

}