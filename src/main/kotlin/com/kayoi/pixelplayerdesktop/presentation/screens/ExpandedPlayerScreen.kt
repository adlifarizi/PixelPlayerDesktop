package com.kayoi.pixelplayerdesktop.presentation.screens

import com.kayoi.pixelplayerdesktop.generated.resources.ic_lyrics
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_graphic_eq
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_keyboard
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_keyboard_arrow_down
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_more_vert
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_search
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speed
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_timer
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speaker
import org.jetbrains.compose.resources.painterResource
import java.io.File

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AudioDeviceSelectorDialog
import com.kayoi.pixelplayerdesktop.presentation.components.LyricsSearchDialog
import com.kayoi.pixelplayerdesktop.presentation.components.PlaybackSpeedDialog
import com.kayoi.pixelplayerdesktop.presentation.components.QueueSidePanel
import com.kayoi.pixelplayerdesktop.presentation.components.QueueToggleButton
import com.kayoi.pixelplayerdesktop.presentation.components.SaveLyricsDialog
import com.kayoi.pixelplayerdesktop.presentation.components.SleepTimerDialog
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LyricsOptionsMenu
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LyricsSyncControls
import com.kayoi.pixelplayerdesktop.presentation.screens.player.DesktopLandscapeHeroPlayer
import com.kayoi.pixelplayerdesktop.presentation.screens.player.LyricsView
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.EqualizerDialog
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker
import com.kayoi.pixelplayerdesktop.utils.MultiLangRomanizer

private val outerCorner = 26.dp
private val innerCorner = 4.dp
private val buttonGap = 4.dp
private val buttonSize = 42.dp

enum class ExpandedTab {
    LYRICS,
    QUEUE
}

@Composable
fun ExpandedPlayerScreen(
    viewModel: PlayerViewModel,
    initialTab: ExpandedTab = ExpandedTab.LYRICS,
    onClose: () -> Unit,
    onEditMetadata: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onNavigateToArtist: ((Artist) -> Unit)? = null,
    onNavigateToAlbum: ((Album) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val allArtists by viewModel.artists.collectAsState()
    val allAlbums by viewModel.albums.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
    val paletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val colorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()
    val showPlayerFileInfo by viewModel.showPlayerFileInfo.collectAsState()
    val lyricsAlignment by viewModel.lyricsAlignment.collectAsState()
    val lyricsTextSize by viewModel.lyricsTextSize.collectAsState()
    val showLyricsRomanization by viewModel.showLyricsRomanization.collectAsState()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val selectedAudioDevice by viewModel.selectedAudioDevice.collectAsState()
    val availableAudioDevices by viewModel.availableAudioDevices.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val isCurrentSongSpeedSaved by viewModel.isCurrentSongSpeedSaved.collectAsState()
    val isEndOfTrackActive by viewModel.isEndOfTrackTimerActive.collectAsState()
    val activeDurationMinutes by viewModel.activeTimerDurationMinutes.collectAsState()
    val sleepTimerPlayCount by viewModel.sleepTimerPlayCount.collectAsState()
    val isSleepTimerActive = isEndOfTrackActive || activeDurationMinutes != null || sleepTimerPlayCount > 1f
    val coroutineScope = rememberCoroutineScope()
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showAudioDeviceDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showLyricsSearchDialog by remember { mutableStateOf(false) }
    var showSaveLyricsDialog by remember { mutableStateOf(false) }
    var showLyricsMenu by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showLyricsSyncControls by remember { mutableStateOf(false) }
    var lyricsStatusMessage by remember { mutableStateOf<String?>(null) }
    var isQueueExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(lyricsStatusMessage) {
        if (lyricsStatusMessage != null) {
            delay(2800)
            lyricsStatusMessage = null
        }
    }

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

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentSong?.id, queue.size) {
        focusRequester.requestFocus()
    }

    if (currentSong == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val activePlayerColorScheme = songColorScheme ?: MaterialTheme.colorScheme
    PixelPlayerTheme(
        darkTheme = isDarkTheme,
        colorSchemeOverride = activePlayerColorScheme
    ) {
        val colors = MaterialTheme.colorScheme
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
        val isCompactLayout = screenWidth < 800.dp
        val isFullPanelQueue = isCompactLayout && isQueueExpanded

        // On compact widths the queue panel starts collapsed so the player and
        // lyrics keep the whole content area.
        LaunchedEffect(isCompactLayout) {
            if (isCompactLayout) {
                isQueueExpanded = false
            }
        }

        Surface(
            modifier = modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() },
            color = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTooltip(
                        text = "Collapse Player (Esc)",
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset(0.dp, 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHighest)
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_keyboard_arrow_down),
                                contentDescription = "Collapse Player (Esc)",
                                tint = colors.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val equalizerContainerColor = if (equalizerEnabled) colors.primary else colors.surfaceContainerHighest
                        val equalizerContentColor = if (equalizerEnabled) colors.onPrimary else colors.onSurface

                        AppTooltip(
                            text = "Equalizer",
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset(0.dp, 6.dp)
                        ) {
                            FilledIconButton(
                                onClick = { showEqualizerDialog = true },
                                shape = RoundedCornerShape(
                                    topStart = outerCorner,
                                    bottomStart = outerCorner,
                                    topEnd = innerCorner,
                                    bottomEnd = innerCorner
                                ),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = equalizerContainerColor,
                                    contentColor = equalizerContentColor
                                ),
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_graphic_eq),
                                    contentDescription = "Equalizer",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(buttonGap))

                        AppTooltip(
                            text = "Audio Output Device",
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset(0.dp, 6.dp)
                        ) {
                            FilledIconButton(
                                onClick = { showAudioDeviceDialog = true },
                                shape = RoundedCornerShape(
                                    topStart = innerCorner,
                                    bottomStart = innerCorner,
                                    topEnd = innerCorner,
                                    bottomEnd = innerCorner
                                ),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.surfaceContainerHighest,
                                    contentColor = colors.onSurface
                                ),
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_speaker),
                                    contentDescription = "Audio Output Device",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(buttonGap))

                        val speedContainerColor = if (playbackSpeed != 1.0f) colors.primary else colors.surfaceContainerHighest
                        val speedContentColor = if (playbackSpeed != 1.0f) colors.onPrimary else colors.onSurface

                        AppTooltip(
                            text = if (playbackSpeed != 1.0f) "Speed: ${playbackSpeed}x" else "Playback Speed",
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset(0.dp, 6.dp)
                        ) {
                            FilledIconButton(
                                onClick = { showPlaybackSpeedDialog = true },
                                shape = RoundedCornerShape(
                                    topStart = innerCorner,
                                    bottomStart = innerCorner,
                                    topEnd = innerCorner,
                                    bottomEnd = innerCorner
                                ),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = speedContainerColor,
                                    contentColor = speedContentColor
                                ),
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_speed),
                                    contentDescription = "Playback Speed",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(buttonGap))

                        val sleepTimerContainerColor = if (isSleepTimerActive) colors.primary else colors.surfaceContainerHighest
                        val sleepTimerContentColor = if (isSleepTimerActive) colors.onPrimary else colors.onSurface

                        AppTooltip(
                            text = if (isSleepTimerActive) "Sleep Timer: Active" else "Sleep Timer",
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset(0.dp, 6.dp)
                        ) {
                            FilledIconButton(
                                onClick = { showSleepTimerDialog = true },
                                shape = if (isCompactLayout) {
                                    RoundedCornerShape(
                                        topStart = innerCorner,
                                        bottomStart = innerCorner,
                                        topEnd = innerCorner,
                                        bottomEnd = innerCorner
                                    )
                                } else {
                                    RoundedCornerShape(
                                        topStart = innerCorner,
                                        bottomStart = innerCorner,
                                        topEnd = outerCorner,
                                        bottomEnd = outerCorner
                                    )
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = sleepTimerContainerColor,
                                    contentColor = sleepTimerContentColor
                                ),
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_timer),
                                    contentDescription = "Sleep Timer",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isCompactLayout) {
                            Spacer(modifier = Modifier.width(buttonGap))
                        }

                        QueueToggleButton(
                            visible = isCompactLayout,
                            isQueueOpen = isQueueExpanded,
                            queueCount = queue.size,
                            onClick = { isQueueExpanded = !isQueueExpanded },
                            showLabel = !isCompactLayout,
                            shape = RoundedCornerShape(
                                topStart = innerCorner,
                                bottomStart = innerCorner,
                                topEnd = outerCorner,
                                bottomEnd = outerCorner
                            ),
                            height = buttonSize,
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = colors.onSurface
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .clipToBounds()
                ) {
                    val queueWidth = if (screenWidth < 960.dp) 320.dp else 360.dp
                    val paneGap = if (screenWidth < 900.dp) 12.dp else 20.dp
                    val heroRadius = if (screenWidth < 900.dp) 22.dp else 28.dp
                    val lyricsPaddingH = if (screenWidth < 900.dp) 16.dp else 22.dp
                    val lyricsPaddingV = if (screenWidth < 900.dp) 10.dp else 14.dp

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(paneGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(paneGap)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(heroRadius),
                                color = colors.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                DesktopLandscapeHeroPlayer(
                                    song = currentSong!!,
                                    isPlaying = playerInfo.isPlaying,
                                    isBuffering = playerInfo.isBuffering,
                                    totalDurationMs = playerInfo.totalDurationMs,
                                    currentPositionFlow = viewModel.currentPositionMs,
                                    controlActionFlow = viewModel.controlActionEvent,
                                    isShuffle = playerInfo.isShuffleEnabled,
                                    repeatMode = playerInfo.repeatMode,
                                    isFavorite = playerInfo.isFavorite,
                                    volume = playerInfo.volume,
                                    isMuted = playerInfo.isMuted,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onPrev = { viewModel.playPrevious() },
                                    onNext = { viewModel.playNext() },
                                    onSeek = { fraction ->
                                        val targetMs = (fraction * playerInfo.totalDurationMs).toLong()
                                        viewModel.seekTo(targetMs)
                                    },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                                    onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                                    onVolumeChange = { viewModel.setVolume(it) },
                                    onToggleMute = { viewModel.toggleMute() },
                                    onArtistClick = if (onNavigateToArtist != null) {
                                        { artistRef ->
                                            val targetArtist = allArtists.firstOrNull { it.id == artistRef.id || it.name.equals(artistRef.name, ignoreCase = true) }
                                                ?: Artist(id = artistRef.id, name = artistRef.name)
                                            onClose()
                                            onNavigateToArtist(targetArtist)
                                        }
                                    } else null,
                                    onAlbumClick = if (onNavigateToAlbum != null) {
                                        {
                                            val song = currentSong
                                            if (song != null) {
                                                val targetAlbum = allAlbums.firstOrNull { it.id == song.albumId || it.title.equals(song.album, ignoreCase = true) }
                                                    ?: Album(
                                                        id = song.albumId,
                                                        title = song.album.ifBlank { "Unknown Album" },
                                                        artist = song.artist,
                                                        albumArtUriString = song.albumArtUriString
                                                    )
                                                onClose()
                                                onNavigateToAlbum(targetAlbum)
                                            }
                                        }
                                    } else null,
                                    showPlayerFileInfo = showPlayerFileInfo,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                color = colors.surfaceContainerLow,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = lyricsPaddingH,
                                                vertical = lyricsPaddingV
                                            ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_lyrics),
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "Lyrics",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = colors.onSurface
                                            )

                                            AnimatedVisibility(
                                                visible = lyricsStatusMessage != null,
                                                enter = fadeIn() + expandHorizontally(),
                                                exit = fadeOut() + shrinkHorizontally()
                                            ) {
                                                Surface(
                                                    color = colors.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = lyricsStatusMessage.orEmpty(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            AppTooltip("Search / Change Lyrics") {
                                                IconButton(
                                                    onClick = { showLyricsSearchDialog = true },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.ic_rounded_search),
                                                        contentDescription = "Search Lyrics",
                                                        tint = colors.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Box {
                                                AppTooltip("Lyrics Options") {
                                                    IconButton(
                                                        onClick = { showLyricsMenu = true },
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(Res.drawable.ic_rounded_more_vert),
                                                            contentDescription = "Lyrics Options",
                                                            tint = colors.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                val hasSyncedLyrics = !playerInfo.lyrics?.synced.isNullOrEmpty()
                                                val hasLyrics = playerInfo.lyrics != null &&
                                                    (hasSyncedLyrics || !playerInfo.lyrics!!.plain.isNullOrEmpty())
                                                val hasRomanizedLyrics = remember(playerInfo.lyrics) {
                                                    val synced = playerInfo.lyrics?.synced?.any { line ->
                                                        !line.romanization.isNullOrBlank() || MultiLangRomanizer.isScriptThatNeedsRomanization(line.line)
                                                    } == true
                                                    val plain = playerInfo.lyrics?.plain?.any { line ->
                                                        MultiLangRomanizer.isScriptThatNeedsRomanization(line.substringBefore("\n"))
                                                    } == true
                                                    synced || plain
                                                }

                                                LyricsOptionsMenu(
                                                    expanded = showLyricsMenu,
                                                    onDismissRequest = { showLyricsMenu = false },
                                                    currentAlignment = lyricsAlignment,
                                                    onAlignmentChange = { viewModel.setLyricsAlignment(it) },
                                                    currentTextSize = lyricsTextSize,
                                                    onTextSizeChange = { viewModel.setLyricsTextSize(it) },
                                                    hasLyrics = hasLyrics,
                                                    hasSyncedLyrics = hasSyncedLyrics,
                                                    hasRomanizedLyrics = hasRomanizedLyrics,
                                                    showRomanization = showLyricsRomanization,
                                                    onToggleRomanization = { viewModel.setShowLyricsRomanization(!showLyricsRomanization) },
                                                    showSyncControls = showLyricsSyncControls,
                                                    onToggleSyncControls = { showLyricsSyncControls = !showLyricsSyncControls },
                                                    onSaveLyrics = {
                                                        showSaveLyricsDialog = true
                                                    },
                                                    onResetLyrics = {
                                                        showResetConfirmDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = colors.outlineVariant.copy(alpha = 0.25f)
                                    )

                                    val hasSyncedLyrics = !playerInfo.lyrics?.synced.isNullOrEmpty()
                                    AnimatedVisibility(
                                        visible = showLyricsSyncControls && hasSyncedLyrics,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LyricsSyncControls(
                                                offsetMillis = playerInfo.lyricsSyncOffsetMs,
                                                onOffsetChange = { viewModel.setLyricsSyncOffset(it) },
                                                onClose = { showLyricsSyncControls = false },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .widthIn(max = 520.dp)
                                            )
                                        }
                                    }

                                    LyricsView(
                                        lyrics = playerInfo.lyrics,
                                        isLoading = playerInfo.isLoadingLyrics,
                                        currentPositionFlow = viewModel.currentPositionMs,
                                        isPlaying = playerInfo.isPlaying,
                                        alignment = lyricsAlignment,
                                        textSize = lyricsTextSize,
                                        syncOffsetMs = playerInfo.lyricsSyncOffsetMs,
                                        showRomanization = showLyricsRomanization,
                                        onSeekToMs = { viewModel.seekTo(it) },
                                        onSearchLyrics = { showLyricsSearchDialog = true }
                                    )
                                }
                            }
                        }

                        if (!isCompactLayout) {
                            Surface(
                                modifier = Modifier
                                    .width(queueWidth)
                                    .fillMaxHeight(),
                                color = colors.surfaceContainerLow,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                QueueSidePanel(
                                    queue = queue,
                                    currentSong = currentSong,
                                    currentQueueIndex = currentQueueIndex,
                                    isPlaying = playerInfo.isPlaying,
                                    isShuffleOn = playerInfo.isShuffleEnabled,
                                    repeatMode = playerInfo.repeatMode,
                                    onSongClick = { _, index ->
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
                                    onAddToPlaylist = onAddToPlaylist,
                                    onDelete = onDelete,
                                    onEditMetadata = onEditMetadata,
                                    onToggleFavorite = { song ->
                                        viewModel.toggleFavorite(song)
                                    },
                                    favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() },
                                    itemContainerColor = colors.surfaceContainer,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isFullPanelQueue,
                        enter = slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(200)),
                        exit = slideOutHorizontally(animationSpec = tween(260)) { it } + fadeOut(tween(180)),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surface)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = colors.surfaceContainerLow,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            QueueSidePanel(
                                queue = queue,
                                currentSong = currentSong,
                                currentQueueIndex = currentQueueIndex,
                                isPlaying = playerInfo.isPlaying,
                                isShuffleOn = playerInfo.isShuffleEnabled,
                                repeatMode = playerInfo.repeatMode,
                                onSongClick = { _, index ->
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
                                onAddToPlaylist = onAddToPlaylist,
                                onDelete = onDelete,
                                onEditMetadata = onEditMetadata,
                                onToggleFavorite = { song ->
                                    viewModel.toggleFavorite(song)
                                },
                                favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() },
                                isFullPanel = true,
                                itemContainerColor = colors.surfaceContainer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        if (showLyricsSearchDialog) {
            val lyricsSearchUiState by viewModel.lyricsSearchUiState.collectAsState()
            LyricsSearchDialog(
                currentSong = currentSong,
                uiState = lyricsSearchUiState,
                onSearch = { viewModel.searchLyrics(it) },
                onSelectLyrics = {
                    viewModel.applyLyrics(it)
                    showLyricsSearchDialog = false
                },
                onImportFile = {
                    viewModel.importLyricsFromFile(it)
                    showLyricsSearchDialog = false
                },
                onDismiss = {
                    showLyricsSearchDialog = false
                    viewModel.resetLyricsSearchState()
                }
            )
        }

        if (showSaveLyricsDialog && currentSong != null) {
            SaveLyricsDialog(
                song = currentSong!!,
                onSaveToSongFolder = {
                    showSaveLyricsDialog = false
                    val saved = viewModel.saveLyricsBesideCurrentSong()
                    lyricsStatusMessage = if (saved != null) "Saved ${saved.name}" else "Failed to save lyrics"
                },
                onExportFile = {
                    showSaveLyricsDialog = false
                    val current = currentSong
                    val defaultName = current?.let { File(it.path).nameWithoutExtension } ?: "lyrics"
                    val initialDir = current?.let { File(it.path).parentFile }
                    DirectoryPicker.saveLrcFile(
                        defaultFileName = defaultName,
                        initialDir = initialDir
                    ) { file ->
                        val ok = viewModel.exportLyricsToFile(file)
                        lyricsStatusMessage = if (ok) "Exported ${file.name}" else "Failed to export lyrics"
                    }
                },
                onEmbedToAudioFile = {
                    showSaveLyricsDialog = false
                    coroutineScope.launch {
                        val ok = viewModel.saveLyricsToEmbeddedMetadata()
                        lyricsStatusMessage = if (ok) "Embedded lyrics into audio file" else "Failed to embed lyrics"
                    }
                },
                onDismiss = {
                    showSaveLyricsDialog = false
                }
            )
        }

        if (showResetConfirmDialog) {
            TrackDialogInput()
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Reset Lyrics?") },
                text = { Text("This will clear custom or cached lyrics for this song and reload original embedded or local lyrics.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirmDialog = false
                            viewModel.resetCurrentSongLyrics()
                            lyricsStatusMessage = "Lyrics reset to default"
                        }
                    ) {
                        Text("Reset", color = colors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEqualizerDialog) {
            EqualizerDialog(
                viewModel = viewModel,
                onDismiss = { showEqualizerDialog = false }
            )
        }

        if (showAudioDeviceDialog) {
            AudioDeviceSelectorDialog(
                selectedDevice = selectedAudioDevice,
                availableDevices = availableAudioDevices,
                onSelectDevice = { viewModel.setSelectedAudioDevice(it) },
                onRefreshDevices = { viewModel.refreshAudioDevices() },
                onDismiss = { showAudioDeviceDialog = false }
            )
        }

        if (showPlaybackSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                isSpeedSavedForThisSong = isCurrentSongSpeedSaved,
                onSpeedChange = { viewModel.setPlaybackSpeed(it, persist = false) },
                onSpeedChangeFinished = { viewModel.setPlaybackSpeed(it, persist = true) },
                onResetSpeed = { viewModel.resetPlaybackSpeed() },
                onRememberSpeedToggle = { viewModel.setRememberSpeedForCurrentSong(it) },
                onDismiss = { showPlaybackSpeedDialog = false }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                viewModel = viewModel,
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}
