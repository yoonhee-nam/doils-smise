package com.custommise.doilmise.data


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Query("SELECT uri FROM image_table WHERE classification = :classification")
    suspend fun getImageUri(classification: String): String?

    @Query("SELECT * FROM image_table")
    suspend fun getAllImageUris(): List<ImageEntity>
}

