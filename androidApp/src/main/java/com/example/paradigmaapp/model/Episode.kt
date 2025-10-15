package com.example.paradigmaapp.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Episode(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val duration: Long,
    val date: String,
    val programId: String,
    val slug: String,
    val content: String?,
    val excerpt: String?,
    val embedded: Embedded?,
    val programaIds: List<String>?
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Embedded(
    val terms: List<List<Programa>>?
)
