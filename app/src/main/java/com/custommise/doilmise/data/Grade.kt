package com.custommise.doilmise.data

import com.google.gson.annotations.SerializedName

enum class Grade(
    val label: String,
) {
    @SerializedName("1")
    BEST("최고 좋음"),

    @SerializedName("2")
    GOOD("좋음"),

    @SerializedName("3")
    FAIR("양호"),

    @SerializedName("4")
    NORMAL("보통",),

    @SerializedName("5")
    BAD("나쁨"),

    @SerializedName("6")
    VERY_BAD("상당히 나쁨"),

    @SerializedName("7")
    EXTREMELY_BAD("매우 매우 나쁨"),

    @SerializedName("8")
    WORST("최악"),

    UNKNOWN("미측정",);

    override fun toString(): String {
        return label
    }
}