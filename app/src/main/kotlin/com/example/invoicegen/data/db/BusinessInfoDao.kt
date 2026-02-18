package com.example.invoicegen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessInfoDao {
    @Query("SELECT * FROM business_info WHERE id = 1")
    fun getBusinessInfo(): Flow<BusinessInfoEntity?>

    @Query("SELECT * FROM business_info WHERE id = 1")
    suspend fun getBusinessInfoOnce(): BusinessInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(info: BusinessInfoEntity)
}
