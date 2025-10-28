package com.example.paradigmaapp.android.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LatestVersionProvider {
    // URL donde se publica la última versión
    private const val VERSION_URL = "https://raw.githubusercontent.com/paradigmamediaandalucia/app_version.json/refs/heads/main/app_version.json"

    suspend fun fetchLatestVersion(): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        return@withContext try {
            val url = URL(VERSION_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            json.optString("versionName", null)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
