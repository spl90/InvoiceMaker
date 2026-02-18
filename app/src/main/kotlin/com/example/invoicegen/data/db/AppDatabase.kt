package com.example.invoicegen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        InvoiceEntity::class,
        LineItemEntity::class,
        BusinessInfoEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun lineItemDao(): LineItemDao
    abstract fun businessInfoDao(): BusinessInfoDao
}
