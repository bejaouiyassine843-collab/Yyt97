package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.YouTubeApiService
import com.example.data.YouTubeRepository
import com.example.data.YouTubeVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface YouTubeUiState {
    object Loading : YouTubeUiState
    data class Success(val videos: List<YouTubeVideo>) : YouTubeUiState
    data class Error(val message: String) : YouTubeUiState
}

class YouTubeViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = YouTubeApiService.create()
    private val repository = YouTubeRepository(apiService)
    private val sharedPrefs = application.getSharedPreferences("tubelight_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<YouTubeUiState>(YouTubeUiState.Loading)
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVideo = MutableStateFlow<YouTubeVideo?>(null)
    val selectedVideo: StateFlow<YouTubeVideo?> = _selectedVideo.asStateFlow()

    private val _isPlayerMinimized = MutableStateFlow(false)
    val isPlayerMinimized: StateFlow<Boolean> = _isPlayerMinimized.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Retrieve custom key or default to provided key
    private val _apiKey = MutableStateFlow(getSavedApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    val categories = listOf("All", "Music", "Gaming", "Tech", "Cooking", "Sports", "Comedy", "Education")

    init {
        loadVideos()
    }

    private fun getSavedApiKey(): String {
        val saved = sharedPrefs.getString("custom_api_key", null)
        if (!saved.isNullOrBlank()) {
            return saved
        }
        
        // Use BuildConfig if valid, else use user's explicit key
        val buildKey = BuildConfig.YOUTUBE_API_KEY
        if (buildKey.isNotEmpty() && !buildKey.startsWith("MY_YOUTUBE")) {
            return buildKey
        }
        
        return "AIzaSyD5SbdDhCEalfSKLlOokxrXd34qVUfs1yk"
    }

    fun updateApiKey(newKey: String) {
        sharedPrefs.edit().putString("custom_api_key", newKey).apply()
        _apiKey.value = newKey
        // Reload videos with the new key
        loadVideos()
    }

    fun restoreDefaultApiKey() {
        sharedPrefs.edit().remove("custom_api_key").apply()
        _apiKey.value = getSavedApiKey()
        loadVideos()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun executeSearch(query: String) {
        _searchQuery.value = query
        _selectedCategory.value = "" // clear category selection when searching manually
        loadVideos()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = "" // clear search query when selecting category
        loadVideos()
    }

    fun selectVideo(video: YouTubeVideo?) {
        _selectedVideo.value = video
        _isPlayerMinimized.value = false // Expand player on selecting new video
    }

    fun setPlayerMinimized(minimized: Boolean) {
        _isPlayerMinimized.value = minimized
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = YouTubeUiState.Loading
            val currentKey = _apiKey.value
            val query = _searchQuery.value
            val category = _selectedCategory.value

            try {
                val videos = if (query.isNotBlank()) {
                    repository.searchVideos(query, currentKey)
                } else if (category.isNotBlank() && category != "All") {
                    repository.searchVideos(category, currentKey)
                } else {
                    repository.getPopularVideos(currentKey)
                }
                
                if (videos.isEmpty()) {
                    _uiState.value = YouTubeUiState.Error("لم يتم العثور على أي مقاطع فيديو. يرجى التحقق من مفتاح الـ API الخاص بك.")
                } else {
                    _uiState.value = YouTubeUiState.Success(videos)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = YouTubeUiState.Error(
                    e.localizedMessage ?: "حدث خطأ أثناء جلب البيانات. يرجى التحقق من الاتصال بالإنترنت أو مفتاح الـ API."
                )
            }
        }
    }
}
