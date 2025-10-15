package com.example.paradigmaapp.config

import android.content.Context
import org.json.JSONObject
import java.io.IOException

object Config {
    lateinit var SPREAKER_API_BASE_URL: String
    lateinit var SPREAKER_USER_ID: String
    lateinit var SPREAKER_API_TOKEN: String
    lateinit var MAIN_WEBSITE_URL: String
    lateinit var LIVE_STREAM_API_URL: String
    lateinit var LIVE_STREAM_URL: String

    fun initialize(context: Context) {
        try {
            val inputStream = context.resources.openRawResource(com.example.paradigmaapp.android.R.raw.config)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            SPREAKER_API_BASE_URL = jsonObject.getString("spreakerApiBaseUrl")
            SPREAKER_USER_ID = jsonObject.getString("spreakerUserId")
            SPREAKER_API_TOKEN = jsonObject.getString("spreakerApiToken")
            MAIN_WEBSITE_URL = jsonObject.getString("mainWebsiteUrl")
            LIVE_STREAM_API_URL = jsonObject.getString("liveStreamApiUrl")
            LIVE_STREAM_URL = jsonObject.getString("liveStreamUrl")

        } catch (e: IOException) {
            e.printStackTrace()
            // Handle error: e.g., log it, throw a runtime exception, or set default values
        } catch (e: org.json.JSONException) {
            e.printStackTrace()
            // Handle error: e.g., log it, throw a runtime exception, or set default values
        }
    }
}
