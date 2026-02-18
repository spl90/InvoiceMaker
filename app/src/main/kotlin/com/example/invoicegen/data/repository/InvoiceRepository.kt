package com.example.invoicegen.data.repository

import com.example.invoicegen.data.db.InvoiceDao
import com.example.invoicegen.data.db.LineItemDao
import com.example.invoicegen.data.model.Invoice
import com.example.invoicegen.data.model.LineItem
import com.example.invoicegen.data.model.toDomain
import com.example.invoicegen.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val lineItemDao: LineItemDao
) {
    fun getAllInvoices(): Flow<List<Invoice>> =
        invoiceDao.getAllInvoices().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getInvoiceWithItems(invoiceId: Long): Invoice? {
        val entity = invoiceDao.getInvoiceById(invoiceId) ?: return null
        val items = lineItemDao.getLineItemsOnce(invoiceId).map { it.toDomain() }
        return entity.toDomain(items)
    }

    suspend fun saveNewInvoice(invoice: Invoice): Long {
        val id = invoiceDao.insert(invoice.toEntity())
        invoice.lineItems.forEach { item ->
            lineItemDao.insert(item.toEntity(id))
        }
        return id
    }

    suspend fun updateInvoice(invoice: Invoice) {
        invoiceDao.update(invoice.toEntity())
        lineItemDao.deleteAllForInvoice(invoice.id)
        invoice.lineItems.forEach { item ->
            lineItemDao.insert(item.toEntity(invoice.id))
        }
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.delete(invoice.toEntity())
    }

    suspend fun updatePdfPath(invoiceId: Long, path: String) {
        invoiceDao.updatePdfPath(invoiceId, path)
    }

    // Line item operations for live editing
    fun getLineItemsForInvoice(invoiceId: Long): Flow<List<LineItem>> =
        lineItemDao.getLineItemsForInvoice(invoiceId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun upsertLineItem(item: LineItem): Long =
        lineItemDao.insert(item.toEntity())

    suspend fun deleteLineItem(item: LineItem) {
        lineItemDao.delete(item.toEntity())
    }
}
