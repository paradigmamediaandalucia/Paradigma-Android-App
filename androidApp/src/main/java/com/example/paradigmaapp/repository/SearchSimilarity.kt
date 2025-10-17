package com.example.paradigmaapp.repository

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

internal fun normalizeForSearch(value: String): String {
    if (value.isBlank()) return ""
    val normalized = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    val withoutAccents = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return withoutAccents
        .replace("&", "and")
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun searchSimilarityScore(rawQuery: String, rawTarget: String): Double {
    if (rawQuery.isBlank() || rawTarget.isBlank()) return 0.0
    val query = normalizeForSearch(rawQuery)
    val target = normalizeForSearch(rawTarget)
    if (query.isEmpty() || target.isEmpty()) return 0.0
    if (target.contains(query)) return 1.0

    val queryTokens = query.split(' ').filter { it.isNotBlank() }
    val targetTokens = target.split(' ').filter { it.isNotBlank() }

    val tokenScore = queryTokens.maxOfOrNull { queryToken ->
        targetTokens.maxOfOrNull { targetToken -> normalizedLevenshteinSimilarity(queryToken, targetToken) } ?: 0.0
    } ?: 0.0

    val overallScore = normalizedLevenshteinSimilarity(query, target)
    return max(tokenScore, overallScore)
}

private fun normalizedLevenshteinSimilarity(lhs: String, rhs: String): Double {
    if (lhs == rhs) return 1.0
    if (lhs.isEmpty() || rhs.isEmpty()) return 0.0

    val distance = levenshteinDistance(lhs, rhs)
    val maxLength = max(lhs.length, rhs.length)
    if (maxLength == 0) return 1.0
    return 1.0 - distance.toDouble() / maxLength
}

private fun levenshteinDistance(lhs: String, rhs: String): Int {
    if (lhs == rhs) return 0
    if (lhs.isEmpty()) return rhs.length
    if (rhs.isEmpty()) return lhs.length

    var previous = IntArray(rhs.length + 1) { it }
    var current = IntArray(rhs.length + 1)

    for (i in 1..lhs.length) {
        current[0] = i
        val lhsChar = lhs[i - 1]
        for (j in 1..rhs.length) {
            val cost = if (lhsChar == rhs[j - 1]) 0 else 1
            current[j] = min(
                min(current[j - 1] + 1, previous[j] + 1),
                previous[j - 1] + cost
            )
        }
        val temp = previous
        previous = current
        current = temp
    }

    return previous[rhs.length]
}
