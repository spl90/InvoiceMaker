package com.example.invoicegen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.invoicegen.data.model.DocumentType

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessInfoId: Long = 1,
    val clientName: String = "",
    val clientAddress: String = "",
    val clientPhone: String = "",
    val proposalDate: String = "",
    val jobAddress: String = "",
    val datePlans: String = "",
    val architect: String = "",
    val workDescription: String = "",
    val documentType: DocumentType = DocumentType.PROPOSAL,
    val subtotal: Long = 0L,      // stored as cents
    val taxPercent: Double = 0.0,
    val total: Long = 0L,          // stored as cents
    val notes: String = "",
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
