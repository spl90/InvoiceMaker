package com.example.invoicegen.ui.input

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.invoicegen.data.model.DocumentType
import com.example.invoicegen.util.CurrencyUtil
import com.example.invoicegen.util.EmailUtil
import com.example.invoicegen.util.PrintUtil
import com.example.invoicegen.viewmodel.InvoiceInputViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceInputScreen(
    onSaved: () -> Unit = {},
    viewModel: InvoiceInputViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage.isNotEmpty()) {
            snackbarHost.showSnackbar(state.errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.id == 0L) "New Invoice" else "Edit Invoice")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Document type selector
            DocumentTypeSection(
                selected = state.documentType,
                onSelect = viewModel::updateDocumentType
            )

            HorizontalDivider()

            // Client / Job info - two column on tablets
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Proposal Submitted To")
                        FormField("Client Name", state.clientName, viewModel::updateClientName)
                        FormField("Client Address", state.clientAddress, viewModel::updateClientAddress)
                        FormField("Client Phone", state.clientPhone, viewModel::updateClientPhone,
                            keyboardType = KeyboardType.Phone)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Work To Be Performed At")
                        FormField("Job Address", state.jobAddress, viewModel::updateJobAddress)
                        FormField("Date of Plans", state.datePlans, viewModel::updateDatePlans)
                        FormField("Architect", state.architect, viewModel::updateArchitect)
                    }
                }
            } else {
                SectionHeader("Proposal Submitted To")
                FormField("Client Name", state.clientName, viewModel::updateClientName)
                FormField("Client Address", state.clientAddress, viewModel::updateClientAddress)
                FormField("Client Phone", state.clientPhone, viewModel::updateClientPhone,
                    keyboardType = KeyboardType.Phone)
                SectionHeader("Work To Be Performed At")
                FormField("Job Address", state.jobAddress, viewModel::updateJobAddress)
                FormField("Date of Plans", state.datePlans, viewModel::updateDatePlans)
                FormField("Architect", state.architect, viewModel::updateArchitect)
            }

            FormField("Proposal Date (MM/DD/YYYY)", state.proposalDate, viewModel::updateProposalDate)

            HorizontalDivider()

            // Work description
            SectionHeader("Scope of Work")
            OutlinedTextField(
                value = state.workDescription,
                onValueChange = viewModel::updateWorkDescription,
                label = { Text("Work Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            HorizontalDivider()

            // Line items
            LineItemsSection(
                lineItems = state.lineItems,
                onAddItem = viewModel::addLineItem,
                onRemoveItem = viewModel::removeLineItem,
                onDescriptionChange = viewModel::updateLineItemDescription,
                onQuantityChange = viewModel::updateLineItemQuantity,
                onUnitPriceChange = viewModel::updateLineItemUnitPrice,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Tax + totals
            TotalsSection(state, viewModel)

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            HorizontalDivider()

            // Action buttons
            ActionButtons(
                isSaving = state.isSaving,
                isGeneratingPdf = state.isGeneratingPdf,
                pdfPath = state.pdfPath,
                context = context,
                onSave = { viewModel.saveInvoice { onSaved() } },
                onGeneratePdf = {
                    viewModel.generatePdf { path ->
                        if (path != null) {
                            Toast.makeText(context, "PDF generated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onEmail = {
                    if (state.pdfPath.isNotEmpty()) {
                        EmailUtil.sendPdfEmail(context, File(state.pdfPath))
                    } else {
                        Toast.makeText(context, "Generate PDF first", Toast.LENGTH_SHORT).show()
                    }
                },
                onPrint = {
                    if (state.pdfPath.isNotEmpty()) {
                        PrintUtil.printPdf(context, File(state.pdfPath))
                    } else {
                        Toast.makeText(context, "Generate PDF first", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DocumentTypeSection(
    selected: DocumentType,
    onSelect: (DocumentType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Document Type:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == DocumentType.PROPOSAL,
                onClick = { onSelect(DocumentType.PROPOSAL) }
            )
            Text("Proposal", maxLines = 1)
            Spacer(Modifier.width(24.dp))
            RadioButton(
                selected = selected == DocumentType.CONTRACT,
                onClick = { onSelect(DocumentType.CONTRACT) }
            )
            Text("Contract", maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun TotalsSection(
    state: com.example.invoicegen.viewmodel.InvoiceFormState,
    viewModel: InvoiceInputViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.taxPercentText,
            onValueChange = viewModel::updateTaxPercent,
            label = { Text("Tax %") },
            modifier = Modifier.width(120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            suffix = { Text("%") }
        )
        Column(horizontalAlignment = Alignment.End) {
            TotalRow("Subtotal", CurrencyUtil.formatCents(state.subtotal))
            TotalRow("Tax", CurrencyUtil.formatCents(state.taxAmount))
            TotalRow("TOTAL", CurrencyUtil.formatCents(state.total), bold = true)
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionButtons(
    isSaving: Boolean,
    isGeneratingPdf: Boolean,
    pdfPath: String,
    context: Context,
    onSave: () -> Unit,
    onGeneratePdf: () -> Unit,
    onEmail: () -> Unit,
    onPrint: () -> Unit
) {
    val isWide = LocalConfiguration.current.screenWidthDp >= 600
    val busy = isSaving || isGeneratingPdf

    @Composable
    fun SaveBtn(modifier: Modifier = Modifier) {
        Button(onClick = onSave, enabled = !busy, modifier = modifier) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Save")
        }
    }

    @Composable
    fun PdfBtn(modifier: Modifier = Modifier) {
        Button(onClick = onGeneratePdf, enabled = !busy, modifier = modifier) {
            if (isGeneratingPdf) CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Generate PDF")
        }
    }

    @Composable
    fun EmailBtn(modifier: Modifier = Modifier) {
        Button(onClick = onEmail, enabled = pdfPath.isNotEmpty() && !busy, modifier = modifier) {
            Icon(Icons.Default.Email, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Email")
        }
    }

    @Composable
    fun PrintBtn(modifier: Modifier = Modifier) {
        Button(onClick = onPrint, enabled = pdfPath.isNotEmpty() && !busy, modifier = modifier) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Print")
        }
    }

    if (isWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaveBtn()
            PdfBtn()
            EmailBtn()
            PrintBtn()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SaveBtn(modifier = Modifier.weight(1f))
                PdfBtn(modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmailBtn(modifier = Modifier.weight(1f))
                PrintBtn(modifier = Modifier.weight(1f))
            }
        }
    }
}
