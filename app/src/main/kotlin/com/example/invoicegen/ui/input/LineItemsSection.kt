package com.example.invoicegen.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.invoicegen.util.CurrencyUtil
import com.example.invoicegen.viewmodel.LineItemState

@Composable
fun LineItemsSection(
    lineItems: List<LineItemState>,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDescriptionChange: (Int, String) -> Unit,
    onQuantityChange: (Int, String) -> Unit,
    onUnitPriceChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "Line Items",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall)
            Text("Description", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text("Qty", modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text("Unit Price", modifier = Modifier.width(88.dp), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text("Total", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(40.dp)) // delete button space
        }

        lineItems.forEachIndexed { index, item ->
            LineItemRow(
                index = index,
                item = item,
                onRemove = { onRemoveItem(index) },
                onDescriptionChange = { onDescriptionChange(index, it) },
                onQuantityChange = { onQuantityChange(index, it) },
                onUnitPriceChange = { onUnitPriceChange(index, it) }
            )
            if (index < lineItems.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onAddItem,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Item")
        }
    }
}

@Composable
private fun LineItemRow(
    index: Int,
    item: LineItemState,
    onRemove: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "${index + 1}",
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = item.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Description", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = item.quantityText,
            onValueChange = onQuantityChange,
            modifier = Modifier.width(56.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = item.unitPriceText,
            onValueChange = onUnitPriceChange,
            modifier = Modifier.width(88.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("$", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        Text(
            text = CurrencyUtil.formatCents(item.lineTotal),
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
