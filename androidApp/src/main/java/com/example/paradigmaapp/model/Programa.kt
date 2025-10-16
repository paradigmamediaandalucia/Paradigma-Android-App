package com.example.paradigmaapp.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull


@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProgramaResponse (
    val response: ShowsResponse
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ShowsResponse(
    val items: List<Programa> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null
)


@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Programa(
    @SerialName("show_id")
    @Serializable(with = NumericStringSerializer::class)
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("image_original_url")
    val imageOriginalUrl: String? = null,
    @SerialName("site_url")
    val siteUrl: String? = null,
    val slug: String? = null
)

object NumericStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NumericStringSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                when {
                    element.isString -> return element.content
                    element.longOrNull != null -> return element.longOrNull!!.toString()
                    element.doubleOrNull != null -> return element.doubleOrNull!!.toString()
                }
            }
        }
        return decoder.decodeString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
