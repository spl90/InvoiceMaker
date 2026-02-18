package com.example.invoicegen.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.invoicegen.data.model.BusinessInfo
import com.example.invoicegen.data.repository.BusinessInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BusinessInfoViewModel @Inject constructor(
    private val repository: BusinessInfoRepository
) : ViewModel() {

    val businessInfo: StateFlow<BusinessInfo> = repository.getBusinessInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessInfo())

    fun save(info: BusinessInfo) {
        viewModelScope.launch { repository.save(info) }
    }

    /**
     * Copy the selected image to app private storage for persistence.
     * Returns the new absolute path.
     */
    fun copyLogoToPrivateStorage(context: Context, sourceFile: File): String {
        val logoDir = File(context.filesDir, "logos").apply { mkdirs() }
        val dest = File(logoDir, "business_logo.jpg")
        sourceFile.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }
}
