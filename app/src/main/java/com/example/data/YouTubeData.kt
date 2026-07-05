package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Unified Domain Model for easy Compose UI consumption
data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val description: String,
    val publishedAt: String,
    val viewCountString: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    @Json(name = "items") val items: List<SearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SearchItem(
    @Json(name = "id") val id: SearchId,
    @Json(name = "snippet") val snippet: Snippet
)

@JsonClass(generateAdapter = true)
data class SearchId(
    @Json(name = "kind") val kind: String = "",
    @Json(name = "videoId") val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoResponse(
    @Json(name = "items") val items: List<VideoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VideoItem(
    @Json(name = "id") val id: String,
    @Json(name = "snippet") val snippet: Snippet,
    @Json(name = "statistics") val statistics: Statistics? = null
)

@JsonClass(generateAdapter = true)
data class Snippet(
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "channelTitle") val channelTitle: String = "",
    @Json(name = "thumbnails") val thumbnails: Thumbnails? = null,
    @Json(name = "publishedAt") val publishedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class Thumbnails(
    @Json(name = "high") val high: ThumbnailDetail? = null,
    @Json(name = "medium") val medium: ThumbnailDetail? = null,
    @Json(name = "default") val default: ThumbnailDetail? = null
)

@JsonClass(generateAdapter = true)
data class ThumbnailDetail(
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class Statistics(
    @Json(name = "viewCount") val viewCount: String? = null,
    @Json(name = "likeCount") val likeCount: String? = null
)

// Helper to clean up HTML entities in titles and descriptions returned by the YouTube API
fun String.unescapeHtml(): String {
    return this
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&ndash;", "–")
        .replace("&mdash;", "—")
}

// Format views helper: 1234567 -> 1.2M, 45000 -> 45K
fun formatViewCount(views: String?): String? {
    if (views == null) return null
    val count = views.toLongOrNull() ?: return null
    return when {
        count >= 1_000_000 -> {
            val millions = count.toDouble() / 1_000_000
            String.format("%.1fM", millions).replace(".0", "")
        }
        count >= 1_000 -> {
            val thousands = count.toDouble() / 1_000
            String.format("%.1fK", thousands).replace(".0", "")
        }
        else -> count.toString()
    }
}

// Simple date parser to make standard date look nicer
fun formatPublishedDate(dateStr: String): String {
    // Expected: 2023-10-01T12:00:00Z -> we can just extract the date portion YYYY-MM-DD
    if (dateStr.length >= 10) {
        return dateStr.substring(0, 10)
    }
    return dateStr
}
