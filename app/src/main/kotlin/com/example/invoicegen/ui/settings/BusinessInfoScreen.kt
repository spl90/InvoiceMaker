package com.example.invoicegen.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.invoicegen.util.AppUpdater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.invoicegen.data.model.BusinessInfo
import com.example.invoicegen.viewmodel.BusinessInfoViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessInfoScreen(
    viewModel: BusinessInfoViewModel = hiltViewModel()
) {
    val savedInfo by viewModel.businessInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Local editable state, initialized from saved
    var businessName by remember(savedInfo) { mutableStateOf(savedInfo.businessName) }
    var address by remember(savedInfo) { mutableStateOf(savedInfo.address) }
    var phone by remember(savedInfo) { mutableStateOf(savedInfo.phone) }
    var email by remember(savedInfo) { mutableStateOf(savedInfo.email) }
    var logoPath by remember(savedInfo) { mutableStateOf(savedInfo.logoPath) }

    // File picker for logo
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Copy to private storage
                val inputStream = context.contentResolver.openInputStream(uri)
                val logoDir = File(context.filesDir, "logos").apply { mkdirs() }
                val dest = File(logoDir, "business_logo.jpg")
                inputStream?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                logoPath = dest.absolutePath
            } catch (e: Exception) {
                scope.launch { snackbarHost.showSnackbar("Failed to save logo: ${e.message}") }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Settings") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Your Business Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "This information appears in the header of every PDF invoice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Logo section
            LogoSection(
                logoPath = logoPath,
                onPickLogo = { logoPicker.launch("image/*") }
            )

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Business Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val info = BusinessInfo(
                        businessName = businessName,
                        address = address,
                        phone = phone,
                        email = email,
                        logoPath = logoPath
                    )
                    viewModel.save(info)
                    scope.launch { snackbarHost.showSnackbar("Business info saved!") }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings")
            }

            HorizontalDivider()

            UpdateSection()
        }
    }
}

@Composable
private fun LogoSection(
    logoPath: String,
    onPickLogo: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo preview
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (logoPath.isNotEmpty() && File(logoPath).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(File(logoPath)),
                    contentDescription = "Business Logo",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "No logo",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column {
            Text("Business Logo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Text(
                if (logoPath.isNotEmpty()) "Logo selected" else "No logo selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = onPickLogo) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Pick Logo Image")
            }
        }
    }
}

@Composable
private fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "App Update",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Button(
            onClick = {
                if (!isUpdating) {
                    isUpdating = true
                    status = "Checking for update..."
                    scope.launch {
                        val apkUrl = AppUpdater.getLatestApkUrl()
                        if (apkUrl == null) {
                            status = "Could not reach update server. Check your connection."
                            isUpdating = false
                            return@launch
                        }
                        val apkFile = AppUpdater.downloadApk(context, apkUrl) { progress ->
                            status = "Downloading... $progress%"
                        }
                        if (apkFile == null) {
                            status = "Download failed. Try again."
                            isUpdating = false
                            return@launch
                        }
                        status = "Installing — tap Install when prompted."
                        AppUpdater.install(context, apkFile)
                        isUpdating = false
                    }
                }
            },
            enabled = !isUpdating
        ) {
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.SystemUpdate, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("Update App")
        }
        if (status.isNotEmpty()) {
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = if (status.contains("fail", ignoreCase = true) ||
                    status.contains("Could not", ignoreCase = true))
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
