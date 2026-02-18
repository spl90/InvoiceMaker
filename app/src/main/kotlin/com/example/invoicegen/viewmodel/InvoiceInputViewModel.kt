package com.example.invoicegen.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.invoicegen.data.model.DocumentType
import com.example.invoicegen.data.model.Invoice
import com.example.invoicegen.data.model.LineItem
import com.example.invoicegen.data.repository.BusinessInfoRepository
import com.example.invoicegen.data.repository.InvoiceRepository
import com.example.invoicegen.pdf.InvoicePdfGenerator
import com.example.invoicegen.util.CurrencyUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LineItemState(
    val id: Long = 0,
    val description: String = "",
    val quantityText: String = "1",
    val unitPriceText: String = "0.00",
    val lineTotal: Long = 0L
)

data class InvoiceFormState(
    val id: Long = 0,
    val clientName: String = "",
    val clientAddress: String = "",
    val clientPhone: String = "",
    val proposalDate: String = "",
    val jobAddress: String = "",
    val datePlans: String = "",
    val architect: String = "",
    val workDescription: String = "",
    val documentType: DocumentType = DocumentType.PROPOSAL,
    val lineItems: List<LineItemState> = listOf(LineItemState()),
    val taxPercentText: String = "0.0",
    val notes: String = "",
    // Computed
    val subtotal: Long = 0L,
    val taxAmount: Long = 0L,
    val total: Long = 0L,
    // UI state
    val isSaving: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val pdfPath: String = "",
    val savedInvoiceId: Long = 0L,
    val errorMessage: String = ""
)

@HiltViewModel
class InvoiceInputViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val pdfGenerator: InvoicePdfGenerator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(InvoiceFormState())
    val state: StateFlow<InvoiceFormState> = _state.asStateFlow()

    private val editInvoiceId: Long = savedStateHandle.get<Long>("invoiceId") ?: 0L

    init {
        if (editInvoiceId != 0L) {
            loadInvoice(editInvoiceId)
        } else {
            // Default today's date
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            _state.update { it.copy(proposalDate = today) }
        }
    }

    private fun loadInvoice(id: Long) {
        viewModelScope.launch {
            val invoice = invoiceRepository.getInvoiceWithItems(id) ?: return@launch
            _state.update {
                it.copy(
                    id = invoice.id,
                    clientName = invoice.clientName,
                    clientAddress = invoice.clientAddress,
                    clientPhone = invoice.clientPhone,
                    proposalDate = invoice.proposalDate,
                    jobAddress = invoice.jobAddress,
                    datePlans = invoice.datePlans,
                    architect = invoice.architect,
                    workDescription = invoice.workDescription,
                    documentType = invoice.documentType,
                    lineItems = invoice.lineItems.map { li ->
                        LineItemState(
                            id = li.id,
                            description = li.description,
                            quantityText = String.format("%.2f", li.quantity),
                            unitPriceText = CurrencyUtil.centsToDisplayString(li.unitPrice),
                            lineTotal = li.lineTotal
                        )
                    }.ifEmpty { listOf(LineItemState()) },
                    taxPercentText = String.format("%.1f", invoice.taxPercent),
                    notes = invoice.notes,
                    pdfPath = invoice.pdfPath,
                    savedInvoiceId = invoice.id
                )
            }
            recalculate()
        }
    }

    // ---- Field update functions ----

    fun updateClientName(value: String) = _state.update { it.copy(clientName = value) }
    fun updateClientAddress(value: String) = _state.update { it.copy(clientAddress = value) }
    fun updateClientPhone(value: String) = _state.update { it.copy(clientPhone = value) }
    fun updateProposalDate(value: String) = _state.update { it.copy(proposalDate = value) }
    fun updateJobAddress(value: String) = _state.update { it.copy(jobAddress = value) }
    fun updateDatePlans(value: String) = _state.update { it.copy(datePlans = value) }
    fun updateArchitect(value: String) = _state.update { it.copy(architect = value) }
    fun updateWorkDescription(value: String) = _state.update { it.copy(workDescription = value) }
    fun updateDocumentType(type: DocumentType) = _state.update { it.copy(documentType = type) }
    fun updateNotes(value: String) = _state.update { it.copy(notes = value) }

    fun updateTaxPercent(value: String) {
        _state.update { it.copy(taxPercentText = value) }
        recalculate()
    }

    // ---- Line item operations ----

    fun addLineItem() {
        _state.update { it.copy(lineItems = it.lineItems + LineItemState()) }
    }

    fun removeLineItem(index: Int) {
        _state.update { s ->
            val updated = s.lineItems.toMutableList().also { it.removeAt(index) }
            s.copy(lineItems = updated.ifEmpty { listOf(LineItemState()) })
        }
        recalculate()
    }

    fun updateLineItemDescription(index: Int, value: String) {
        updateLineItem(index) { it.copy(description = value) }
    }

    fun updateLineItemQuantity(index: Int, value: String) {
        updateLineItem(index) { item ->
            val qty = value.toDoubleOrNull() ?: 0.0
            val unitPrice = CurrencyUtil.parseToCents(item.unitPriceText)
            val total = CurrencyUtil.calcLineTotal(qty, unitPrice)
            item.copy(quantityText = value, lineTotal = total)
        }
        recalculate()
    }

    fun updateLineItemUnitPrice(index: Int, value: String) {
        updateLineItem(index) { item ->
            val qty = item.quantityText.toDoubleOrNull() ?: 0.0
            val unitPrice = CurrencyUtil.parseToCents(value)
            val total = CurrencyUtil.calcLineTotal(qty, unitPrice)
            item.copy(unitPriceText = value, lineTotal = total)
        }
        recalculate()
    }

    private fun updateLineItem(index: Int, transform: (LineItemState) -> LineItemState) {
        _state.update { s ->
            val updated = s.lineItems.toMutableList()
            if (index in updated.indices) updated[index] = transform(updated[index])
            s.copy(lineItems = updated)
        }
    }

    private fun recalculate() {
        _state.update { s ->
            val subtotal = s.lineItems.sumOf { it.lineTotal }
            val taxPct = s.taxPercentText.toDoubleOrNull() ?: 0.0
            val tax = CurrencyUtil.calcTax(subtotal, taxPct)
            s.copy(subtotal = subtotal, taxAmount = tax, total = subtotal + tax)
        }
    }

    // ---- Persistence ----

    fun saveInvoice(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = "") }
            try {
                val invoice = buildInvoice()
                val id = if (invoice.id == 0L) {
                    invoiceRepository.saveNewInvoice(invoice)
                } else {
                    invoiceRepository.updateInvoice(invoice)
                    invoice.id
                }
                _state.update { it.copy(isSaving = false, savedInvoiceId = id) }
                onSaved(id)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Save failed") }
            }
        }
    }

    fun generatePdf(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingPdf = true, errorMessage = "") }
            try {
                // Ensure saved first
                val invoice = buildInvoice()
                val savedId = if (invoice.id == 0L) {
                    invoiceRepository.saveNewInvoice(invoice)
                } else {
                    invoiceRepository.updateInvoice(invoice)
                    invoice.id
                }
                val businessInfo = businessInfoRepository.getBusinessInfoOnce()
                val finalInvoice = invoiceRepository.getInvoiceWithItems(savedId)
                    ?: buildInvoice().copy(id = savedId)
                val pdfFile = pdfGenerator.generate(finalInvoice, businessInfo)
                invoiceRepository.updatePdfPath(savedId, pdfFile.absolutePath)
                _state.update {
                    it.copy(
                        isGeneratingPdf = false,
                        pdfPath = pdfFile.absolutePath,
                        savedInvoiceId = savedId
                    )
                }
                onResult(pdfFile.absolutePath)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isGeneratingPdf = false, errorMessage = e.message ?: "PDF generation failed")
                }
                onResult(null)
            }
        }
    }

    private fun buildInvoice(): Invoice {
        val s = _state.value
        val taxPct = s.taxPercentText.toDoubleOrNull() ?: 0.0
        return Invoice(
            id = s.id,
            clientName = s.clientName,
            clientAddress = s.clientAddress,
            clientPhone = s.clientPhone,
            proposalDate = s.proposalDate,
            jobAddress = s.jobAddress,
            datePlans = s.datePlans,
            architect = s.architect,
            workDescription = s.workDescription,
            documentType = s.documentType,
            lineItems = s.lineItems.map { li ->
                LineItem(
                    id = li.id,
                    description = li.description,
                    quantity = li.quantityText.toDoubleOrNull() ?: 0.0,
                    unitPrice = CurrencyUtil.parseToCents(li.unitPriceText),
                    lineTotal = li.lineTotal
                )
            },
            subtotal = s.subtotal,
            taxPercent = taxPct,
            total = s.total,
            notes = s.notes,
            pdfPath = s.pdfPath
        )
    }

    fun clearError() = _state.update { it.copy(errorMessage = "") }
}
