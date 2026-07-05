package com.example

import android.os.Bundle
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.YouTubeVideo
import com.example.ui.YouTubeUiState
import com.example.ui.YouTubeViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.StudioCard
import com.example.ui.theme.YoutubeRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.AccentGrey
import com.example.ui.theme.BorderWhiteTranslucent
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.PureWhite
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: YouTubeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepBlack)
                ) { innerPadding ->
                    YouTubeAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeAppScreen(
    viewModel: YouTubeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val isPlayerMinimized by viewModel.isPlayerMinimized.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var isPlayerFullScreenLandscape by remember { mutableStateOf(false) }

    // Check device orientation dynamically
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    // Remember a single shared WebView instance so video playback is preserved
    // and never reset when switching between minimized/expanded/orientation states.
    var currentPlaybackProgress by remember { mutableStateOf(0f) }
    var totalDuration by remember { mutableStateOf(0f) }
    var isVideoPlayingState by remember { mutableStateOf(true) }
    var playerErrorMessage by remember { mutableStateOf<String?>(null) }
    var lastConsoleMessage by remember { mutableStateOf<String?>(null) }
    var errorLogText by remember { mutableStateOf<String?>(null) }

    var activeYouTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }

    val sharedYouTubePlayerView = remember {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            
            val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
            lifecycleOwner?.lifecycle?.addObserver(this)
            
            val options = IFramePlayerOptions.Builder()
                .controls(0) // disable default controls
                .build()
            
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    activeYouTubePlayer = youTubePlayer
                    selectedVideo?.let { video ->
                        youTubePlayer.loadVideo(video.id, 0f)
                    }
                }
                
                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    currentPlaybackProgress = second
                }
                
                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    totalDuration = duration
                }
                
                override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                    isVideoPlayingState = (state == PlayerConstants.PlayerState.PLAYING)
                }
                
                override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                    val msg = "خطأ في مشغل الفيديو: $error"
                    android.util.Log.e("YTPlayer", msg)
                    playerErrorMessage = msg
                    errorLogText = msg
                }
            }, options)
        }
    }

    var lastLoadedVideoId by remember { mutableStateOf("") }

    LaunchedEffect(selectedVideo) {
        playerErrorMessage = null
        lastConsoleMessage = null
        errorLogText = null
        selectedVideo?.let { video ->
            if (lastLoadedVideoId != video.id) {
                lastLoadedVideoId = video.id
                activeYouTubePlayer?.loadVideo(video.id, 0f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Landscape Full Screen Player Layout
        if (isLandscape && isPlayerFullScreenLandscape && selectedVideo != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { sharedYouTubePlayerView },
                    modifier = Modifier.fillMaxSize()
                )
                VideoControlsOverlay(
                    youTubePlayer = activeYouTubePlayer,
                    isPlaying = isVideoPlayingState,
                    progress = currentPlaybackProgress,
                    duration = totalDuration,
                    playerErrorMessage = playerErrorMessage,
                    lastConsoleMessage = lastConsoleMessage,
                    errorLogText = errorLogText,
                    onErrorDismiss = {
                        playerErrorMessage = null
                        lastConsoleMessage = null
                        errorLogText = null
                    },
                    onMinimizeClick = {
                        isPlayerFullScreenLandscape = false
                        viewModel.setPlayerMinimized(true)
                    },
                    onFullScreenClick = {
                        isPlayerFullScreenLandscape = false
                    }
                )
            }
        } else {
            // Standard feed Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBlack)
                    .padding(bottom = if (!isLandscape && selectedVideo != null && isPlayerMinimized) 80.dp else 0.dp)
            ) {
                // Top Search Bar
                TopSearchBar(
                    query = searchQuery,
                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { viewModel.executeSearch(it) },
                    onSettingsClick = { showSettingsDialog = true }
                )

                // Category row
                CategoryRow(
                    categories = viewModel.categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.selectCategory(it) }
                )

                if (isLandscape) {
                    // Landscape Master-Detail Layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Left List Grid (45% width)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            VideoListContent(
                                uiState = uiState,
                                selectedVideo = selectedVideo,
                                onVideoSelect = { viewModel.selectVideo(it) },
                                onRetry = { viewModel.loadVideos() }
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right player details (55% width)
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        ) {
                            if (selectedVideo != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, BorderWhiteTranslucent), RoundedCornerShape(16.dp))
                                ) {
                                    AndroidView(
                                        factory = { sharedYouTubePlayerView },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    VideoControlsOverlay(
                                        youTubePlayer = activeYouTubePlayer,
                                        isPlaying = isVideoPlayingState,
                                        progress = currentPlaybackProgress,
                                        duration = totalDuration,
                                        playerErrorMessage = playerErrorMessage,
                                        lastConsoleMessage = lastConsoleMessage,
                                        errorLogText = errorLogText,
                                        onErrorDismiss = {
                                            playerErrorMessage = null
                                            lastConsoleMessage = null
                                            errorLogText = null
                                        },
                                        onMinimizeClick = { viewModel.setPlayerMinimized(true) },
                                        onFullScreenClick = { isPlayerFullScreenLandscape = true }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = selectedVideo!!.title,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedVideo!!.channelTitle,
                                    color = YoutubeRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (selectedVideo!!.viewCountString != null) {
                                        "مشاهدة ${selectedVideo!!.viewCountString} • تاريخ النشر: ${selectedVideo!!.publishedAt}"
                                    } else {
                                        "تاريخ النشر: ${selectedVideo!!.publishedAt}"
                                    },
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            } else {
                                // Default Empty Player Card on Right
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(StudioCard)
                                        .border(BorderStroke(1.dp, BorderWhiteTranslucent), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = YoutubeRed,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "الرجاء اختيار فيديو للبدء في التشغيل",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Home Grid Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        VideoListContent(
                            uiState = uiState,
                            selectedVideo = selectedVideo,
                            onVideoSelect = { viewModel.selectVideo(it) },
                            onRetry = { viewModel.loadVideos() }
                        )
                    }
                }
            }

            // Portrait Sliding Overlay Player with gesture dragging for Swipe-To-Minimize support
            if (!isLandscape && selectedVideo != null) {
                var dragOffsetY by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!isPlayerMinimized) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (dragAmount.y > 0 || dragOffsetY > 0) {
                                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                                            }
                                        },
                                        onDragEnd = {
                                            if (dragOffsetY > 250f) {
                                                viewModel.setPlayerMinimized(true)
                                            }
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                            } else Modifier
                        )
                        .offset { IntOffset(0, if (isPlayerMinimized) 0 else dragOffsetY.roundToInt()) }
                ) {
                    if (isPlayerMinimized) {
                        // Gesture / Mini-player Bar Docked at bottom
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable { viewModel.setPlayerMinimized(false) }
                                .testTag("mini_player_bar"),
                            colors = CardDefaults.cardColors(containerColor = StudioCard),
                            border = BorderStroke(1.dp, BorderWhiteTranslucent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AndroidView(
                                        factory = { sharedYouTubePlayerView },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = selectedVideo!!.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedVideo!!.channelTitle,
                                        maxLines = 1,
                                        color = YoutubeRed,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setPlayerMinimized(false) },
                                    modifier = Modifier.testTag("mini_player_expand")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Maximize",
                                        tint = TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.selectVideo(null) },
                                    modifier = Modifier.testTag("mini_player_close")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        // Expanded Full Screen Layout in Portrait mode
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DeepBlack)
                        ) {
                            // Header row with back indicator to dismiss/minimize
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.setPlayerMinimized(true) },
                                    modifier = Modifier.testTag("minimize_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Minimize",
                                        tint = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedVideo?.title ?: "",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.selectVideo(null) },
                                    modifier = Modifier.testTag("close_player_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextPrimary
                                    )
                                }
                            }

                            // Interactive YouTube Player Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            ) {
                                AndroidView(
                                    factory = { sharedYouTubePlayerView },
                                    modifier = Modifier.fillMaxSize()
                                )
                                VideoControlsOverlay(
                                    youTubePlayer = activeYouTubePlayer,
                                    isPlaying = isVideoPlayingState,
                                    progress = currentPlaybackProgress,
                                    duration = totalDuration,
                                    playerErrorMessage = playerErrorMessage,
                                    lastConsoleMessage = lastConsoleMessage,
                                    errorLogText = errorLogText,
                                    onErrorDismiss = {
                                        playerErrorMessage = null
                                        lastConsoleMessage = null
                                        errorLogText = null
                                    },
                                    onMinimizeClick = { viewModel.setPlayerMinimized(true) },
                                    onFullScreenClick = {
                                        val activity = context as? ComponentActivity
                                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                )
                            }

                            // Scrollable Video Info and Recommended / Suggested Videos List!
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(DeepBlack),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = selectedVideo!!.title,
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 24.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = selectedVideo!!.channelTitle,
                                            color = YoutubeRed,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (selectedVideo!!.viewCountString != null) {
                                                "مشاهدة ${selectedVideo!!.viewCountString} • تاريخ النشر: ${selectedVideo!!.publishedAt}"
                                            } else {
                                                "تاريخ النشر: ${selectedVideo!!.publishedAt}"
                                            },
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = selectedVideo!!.description,
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "فيديوهات مقترحة لكم",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (uiState is YouTubeUiState.Success) {
                                    val suggestedVideos = (uiState as YouTubeUiState.Success).videos.filter { it.id != selectedVideo!!.id }
                                    items(suggestedVideos) { video ->
                                        VideoCardItem(
                                            video = video,
                                            isPlaying = false,
                                            onClick = { viewModel.selectVideo(video) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings API Key Dialog
    if (showSettingsDialog) {
        ApiKeySettingsDialog(
            currentKey = apiKey,
            onDismiss = { showSettingsDialog = false },
            onSave = { newKey ->
                viewModel.updateApiKey(newKey)
                showSettingsDialog = false
            },
            onRestoreDefault = {
                viewModel.restoreDefaultApiKey()
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun VideoControlsOverlay(
    youTubePlayer: YouTubePlayer?,
    isPlaying: Boolean,
    progress: Float,
    duration: Float,
    playerErrorMessage: String?,
    lastConsoleMessage: String?,
    errorLogText: String?,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimizeClick: () -> Unit,
    onFullScreenClick: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var doubleTapIndicatorRight by remember { mutableStateOf(false) }
    var doubleTapIndicatorLeft by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3500)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (controlsVisible) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x > size.width / 2
                        if (isRightSide) {
                            doubleTapIndicatorRight = true
                            youTubePlayer?.seekTo(progress + 10f)
                        } else {
                            doubleTapIndicatorLeft = true
                            youTubePlayer?.seekTo((progress - 10f).coerceAtLeast(0f))
                        }
                    }
                )
            }
    ) {
        if (doubleTapIndicatorLeft) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(650)
                doubleTapIndicatorLeft = false
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("«", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("10- ثوانٍ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (doubleTapIndicatorRight) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(650)
                doubleTapIndicatorRight = false
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("»", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("10+ ثوانٍ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && (playerErrorMessage == null && lastConsoleMessage == null),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMinimizeClick) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    IconButton(onClick = onFullScreenClick) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isPlaying) {
                            youTubePlayer?.pause()
                        } else {
                            youTubePlayer?.play()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedProgress = formatTime(progress)
                        val formattedDuration = formatTime(duration)
                        Text(
                            text = "$formattedProgress / $formattedDuration",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Slider(
                        value = if (duration > 0) progress else 0f,
                        onValueChange = { newTime ->
                            youTubePlayer?.seekTo(newTime)
                        },
                        valueRange = 0f..(if (duration > 0) duration else 100f),
                        colors = SliderDefaults.colors(
                            thumbColor = YoutubeRed,
                            activeTrackColor = YoutubeRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }
            }
        }

        // Diagnostics / Error Overlay (Shown over video if loading or playback fails)
        if (errorLogText != null || playerErrorMessage != null || lastConsoleMessage != null) {
            val finalErrorMessage = errorLogText ?: playerErrorMessage ?: lastConsoleMessage ?: ""
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(16.dp)
                    .clickable { /* Prevent passing taps to the player behind */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تشخيص أخطاء مشغل الفيديو (Diagnostics)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // The highly prominent Error Log TextView requested by user
                    // ID/Tag: id="@+id/errorLogTextView"
                    // Color: #FF0000 (bright red), Size: 16sp, Background: White/Legible high-contrast
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, Color(0xFFFF0000)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("errorLogTextView"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = finalErrorMessage,
                            color = Color(0xFFFF0000), // #FF0000 bright red
                            fontSize = 16.sp, // 16sp
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onErrorDismiss()
                                youTubePlayer?.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("إعادة المحاولة (Retry)", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                onErrorDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("إغلاق (Dismiss)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(seconds: Float): String {
    val totalSecs = seconds.toInt()
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}


@Composable
fun TopSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title Logo Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(YoutubeRed)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TubeLight",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }

            // Elegant Search TextField (Apple-inspired minimalist design)
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = "ابحث عن فيديوهات...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = StudioCard,
                    unfocusedContainerColor = StudioCard,
                    disabledContainerColor = StudioCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = YoutubeRed
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch(query)
                    keyboardController?.hide()
                }),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(BorderStroke(1.dp, BorderWhiteTranslucent), RoundedCornerShape(16.dp))
                    .testTag("search_text_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Settings Dialog Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCard)
                    .border(BorderStroke(1.dp, BorderWhiteTranslucent), RoundedCornerShape(12.dp))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "API Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Subtle minimalist line separating header from the feed (border-b border-white/5)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderWhiteTranslucent)
        )
    }
}

@Composable
fun CategoryRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) YoutubeRed else StudioCard)
                    .then(
                        if (!isSelected) {
                            Modifier.border(BorderStroke(1.dp, BorderWhiteTranslucent), RoundedCornerShape(16.dp))
                        } else Modifier
                    )
                    .clickable { onCategorySelect(category) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("category_$category")
            ) {
                Text(
                    text = when (category) {
                        "All" -> "الكل"
                        "Music" -> "موسيقى"
                        "Gaming" -> "ألعاب"
                        "Tech" -> "تقنية"
                        "Cooking" -> "طبخ"
                        "Sports" -> "رياضة"
                        "Comedy" -> "ترفيه"
                        "Education" -> "تعليم"
                        else -> category
                    },
                    color = if (isSelected) PureWhite else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun VideoListContent(
    uiState: YouTubeUiState,
    selectedVideo: YouTubeVideo?,
    onVideoSelect: (YouTubeVideo) -> Unit,
    onRetry: () -> Unit
) {
    when (uiState) {
        is YouTubeUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = YoutubeRed)
            }
        }
        is YouTubeUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Text(
                        text = uiState.message,
                        color = YoutubeRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة", color = PureWhite)
                    }
                }
            }
        }
        is YouTubeUiState.Success -> {
            val videos = uiState.videos
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val columnsCount = if (isLandscape) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos) { video ->
                    val isPlaying = selectedVideo?.id == video.id
                    VideoCardItem(
                        video = video,
                        isPlaying = isPlaying,
                        onClick = { onVideoSelect(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCardItem(
    video: YouTubeVideo,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_card_${video.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) AccentGrey else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Video Thumbnail (Styled with Clean Minimalism 16dp corners and white/5 border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioCard)
                    .border(BorderStroke(1.dp, BorderWhiteTranslucent), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Optional Play Overlay tag or indicators
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = YoutubeRed,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Video Metadata Title and Channel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = video.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.channelTitle,
                    color = YoutubeRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (video.viewCountString != null) {
                            "مشاهدة ${video.viewCountString} • ${video.publishedAt}"
                        } else {
                            video.publishedAt
                        },
                        color = MutedGrey,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeySettingsDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRestoreDefault: () -> Unit
) {
    var keyText by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات واجهة برمجة التطبيقات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "يستخدم هذا التطبيق YouTube Data API v3 لجلب الفيديوهات. إذا تعطل التطبيق أو انتهت كوتا المفتاح الحالي، يمكنك تعديله هنا.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("مفتاح YouTube API Key", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YoutubeRed,
                        unfocusedBorderColor = AccentGrey,
                        focusedLabelColor = YoutubeRed,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyText) },
                colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed)
            ) {
                Text("حفظ وتحديث", color = PureWhite)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onRestoreDefault,
                    colors = ButtonDefaults.textButtonColors(contentColor = YoutubeRed)
                ) {
                    Text("استعادة الافتراضي")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("إلغاء")
                }
            }
        },
        containerColor = StudioCard,
        shape = RoundedCornerShape(16.dp)
    )
}
