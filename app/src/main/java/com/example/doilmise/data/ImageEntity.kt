package com.example.doilmise.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_table")
data class ImageEntity(
    @PrimaryKey val classification: String, // 공기질 상태
    val uri: String
)