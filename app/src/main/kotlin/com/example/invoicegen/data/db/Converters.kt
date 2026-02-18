package com.example.invoicegen.data.db

import androidx.room.TypeConverter
import com.example.invoicegen.data.model.DocumentType

class Converters {
    @TypeConverter
    fun fromDocumentType(type: DocumentType): String = type.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType =
        DocumentType.valueOf(value)
}
