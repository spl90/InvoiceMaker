package com.example.invoicegen.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object EmailUtil {
    fun sendPdfEmail(
        context: Context,
        pdfFile: File,
        toEmail: String = "",
        subject: String = "Invoice / Proposal"
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, if (toEmail.isNotBlank()) arrayOf(toEmail) else emptyArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Please find the attached proposal/invoice.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send Invoice via…"))
    }
}
