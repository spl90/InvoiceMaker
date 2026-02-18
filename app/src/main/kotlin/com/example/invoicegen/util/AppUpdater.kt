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

    private val token get() = BuildConfig.GITHUB_TOKEN

    /**
     * Returns the API asset URL for the APK in the latest release, or null on failure.
     * Uses the asset's `url` field (not browser_download_url) so auth works for private repos.
     */
    suspend fun getLatestAssetUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(latestReleaseUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "InvoiceMaker-App")
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // The asset `url` field is the proper API endpoint for private repo downloads.
            // Pattern: "assets":[{"url":"https://api.github.com/repos/.../releases/assets/123",...}]
            """"assets"\s*:\s*\[\s*\{\s*"url"\s*:\s*"([^"]+)"""".toRegex()
                .find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads the APK asset from [assetUrl] (a GitHub API asset endpoint).
     * Handles the auth-redirect pattern: GitHub redirects to a pre-signed CDN URL.
     * Calls [onProgress] with 0–100. Returns the local File or null on failure.
     */
    suspend fun downloadApk(
        context: Context,
        assetUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dest = File(context.cacheDir, "update.apk")

            // Step 1: Ask GitHub for the asset. It will redirect to a signed CDN URL.
            val apiConn = URL(assetUrl).openConnection() as HttpURLConnection
            apiConn.setRequestProperty("Accept", "application/octet-stream")
            apiConn.setRequestProperty("User-Agent", "InvoiceMaker-App")
            apiConn.setRequestProperty("Authorization", "Bearer $token")
            apiConn.instanceFollowRedirects = false
            apiConn.connect()

            val cdnUrl = when (apiConn.responseCode) {
                // Private repo: GitHub sends a 302 to a signed S3/CDN URL
                in 301..303 -> {
                    val loc = apiConn.getHeaderField("Location")
                    apiConn.disconnect()
                    loc ?: return@withContext null
                }
                // Public repo: GitHub returns the bytes directly
                200 -> {
                    streamToFile(apiConn, dest, onProgress)
                    apiConn.disconnect()
                    return@withContext dest
                }
                else -> { apiConn.disconnect(); return@withContext null }
            }

            // Step 2: Download from the signed CDN URL — no auth header (pre-signed URL)
            val cdnConn = URL(cdnUrl).openConnection() as HttpURLConnection
            cdnConn.setRequestProperty("User-Agent", "InvoiceMaker-App")
            cdnConn.connect()
            if (cdnConn.responseCode != 200) { cdnConn.disconnect(); return@withContext null }
            streamToFile(cdnConn, dest, onProgress)
            cdnConn.disconnect()
            dest
        } catch (e: Exception) {
            null
        }
    }

    private fun streamToFile(
        conn: HttpURLConnection,
        dest: File,
        onProgress: (Int) -> Unit
    ) {
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
        onProgress(100)
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
