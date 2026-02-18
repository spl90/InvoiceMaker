package com.example.invoicegen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_info")
data class BusinessInfoEntity(
    @PrimaryKey val id: Long = 1, // singleton row
    val businessName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val logoPath: String = "" // absolute path in app private storage
)
