package com.example.invoicegen.data.model

import com.example.invoicegen.data.db.BusinessInfoEntity
import com.example.invoicegen.data.db.InvoiceEntity
import com.example.invoicegen.data.db.LineItemEntity

// ---- Entity -> Domain ----

fun InvoiceEntity.toDomain(lineItems: List<LineItem> = emptyList()) = Invoice(
    id = id,
    businessInfoId = businessInfoId,
    clientName = clientName,
    clientAddress = clientAddress,
    clientPhone = clientPhone,
    proposalDate = proposalDate,
    jobAddress = jobAddress,
    datePlans = datePlans,
    architect = architect,
    workDescription = workDescription,
    documentType = documentType,
    lineItems = lineItems,
    subtotal = subtotal,
    taxPercent = taxPercent,
    total = total,
    notes = notes,
    pdfPath = pdfPath,
    createdAt = createdAt
)

fun LineItemEntity.toDomain() = LineItem(
    id = id,
    invoiceId = invoiceId,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal
)

fun BusinessInfoEntity.toDomain() = BusinessInfo(
    businessName = businessName,
    address = address,
    phone = phone,
    email = email,
    logoPath = logoPath
)

// ---- Domain -> Entity ----

fun Invoice.toEntity() = InvoiceEntity(
    id = id,
    businessInfoId = businessInfoId,
    clientName = clientName,
    clientAddress = clientAddress,
    clientPhone = clientPhone,
    proposalDate = proposalDate,
    jobAddress = jobAddress,
    datePlans = datePlans,
    architect = architect,
    workDescription = workDescription,
    documentType = documentType,
    subtotal = subtotal,
    taxPercent = taxPercent,
    total = total,
    notes = notes,
    pdfPath = pdfPath,
    createdAt = createdAt
)

fun LineItem.toEntity(invoiceId: Long = this.invoiceId) = LineItemEntity(
    id = id,
    invoiceId = invoiceId,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal
)

fun BusinessInfo.toEntity() = BusinessInfoEntity(
    id = 1,
    businessName = businessName,
    address = address,
    phone = phone,
    email = email,
    logoPath = logoPath
)
