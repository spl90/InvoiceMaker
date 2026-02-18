package com.example.invoicegen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.invoicegen.data.model.Invoice
import com.example.invoicegen.data.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceHistoryViewModel @Inject constructor(
    private val repository: InvoiceRepository
) : ViewModel() {

    val invoices: StateFlow<List<Invoice>> = repository.getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch { repository.deleteInvoice(invoice) }
    }
}
