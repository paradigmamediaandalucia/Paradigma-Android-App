package com.example.paradigmaapp.android.utils

import kotlin.math.roundToInt

private const val DETAIL_THRESHOLD_PX = 480

/**
 * Represents a set of URLs that can be used to load Spreaker artwork.
 * [preferred] points to the best guess for the requested usage, [fallback] offers a second option.
 */
data class SpreakerImageSource(
    val preferred: String?,
    val fallback: String?
)

/**
 * Selects the most appropriate Spreaker artwork URL for a given target size.
 * Favour the CDN-sized URL for thumbnails and the original asset for large artwork.
 */
fun selectSpreakerImageSource(
    primaryUrl: String?,
    originalUrl: String?,
    targetPx: Int
): SpreakerImageSource {
    val sanitizedPrimary = primaryUrl.sanitise()
    val sanitizedOriginal = originalUrl.sanitise()

    val preferOriginal = sanitizedOriginal != null && (sanitizedPrimary == null || targetPx >= DETAIL_THRESHOLD_PX)

    val preferred = when {
        preferOriginal -> sanitizedOriginal ?: sanitizedPrimary
        else -> sanitizedPrimary ?: sanitizedOriginal
    }

    val fallback = when {
        preferred == null -> null
        preferred == sanitizedPrimary -> sanitizedOriginal
        else -> sanitizedPrimary
    }

    return SpreakerImageSource(
        preferred = preferred,
        fallback = fallback
    )
}

private fun String?.sanitise(): String? {
    val value = this?.trim().takeUnless { it.isNullOrEmpty() }
    val lower = value?.lowercase()
    return when (lower) {
        null, "null", "about:blank" -> null
        else -> value
    }
}

/** Converts density independent size to pixels when the caller only has dps. */
fun dpToPreferredSquarePx(dp: Float, density: Float): Int {
    val px = dp * density
    return px.coerceAtLeast(100f).coerceAtMost(1024f).roundToInt()
}
