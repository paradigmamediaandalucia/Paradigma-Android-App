package com.example.paradigmaapp.config

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Config {
    private const val TAG = "Config"
    private const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/paradigmamediaandalucia/apiParadigma/main/api_config.json"

    lateinit var SPREAKER_API_BASE_URL: String
    lateinit var SPREAKER_USER_ID: String
    lateinit var SPREAKER_API_TOKEN: String
    lateinit var MAIN_WEBSITE_URL: String
    lateinit var LIVE_STREAM_API_URL: String
    lateinit var LIVE_STREAM_URL: String

    fun initialize(context: Context) {
        if (::SPREAKER_API_BASE_URL.isInitialized) {
            return
        }

        val appContext = context.applicationContext

        val bootstrapJson = loadCachedConfig(appContext) ?: loadLocalConfig(appContext)
        if (bootstrapJson != null) {
            applyConfig(bootstrapJson)
        }

        MainScope().launch {
            val remoteJson = fetchRemoteConfig()
            if (remoteJson != null) {
                cacheRemoteConfig(appContext, remoteJson)
                applyConfig(remoteJson)
            } else if (!::SPREAKER_API_BASE_URL.isInitialized) {
                val fallbackJson = loadLocalConfig(appContext)
                if (fallbackJson != null) {
                    applyConfig(fallbackJson)
                } else {
                    Log.e(TAG, "Failed to load configuration from all sources")
                }
            }
        }
    }

    private suspend fun fetchRemoteConfig(): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        return@withContext try {
            val url = URL(REMOTE_CONFIG_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }

            val responseCode = connection?.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Remote config request failed with code $responseCode")
                null
            } else {
                connection?.inputStream?.bufferedReader()?.use { reader -> reader.readText() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch remote config", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun cacheRemoteConfig(context: Context, json: String) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            .edit()
            .putString("cached_config", json)
            .apply()
    }

    private fun loadCachedConfig(context: Context): String? {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            .getString("cached_config", null)
    }

    private fun loadLocalConfig(context: Context): String? = try {
        context.resources.openRawResource(com.example.paradigmaapp.android.R.raw.config)
            .bufferedReader()
            .use { it.readText() }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bundled config", e)
        null
    }

    private fun applyConfig(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            SPREAKER_API_BASE_URL = jsonObject.getString("spreakerApiBaseUrl")
            SPREAKER_USER_ID = jsonObject.getString("spreakerUserId")
            SPREAKER_API_TOKEN = jsonObject.getString("spreakerApiToken")
            MAIN_WEBSITE_URL = jsonObject.getString("mainWebsiteUrl")
            LIVE_STREAM_API_URL = jsonObject.getString("liveStreamApiUrl")
            LIVE_STREAM_URL = jsonObject.getString("liveStreamUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse configuration", e)
        }
    }
}
