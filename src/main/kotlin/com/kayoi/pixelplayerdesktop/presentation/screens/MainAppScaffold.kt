package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import com.kayoi.pixelplayerdesktop.presentation.components.AddToPlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.AppSidebar
import com.kayoi.pixelplayerdesktop.presentation.components.AppTopBar
import com.kayoi.pixelplayerdesktop.presentation.components.AppUpdateDialog
import com.kayoi.pixelplayerdesktop.presentation.components.BatchEditMetadataDialog
import com.kayoi.pixelplayerdesktop.presentation.components.DeleteSongConfirmationDialog
import com.kayoi.pixelplayerdesktop.presentation.components.DismissUndoBar
import com.kayoi.pixelplayerdesktop.presentation.components.EditSongMetadataDialog
import com.kayoi.pixelplayerdesktop.presentation.components.KeyboardShortcutsDialog
import com.kayoi.pixelplayerdesktop.presentation.components.MiniPlayerBar
import com.kayoi.pixelplayerdesktop.presentation.components.QueueSidePanel
import com.kayoi.pixelplayerdesktop.presentation.components.QuickAccessSettingsDialog
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopBackDispatcher
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopBackHandler
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavTarget
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavigator
import com.kayoi.pixelplayerdesktop.presentation.navigation.LocalBackDispatcher
import com.kayoi.pixelplayerdesktop.presentation.navigation.toDefaultTarget
import com.kayoi.pixelplayerdesktop.presentation.screens.LibrarySubDetail
import com.kayoi.pixelplayerdesktop.presentation.screens.library.AlbumsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.ArtistsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.FoldersTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.PlaylistsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.SongsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSection
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainAppScaffold(
    viewModel: PlayerViewModel,
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    onSearchFocusChange: (Boolean) -> Unit = {},
    isQueueSidePanelOpen: Boolean = true,
    onToggleQueueSidePanel: () -> Unit = {},
    onCloseQueueSidePanel: () -> Unit = onToggleQueueSidePanel,
    navigator: DesktopNavigator? = null,
    backDispatcher: DesktopBackDispatcher = LocalBackDispatcher.current,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showDismissUndoBar by remember { mutableStateOf(false) }

    val songs by viewModel.songs.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val filteredFavoriteSongs by viewModel.filteredFavoriteSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val filteredAlbums by viewModel.filteredAlbums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val filteredArtists by viewModel.filteredArtists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val homeStatsOverview by viewModel.homeStatsOverview.collectAsState()
    val musicDirectories by viewModel.musicDirectories.collectAsState()

    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
    val paletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val colorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()
    val navBarCompactMode by viewModel.navBarCompactMode.collectAsState()
    val defaultTabKey by viewModel.defaultTab.collectAsState()
    val showQueueHistory by viewModel.showQueueHistory.collectAsState()
    val storageFilter by viewModel.storageFilter.collectAsState()
    val showShortcutsDialog by viewModel.showShortcutsDialog.collectAsState()
    val quickAccessItems by viewModel.quickAccessItems.collectAsState()
    val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
    var showQuickAccessSettingsDialog by remember { mutableStateOf(false) }
    val showUpdateDialog = viewModel.updateStateHolder?.showUpdateDialog?.collectAsState()?.value ?: false
    val availableRelease = viewModel.updateStateHolder?.availableRelease?.collectAsState()?.value

    LaunchedEffect(Unit) {
        viewModel.updateStateHolder?.runStartupCheckIfNeeded()
    }

    val initialDestination = remember {
        DesktopNavDestination.entries.firstOrNull { it.name == viewModel.defaultTab.value } ?: DesktopNavDestination.HOME
    }
    val nav = navigator ?: remember { DesktopNavigator(initialDestination.toDefaultTarget()) }
    val currentTarget = nav.currentTarget
    val currentDestination = nav.currentDestination

    val songColorScheme by produceState<ColorScheme?>(
        initialValue = if (isDynamicColor && currentSong?.albumArtUriString != null) {
            DynamicColorProcessor.getCachedColorScheme(
                pathOrUri = currentSong?.albumArtUriString,
                paletteStyle = paletteStyle,
                colorAccuracyLevel = colorAccuracy
            )?.let { if (isDarkTheme) it.dark else it.light }
        } else null,
        currentSong?.albumArtUriString,
        isDarkTheme,
        isDynamicColor,
        paletteStyle,
        colorAccuracy
    ) {
        if (!isDynamicColor || currentSong?.albumArtUriString == null) {
            value = null
            return@produceState
        }
        val scheme = withContext(Dispatchers.Default) {
            val pair = DynamicColorProcessor.extractColorScheme(
                pathOrUri = currentSong?.albumArtUriString,
                paletteStyle = paletteStyle,
                colorAccuracyLevel = colorAccuracy
            )
            if (isDarkTheme) pair?.dark else pair?.light
        }
        value = scheme
    }

    var searchQuery by remember { mutableStateOf("") }
    var previousTargetBeforeSearch by remember { mutableStateOf<DesktopNavTarget?>(null) }

    val handleSearchQueryChange: (String) -> Unit = { newQuery ->
        if (newQuery.isNotBlank()) {
            if (searchQuery.isBlank()) {
                if (currentDestination == DesktopNavDestination.HOME) {
                    previousTargetBeforeSearch = currentTarget
                    nav.navigate(DesktopNavTarget.Songs)
                }
            }
        } else {
            if (previousTargetBeforeSearch != null) {
                nav.navigate(previousTargetBeforeSearch!!)
                previousTargetBeforeSearch = null
            }
        }
        searchQuery = newQuery
        viewModel.onSearchQueryChanged(newQuery)
    }

    var isExpandedPlayerOpen by remember { mutableStateOf(false) }
    var initialExpandedTab by remember { mutableStateOf(ExpandedTab.LYRICS) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songsToAddToPlaylist by remember { mutableStateOf<List<Song>?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var songToEditMetadata by remember { mutableStateOf<Song?>(null) }
    var batchSongsToEdit by remember { mutableStateOf<List<Song>?>(null) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.playbackErrorEvents.collect { errorMsg ->
            playbackErrorMessage = errorMsg
        }
    }

    DesktopBackHandler(enabled = batchSongsToEdit != null, dispatcher = backDispatcher) {
        batchSongsToEdit = null
        true
    }
    DesktopBackHandler(enabled = songToEditMetadata != null, dispatcher = backDispatcher) {
        songToEditMetadata = null
        true
    }
    DesktopBackHandler(enabled = songToDelete != null, dispatcher = backDispatcher) {
        songToDelete = null
        true
    }
    DesktopBackHandler(enabled = songToAddToPlaylist != null, dispatcher = backDispatcher) {
        songToAddToPlaylist = null
        true
    }
    DesktopBackHandler(enabled = showShortcutsDialog, dispatcher = backDispatcher) {
        viewModel.hideShortcutsDialog()
        true
    }
    DesktopBackHandler(enabled = isExpandedPlayerOpen, dispatcher = backDispatcher) {
        isExpandedPlayerOpen = false
        true
    }
    DesktopBackHandler(enabled = nav.canGoBack, dispatcher = backDispatcher) {
        nav.goBack()
    }
    DisposableEffect(nav, backDispatcher) {
        val reg = backDispatcher.registerForwardHandler {
            if (!isExpandedPlayerOpen && nav.canGoForward) {
                nav.goForward()
            } else {
                false
            }
        }
        onDispose { reg.close() }
    }

    val displayedSongs = if (currentDestination == DesktopNavDestination.LIKED) {
        filteredFavoriteSongs
    } else {
        filteredSongs
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isNarrowWindow = maxWidth < 800.dp
        val isQueueVisible = isQueueSidePanelOpen && (currentSong != null || queue.isNotEmpty())
        val isCompactQueueMode = isNarrowWindow && isQueueVisible
        val isSidebarCompact = navBarCompactMode

        LaunchedEffect(isNarrowWindow) {
            if (isNarrowWindow && isQueueSidePanelOpen) {
                onCloseQueueSidePanel()
            }
        }

        DesktopBackHandler(enabled = isCompactQueueMode, dispatcher = backDispatcher) {
            onCloseQueueSidePanel()
            true
        }

        Row(modifier = Modifier.fillMaxSize()) {
            AppSidebar(
                currentDestination = currentDestination,
                currentTarget = currentTarget,
                quickAccessItems = quickAccessItems,
                albums = albums,
                artists = artists,
                playlists = playlists,
                fetchDeezerImages = fetchDeezerImages,
                onNavigate = { destination ->
                    previousTargetBeforeSearch = null
                    nav.navigateToRoot(destination)
                },
                onNavigateToTarget = { target ->
                    previousTargetBeforeSearch = null
                    nav.navigate(target)
                },
                onUnpinQuickAccess = { id ->
                    viewModel.unpinFromQuickAccess(id)
                },
                onOpenQuickAccessSettings = {
                    showQuickAccessSettingsDialog = true
                },
                isCompact = isSidebarCompact
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.background)
            ) {
                AppTopBar(
                    currentDestination = currentDestination,
                    searchQuery = searchQuery,
                    onSearchQueryChange = handleSearchQueryChange,
                    showQueueToggle = true,
                    isQueueOpen = isQueueSidePanelOpen,
                    queueCount = queue.size,
                    onToggleQueue = onToggleQueueSidePanel,
                    searchFocusRequester = searchFocusRequester,
                    onSearchFocusChange = onSearchFocusChange,
                    canGoBack = nav.canGoBack || isExpandedPlayerOpen || isCompactQueueMode || songToAddToPlaylist != null || songToDelete != null || songToEditMetadata != null || batchSongsToEdit != null,
                    canGoForward = nav.canGoForward && !isExpandedPlayerOpen,
                    onGoBack = { backDispatcher.dispatchBack() },
                    onGoForward = { backDispatcher.dispatchForward() }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (currentDestination) {
                                DesktopNavDestination.HOME -> {
                                    when (currentTarget) {
                                        is DesktopNavTarget.DailyMix -> {
                                            DailyMixScreen(
                                                songs = songs,
                                                favoriteSongs = favoriteSongs,
                                                currentSong = currentSong,
                                                isPlaying = playerInfo.isPlaying,
                                                viewModel = viewModel,
                                                onBack = { nav.goBack() },
                                                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                                onDelete = { song -> songToDelete = song },
                                                onEditMetadata = { song -> songToEditMetadata = song }
                                            )
                                        }
                                        is DesktopNavTarget.RecentlyPlayed -> {
                                            RecentlyPlayedScreen(
                                                recentlyPlayedSongs = recentlyPlayedSongs,
                                                currentSong = currentSong,
                                                isPlaying = playerInfo.isPlaying,
                                                viewModel = viewModel,
                                                onBack = { nav.goBack() },
                                                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                                onDelete = { song -> songToDelete = song },
                                                onEditMetadata = { song -> songToEditMetadata = song }
                                            )
                                        }
                                        is DesktopNavTarget.RecentlyAdded -> {
                                            RecentlyAddedScreen(
                                                songs = songs,
                                                currentSong = currentSong,
                                                isPlaying = playerInfo.isPlaying,
                                                viewModel = viewModel,
                                                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                                onDelete = { song -> songToDelete = song },
                                                onEditMetadata = { song -> songToEditMetadata = song }
                                            )
                                        }
                                        is DesktopNavTarget.MostListened -> {
                                            MostListenedScreen(
                                                songs = songs,
                                                albums = albums,
                                                artists = artists,
                                                currentSong = currentSong,
                                                isPlaying = playerInfo.isPlaying,
                                                viewModel = viewModel,
                                                onSelectAlbum = { album -> nav.navigate(DesktopNavTarget.AlbumDetail(album)) },
                                                onSelectArtist = { artist -> nav.navigate(DesktopNavTarget.ArtistDetail(artist)) },
                                                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                                onAddSongsToPlaylist = { albumSongs -> songsToAddToPlaylist = albumSongs },
                                                onDelete = { song -> songToDelete = song },
                                                onEditMetadata = { song -> songToEditMetadata = song }
                                            )
                                        }
                                        is DesktopNavTarget.Stats -> {
                                            StatsScreen(
                                                viewModel = viewModel,
                                                songs = songs,
                                                onBack = { nav.goBack() }
                                            )
                                        }
                                        else -> {
                                            HomeScreen(
                                                songs = songs,
                                                favoriteSongs = favoriteSongs,
                                                recentlyPlayedSongs = recentlyPlayedSongs,
                                                homeStatsOverview = homeStatsOverview,
                                                currentSong = currentSong,
                                                isPlaying = playerInfo.isPlaying,
                                                onSongSelect = { song ->
                                                    val idx = songs.indexOfFirst { it.id == song.id }
                                                    if (idx != -1) {
                                                        viewModel.playSongs(songs.drop(idx), 0)
                                                    } else {
                                                        viewModel.playSong(song)
                                                    }
                                                },
                                                onPlaySongs = { songList, startIndex ->
                                                    viewModel.playSongs(songList, startIndex)
                                                },
                                                onShuffleAll = { viewModel.playSongs(songs.shuffled()) },
                                                onNavigateToLibrary = { nav.navigate(DesktopNavTarget.Songs) },
                                                onNavigateToDailyMix = { nav.navigate(DesktopNavTarget.DailyMix) },
                                                onNavigateToRecentlyPlayed = { nav.navigate(DesktopNavTarget.RecentlyPlayed) },
                                                onNavigateToStats = { nav.navigate(DesktopNavTarget.Stats) },
                                                onNavigateToFolders = {
                                                    nav.navigate(
                                                        DesktopNavTarget.Settings(
                                                            SettingsSection.LIBRARY,
                                                            LibrarySubDetail.FOLDERS
                                                        )
                                                    )
                                                },
                                                onRescanLibrary = { viewModel.rescanLibrary() },
                                                onPlayNext = { song -> viewModel.playSongNext(song) },
                                                onAddToQueue = { song -> viewModel.addSongToQueue(song) },
                                                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                                onDelete = { song -> songToDelete = song },
                                                onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                                                onEditMetadata = { song -> songToEditMetadata = song }
                                            )
                                        }
                                    }
                                }

                                DesktopNavDestination.SONGS, DesktopNavDestination.LIKED -> {
                                    SongsTab(
                                        filteredSongs = displayedSongs,
                                        currentSong = currentSong,
                                        isPlaying = playerInfo.isPlaying,
                                        searchQuery = searchQuery,
                                        viewModel = viewModel,
                                        isLikedTab = currentDestination == DesktopNavDestination.LIKED,
                                        onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                        onDelete = { song -> songToDelete = song },
                                        onEditMetadata = { song -> songToEditMetadata = song }
                                    )
                                }

                                DesktopNavDestination.ALBUMS -> {
                                    val selectedAlbum = (currentTarget as? DesktopNavTarget.AlbumDetail)?.album
                                    AlbumsTab(
                                        albums = albums,
                                        songs = songs,
                                        isDarkTheme = isDarkTheme,
                                        searchQuery = searchQuery,
                                        selectedAlbum = selectedAlbum,
                                        onSelectAlbum = { album ->
                                            if (album != null) {
                                                nav.navigate(DesktopNavTarget.AlbumDetail(album))
                                            } else {
                                                nav.goBack()
                                            }
                                        },
                                        viewModel = viewModel,
                                        filteredAlbums = filteredAlbums,
                                        onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                        onAddSongsToPlaylist = { albumSongs -> songsToAddToPlaylist = albumSongs },
                                        onNavigateToArtist = { artist -> nav.navigate(DesktopNavTarget.ArtistDetail(artist)) },
                                        onDelete = { song -> songToDelete = song },
                                        onEditMetadata = { song -> songToEditMetadata = song },
                                        onBatchEditMetadata = { batchSongs -> batchSongsToEdit = batchSongs }
                                    )
                                }

                                DesktopNavDestination.ARTISTS -> {
                                    val selectedArtist = (currentTarget as? DesktopNavTarget.ArtistDetail)?.artist
                                    ArtistsTab(
                                        artists = artists,
                                        songs = songs,
                                        searchQuery = searchQuery,
                                        selectedArtist = selectedArtist,
                                        onSelectArtist = { artist ->
                                            if (artist != null) {
                                                nav.navigate(DesktopNavTarget.ArtistDetail(artist))
                                            } else {
                                                nav.goBack()
                                            }
                                        },
                                        viewModel = viewModel,
                                        filteredArtists = filteredArtists,
                                        onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                        onAddSongsToPlaylist = { artistSongs -> songsToAddToPlaylist = artistSongs },
                                        onDelete = { song -> songToDelete = song },
                                        onEditMetadata = { song -> songToEditMetadata = song }
                                    )
                                }

                                DesktopNavDestination.PLAYLISTS -> {
                                    val selectedPlaylistId = (currentTarget as? DesktopNavTarget.PlaylistDetail)?.playlistId
                                    PlaylistsTab(
                                        playlists = playlists,
                                        songs = songs,
                                        searchQuery = searchQuery,
                                        selectedPlaylistId = selectedPlaylistId,
                                        onSelectPlaylist = { playlistId ->
                                            if (playlistId != null) {
                                                nav.navigate(DesktopNavTarget.PlaylistDetail(playlistId))
                                            } else {
                                                nav.goBack()
                                            }
                                        },
                                        viewModel = viewModel,
                                        onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                        onDelete = { song -> songToDelete = song },
                                        onEditMetadata = { song -> songToEditMetadata = song }
                                    )
                                }

                                DesktopNavDestination.FOLDERS -> {
                                    val selectedFolderDetail = (currentTarget as? DesktopNavTarget.FolderDetail)?.folder
                                    val currentFolderTreePath = (currentTarget as? DesktopNavTarget.Folders)?.treePath
                                    FoldersTab(
                                        songs = songs,
                                        musicDirectories = musicDirectories,
                                        currentSong = currentSong,
                                        isPlaying = playerInfo.isPlaying,
                                        favoriteSongs = favoriteSongs,
                                        searchQuery = searchQuery,
                                        selectedFolderDetail = selectedFolderDetail,
                                        onSelectFolderDetail = { folder ->
                                            if (folder != null) {
                                                nav.navigate(DesktopNavTarget.FolderDetail(folder))
                                            } else {
                                                nav.goBack()
                                            }
                                        },
                                        currentFolderTreePath = currentFolderTreePath,
                                        onFolderTreePathChange = { path -> nav.navigate(DesktopNavTarget.Folders(path)) },
                                        viewModel = viewModel,
                                        onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                        onDelete = { song -> songToDelete = song },
                                        onEditMetadata = { song -> songToEditMetadata = song },
                                        onBatchEditMetadata = { batchSongs -> batchSongsToEdit = batchSongs }
                                    )
                                }

                                DesktopNavDestination.SETTINGS -> {
                                    val settingsTarget = currentTarget as? DesktopNavTarget.Settings
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        isDarkTheme = isDarkTheme,
                                        isDynamicColor = isDynamicColor,
                                        onToggleDarkTheme = { viewModel.toggleDarkMode() },
                                        onToggleDynamicColor = { viewModel.toggleDynamicColor() },
                                        onRescanLibrary = { viewModel.rescanLibrary() },
                                        activeSection = settingsTarget?.section,
                                        activeSubDetail = settingsTarget?.subDetail ?: LibrarySubDetail.NONE,
                                        searchQuery = searchQuery,
                                        onClearSearch = { handleSearchQueryChange("") },
                                        onNavigate = { section, subDetail ->
                                            nav.navigate(DesktopNavTarget.Settings(section, subDetail))
                                        },
                                        onBack = { nav.goBack() }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isQueueVisible && !isCompactQueueMode,
                            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(tween(250)),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(tween(200))
                        ) {
                            QueueSidePanel(
                                queue = queue,
                                currentSong = currentSong,
                                currentQueueIndex = currentQueueIndex,
                                isPlaying = playerInfo.isPlaying,
                                isShuffleOn = playerInfo.isShuffleEnabled,
                                repeatMode = playerInfo.repeatMode,
                                showQueueHistory = showQueueHistory,
                                onSongClick = { song, index ->
                                    viewModel.playSongAtIndex(index)
                                },
                                onToggleShuffle = {
                                    viewModel.toggleShuffle()
                                },
                                onCycleRepeat = {
                                    viewModel.cycleRepeatMode()
                                },
                                onClearQueue = {
                                    viewModel.clearQueue()
                                },
                                onMoveItem = { from, to ->
                                    viewModel.moveQueueItem(from, to)
                                },
                                onRemoveItem = { index ->
                                    viewModel.removeFromQueue(index)
                                },
                                onPlayNext = { song ->
                                    viewModel.playSongNext(song)
                                },
                                onAddToPlaylist = { song ->
                                    songToAddToPlaylist = song
                                },
                                onDelete = { song ->
                                    songToDelete = song
                                },
                                onToggleFavorite = { song ->
                                    viewModel.toggleFavorite(song)
                                },
                                onEditMetadata = { song ->
                                    songToEditMetadata = song
                                },
                                favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCompactQueueMode,
                        enter = slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(200)),
                        exit = slideOutHorizontally(animationSpec = tween(260)) { it } + fadeOut(tween(180))
                    ) {
                        QueueSidePanel(
                            queue = queue,
                            currentSong = currentSong,
                            currentQueueIndex = currentQueueIndex,
                            isPlaying = playerInfo.isPlaying,
                            isShuffleOn = playerInfo.isShuffleEnabled,
                            repeatMode = playerInfo.repeatMode,
                            showQueueHistory = showQueueHistory,
                            onSongClick = { song, index ->
                                viewModel.playSongAtIndex(index)
                            },
                            onToggleShuffle = {
                                viewModel.toggleShuffle()
                            },
                            onCycleRepeat = {
                                viewModel.cycleRepeatMode()
                            },
                            onClearQueue = {
                                viewModel.clearQueue()
                            },
                            onMoveItem = { from, to ->
                                viewModel.moveQueueItem(from, to)
                            },
                            onRemoveItem = { index ->
                                viewModel.removeFromQueue(index)
                            },
                            onPlayNext = { song ->
                                viewModel.playSongNext(song)
                            },
                            onAddToPlaylist = { song ->
                                songToAddToPlaylist = song
                            },
                            onDelete = { song ->
                                songToDelete = song
                            },
                            onToggleFavorite = { song ->
                                viewModel.toggleFavorite(song)
                            },
                            onEditMetadata = { song ->
                                songToEditMetadata = song
                            },
                            favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() },
                            isFullPanel = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.background)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = playbackErrorMessage != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp)
                ) {
                    DismissUndoBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        message = playbackErrorMessage ?: "",
                        actionLabel = "Retry",
                        onUndo = {
                            playbackErrorMessage = null
                            viewModel.togglePlayPause()
                        },
                        onClose = {
                            playbackErrorMessage = null
                        },
                        durationMillis = 6000L
                    )
                }

                if (currentSong != null) {
                    val activePlayerColorScheme = songColorScheme ?: MaterialTheme.colorScheme
                    PixelPlayerTheme(
                        darkTheme = isDarkTheme,
                        colorSchemeOverride = activePlayerColorScheme
                    ) {
                        MiniPlayerBar(
                            currentSong = currentSong!!,
                            isPlaying = playerInfo.isPlaying,
                            isBuffering = playerInfo.isBuffering,
                            totalDurationMs = playerInfo.totalDurationMs,
                            currentPositionFlow = viewModel.currentPositionMs,
                            isShuffle = playerInfo.isShuffleEnabled,
                            repeatMode = playerInfo.repeatMode,
                            isFavorite = playerInfo.isFavorite,
                            volume = playerInfo.volume,
                            isMuted = playerInfo.isMuted,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onPreviousClick = { viewModel.playPrevious() },
                            onNextClick = { viewModel.playNext() },
                            onSeek = { fraction ->
                                val targetMs = (fraction * playerInfo.totalDurationMs).toLong()
                                viewModel.seekTo(targetMs)
                            },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            onExpandClick = { isExpandedPlayerOpen = true },
                            onQueueClick = onToggleQueueSidePanel,
                            onMiniPlayerClick = { viewModel.setMiniPlayerMode(true) },
                            onCloseClick = {
                                viewModel.stopPlayback()
                                showDismissUndoBar = true
                            }
                        )
                    }
                } else if (showDismissUndoBar) {
                    AnimatedVisibility(
                        visible = showDismissUndoBar,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        DismissUndoBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            message = "Playlist dismissed",
                            onUndo = {
                                showDismissUndoBar = false
                                viewModel.undoStopPlayback()
                            },
                            onClose = {
                                showDismissUndoBar = false
                            },
                            durationMillis = 4500L
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpandedPlayerOpen && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380)) + fadeIn(tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(250))
        ) {
            ExpandedPlayerScreen(
                viewModel = viewModel,
                initialTab = initialExpandedTab,
                onClose = { isExpandedPlayerOpen = false },
                onEditMetadata = { song -> songToEditMetadata = song },
                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                onDelete = { song -> songToDelete = song },
                onNavigateToArtist = { artist ->
                    isExpandedPlayerOpen = false
                    nav.navigate(DesktopNavTarget.ArtistDetail(artist))
                },
                onNavigateToAlbum = { album ->
                    isExpandedPlayerOpen = false
                    nav.navigate(DesktopNavTarget.AlbumDetail(album))
                }
            )
        }

        if (songToAddToPlaylist != null) {
            AddToPlaylistDialog(
                song = songToAddToPlaylist!!,
                playlists = playlists,
                onDismiss = { songToAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    viewModel.addSongToPlaylist(playlist.id, songToAddToPlaylist!!.id)
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name, listOf(songToAddToPlaylist!!.id))
                }
            )
        }

        if (songsToAddToPlaylist != null) {
            AddToPlaylistDialog(
                songs = songsToAddToPlaylist!!,
                playlists = playlists,
                onDismiss = { songsToAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    viewModel.addSongsToPlaylist(playlist.id, songsToAddToPlaylist!!.map { it.id })
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name, songsToAddToPlaylist!!.map { it.id })
                }
            )
        }

        if (songToDelete != null) {
            DeleteSongConfirmationDialog(
                song = songToDelete!!,
                onDismiss = { songToDelete = null },
                onConfirm = {
                    viewModel.deleteSong(songToDelete!!)
                }
            )
        }

        if (songToEditMetadata != null) {
            EditSongMetadataDialog(
                song = songToEditMetadata!!,
                viewModel = viewModel,
                onDismiss = { songToEditMetadata = null }
            )
        }

        if (batchSongsToEdit != null) {
            BatchEditMetadataDialog(
                songs = batchSongsToEdit!!,
                viewModel = viewModel,
                onDismiss = { batchSongsToEdit = null }
            )
        }

        if (showShortcutsDialog) {
            KeyboardShortcutsDialog(
                onDismiss = { viewModel.hideShortcutsDialog() }
            )
        }

        val updateHolder = viewModel.updateStateHolder
        if (showUpdateDialog && availableRelease != null) {
            updateHolder.let { holder ->
                AppUpdateDialog(
                    release = availableRelease,
                    onDismiss = { skipThisVersion ->
                        holder.dismissUpdateDialog(skipThisVersion)
                    },
                    onOpenRelease = { url ->
                        holder.openReleasePage(url)
                    }
                )
            }
        }

        if (showQuickAccessSettingsDialog) {
            QuickAccessSettingsDialog(
                items = quickAccessItems,
                playlists = playlists,
                albums = albums,
                artists = artists,
                onDismiss = { showQuickAccessSettingsDialog = false },
                onSave = { updatedItems ->
                    viewModel.setQuickAccessItems(updatedItems)
                }
            )
        }
    }
}
