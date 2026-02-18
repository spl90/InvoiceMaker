package com.example.invoicegen.data.model

data class Invoice(
    val id: Long = 0,
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
    val lineItems: List<LineItem> = emptyList(),
    val subtotal: Long = 0L,
    val taxPercent: Double = 0.0,
    val total: Long = 0L,
    val notes: String = "",
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
