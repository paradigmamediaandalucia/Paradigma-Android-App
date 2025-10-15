package com.example.paradigmaapp.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Programa constructor(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val episodes: List<Episode> = emptyList()
)
