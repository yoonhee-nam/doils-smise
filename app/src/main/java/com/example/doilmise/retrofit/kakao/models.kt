package com.example.doilmise.retrofit.kakao

import com.google.gson.annotations.SerializedName

// Document.kt
data class Document(
    @SerializedName("x")
    val x: Double?,
    @SerializedName("y")
    val y: Double?
)

// Meta.kt
data class Meta(
    @SerializedName("total_count")
    val totalCount: Int?
)

// TmCoordinatesResponse.kt
data class TmCoordinatesResponse(
    @SerializedName("documents")
    val documents: List<Document>?,
    @SerializedName("meta")
    val meta: Meta?
)