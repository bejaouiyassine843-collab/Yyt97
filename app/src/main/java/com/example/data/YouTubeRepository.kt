package com.example.data

class YouTubeRepository(private val apiService: YouTubeApiService) {

    suspend fun searchVideos(query: String, apiKey: String): List<YouTubeVideo> {
        val response = apiService.searchVideos(query = query, apiKey = apiKey)
        return response.items.mapNotNull { item ->
            val videoId = item.id.videoId ?: return@mapNotNull null
            YouTubeVideo(
                id = videoId,
                title = item.snippet.title.unescapeHtml(),
                channelTitle = item.snippet.channelTitle.unescapeHtml(),
                thumbnailUrl = item.snippet.thumbnails?.high?.url 
                    ?: item.snippet.thumbnails?.medium?.url 
                    ?: item.snippet.thumbnails?.default?.url 
                    ?: "",
                description = item.snippet.description.unescapeHtml(),
                publishedAt = formatPublishedDate(item.snippet.publishedAt),
                viewCountString = null
            )
        }
    }

    suspend fun getPopularVideos(apiKey: String): List<YouTubeVideo> {
        val response = apiService.getPopularVideos(apiKey = apiKey)
        return response.items.map { item ->
            YouTubeVideo(
                id = item.id,
                title = item.snippet.title.unescapeHtml(),
                channelTitle = item.snippet.channelTitle.unescapeHtml(),
                thumbnailUrl = item.snippet.thumbnails?.high?.url 
                    ?: item.snippet.thumbnails?.medium?.url 
                    ?: item.snippet.thumbnails?.default?.url 
                    ?: "",
                description = item.snippet.description.unescapeHtml(),
                publishedAt = formatPublishedDate(item.snippet.publishedAt),
                viewCountString = formatViewCount(item.statistics?.viewCount)
            )
        }
    }
}
