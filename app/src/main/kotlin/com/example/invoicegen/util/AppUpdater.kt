package com.example.invoicegen.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.invoicegen.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {

    private val latestReleaseUrl =
        "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"

    /** Returns the APK download URL from the latest public GitHub Release. */
    suspend fun getLatestAssetUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(latestReleaseUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "InvoiceMaker-App")
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // browser_download_url is publicly accessible on a public repo
            """"browser_download_url"\s*:\s*"([^"]+\.apk)"""".toRegex()
                .find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads the APK from [assetUrl], calling [onProgress] with 0–100.
     * Follows redirects (GitHub → CDN) without forwarding auth headers.
     */
    suspend fun downloadApk(
        context: Context,
        assetUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dest = File(context.cacheDir, "update.apk")
            var currentUrl = assetUrl
            var redirects = 0
            while (redirects < 5) {
                val conn = URL(currentUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "InvoiceMaker-App")
                conn.instanceFollowRedirects = false
                conn.connect()
                when (conn.responseCode) {
                    in 301..303 -> {
                        currentUrl = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        redirects++
                    }
                    200 -> {
                        val total = conn.contentLength
                        var downloaded = 0
                        conn.inputStream.use { input ->
                            dest.outputStream().use { output ->
                                val buf = ByteArray(8192)
                                var n: Int
                                while (input.read(buf).also { n = it } != -1) {
                                    output.write(buf, 0, n)
                                    downloaded += n
                                    if (total > 0) onProgress(downloaded * 100 / total)
                                }
                            }
                        }
                        conn.disconnect()
                        onProgress(100)
                        return@withContext dest
                    }
                    else -> { conn.disconnect(); return@withContext null }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Hands the downloaded APK off to Android's package installer. */
    fun install(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
