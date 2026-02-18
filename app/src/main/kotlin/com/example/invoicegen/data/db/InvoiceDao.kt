package com.example.invoicegen.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeInvoiceById(id: Long): Flow<InvoiceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Delete
    suspend fun delete(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET pdfPath = :path WHERE id = :id")
    suspend fun updatePdfPath(id: Long, path: String)

    @Query("UPDATE invoices SET subtotal = :subtotal, taxPercent = :taxPercent, total = :total WHERE id = :id")
    suspend fun updateTotals(id: Long, subtotal: Long, taxPercent: Double, total: Long)
}
