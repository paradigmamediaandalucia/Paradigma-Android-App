package com.example.paradigmaapp.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RadioInfo constructor(
    val title: String,
    val art: String,
    val imageUrl: String
)
