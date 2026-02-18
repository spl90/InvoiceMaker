package com.example.invoicegen.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LineItemDao {
    @Query("SELECT * FROM line_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun getLineItemsForInvoice(invoiceId: Long): Flow<List<LineItemEntity>>

    @Query("SELECT * FROM line_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getLineItemsOnce(invoiceId: Long): List<LineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LineItemEntity): Long

    @Update
    suspend fun update(item: LineItemEntity)

    @Delete
    suspend fun delete(item: LineItemEntity)

    @Query("DELETE FROM line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteAllForInvoice(invoiceId: Long)
}
