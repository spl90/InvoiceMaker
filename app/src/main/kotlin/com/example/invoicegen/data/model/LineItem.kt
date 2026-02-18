package com.example.invoicegen.data.model

data class LineItem(
    val id: Long = 0,
    val invoiceId: Long = 0,
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Long = 0L,  // cents
    val lineTotal: Long = 0L   // cents
)
