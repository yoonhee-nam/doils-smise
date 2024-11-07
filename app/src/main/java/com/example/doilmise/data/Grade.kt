package com.example.doilmise.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorRes
import com.example.doilmise.R
import com.google.gson.annotations.SerializedName

enum class Grade(
    private val label: String,
    private val emoji: String,
    @ColorRes val colorResId: Int
) {
    @SerializedName("1")
    BEST("최고 좋음", "😇", R.color.black),

    @SerializedName("2")
    GOOD("좋음", "😊", R.color.black),

    @SerializedName("3")
    FAIR("양호", "🙂", R.color.black),

    @SerializedName("4")
    NORMAL("보통", "😐", R.color.black),

    @SerializedName("5")
    BAD("나쁨", "😰", R.color.black),

    @SerializedName("6")
    VERY_BAD("상당히 나쁨", "😷", R.color.black),

    @SerializedName("7")
    EXTREMELY_BAD("매우 매우 나쁨", "🤢", R.color.black),

    @SerializedName("8")
    WORST("최악", "😵", R.color.black),

    UNKNOWN("미측정", "🙄", R.color.black);

    override fun toString(): String {
        return "$label $emoji"
    }
}