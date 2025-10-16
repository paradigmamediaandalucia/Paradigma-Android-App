package com.example.paradigmaapp.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNames

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EpisodeResponse(
    val response: EpisodesEnvelope
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EpisodesEnvelope(
    val items: List<Episode> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EpisodeDetailResponse(
    val response: EpisodeWrapper
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EpisodeWrapper(
    val episode: Episode
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Episode(
    @SerialName("episode_id")
    @Serializable(with = NumericStringSerializer::class)
    val id: String,
    val title: String = "",
    val description: String = "",
    @JsonNames("playback_url", "audioUrl")
    val audioUrl: String = "",
    @JsonNames("download_url", "downloadUrl")
    val downloadUrl: String? = null,
    @JsonNames("image_url", "imageUrl")
    val imageUrl: String? = null,
    @JsonNames("image_original_url", "imageOriginalUrl")
    val imageOriginalUrl: String? = null,
    val duration: Long = 0L,
    @JsonNames("published_at", "date")
    val date: String? = null,
    @JsonNames("show_id", "programId")
    @Serializable(with = NumericStringSerializer::class)
    val programId: String = "",
    val slug: String = "",
    @SerialName("type")
    val type: String? = null,
    val explicit: Boolean = false,
    @SerialName("site_url")
    val siteUrl: String? = null,
    @SerialName("waveform_url")
    val waveformUrl: String? = null,
    @SerialName("stream_id")
    val streamId: String? = null,
    val show: Programa? = null,
    val content: String? = null,
    val excerpt: String? = null,
    val embedded: Embedded? = null,
    val programaIds: List<String>? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Embedded(
    @SerialName("wp:term")
    val terms: List<List<Term>> = emptyList()
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Term(
    val id: String,
    val name: String,
    val slug: String,
    val taxonomy: String
)
