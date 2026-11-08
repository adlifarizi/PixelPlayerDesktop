package com.kayoi.pixelplayerdesktop.presentation.screens

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_shuffle
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.domain.DailyMixManager
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.ContainedLoadingIndicator
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.components.threeShapeSwitch
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

private fun formatTotalDuration(songs: List<Song>): String {
    val totalMs = songs.sumOf { it.duration }
    val totalSeconds = (totalMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        "${hours} hr ${remainingMinutes} min"
    } else {
        "${minutes} min"
    }
}

@Composable
fun DailyMixScreen(
    songs: List<Song>,
    favoriteSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    viewModel: PlayerViewModel,
    onBack: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dailyMixManager = remember { DailyMixManager() }
    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }
    val dailyMixSongs = remember(songs, favoriteIds) {
        dailyMixManager.generateDailyMix(songs, favoriteIds, limit = 30)
    }

    val colors = MaterialTheme.colorScheme
    val backgroundBrush = remember(colors.surface, colors.primary) {
        Brush.verticalGradient(
            colors = listOf(
                colors.primary.copy(alpha = 0.22f),
                colors.surface.copy(alpha = 0.5f),
                colors.surface
            ),
            endY = 1200f
        )
    }

    val lazyListState = rememberLazyListState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val screenWidth = maxWidth
        val isVeryCompact = screenWidth < 400.dp
        val isCompact = screenWidth < 560.dp
        val horizontalPadding = when {
            isVeryCompact -> 12.dp
            isCompact -> 16.dp
            else -> 24.dp
        }

        if (dailyMixSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "daily_mix_header") {
                    ExpressiveDailyMixHeader(
                        songs = dailyMixSongs,
                        scrollState = lazyListState,
                        isCompact = isCompact,
                        isVeryCompact = isVeryCompact,
                        horizontalPadding = horizontalPadding
                    )
                }

                item(key = "play_shuffle_buttons") {
                    val buttonHeight = when {
                        isVeryCompact -> 58.dp
                        isCompact -> 64.dp
                        else -> 72.dp
                    }
                    val buttonShapeCorner = when {
                        isVeryCompact -> 42.dp
                        isCompact -> 50.dp
                        else -> 60.dp
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isVeryCompact) 8.dp else 12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (dailyMixSongs.isNotEmpty()) {
                                    viewModel.playSongs(dailyMixSongs, 0)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(buttonHeight),
                            enabled = dailyMixSongs.isNotEmpty(),
                            shape = RoundedCornerShape(
                                topStart = buttonShapeCorner,
                                topEnd = 14.dp,
                                bottomStart = buttonShapeCorner,
                                bottomEnd = 14.dp
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                                contentDescription = "Play",
                                modifier = Modifier.size(if (isVeryCompact) 20.dp else 24.dp)
                            )
                            Spacer(Modifier.width(if (isVeryCompact) 4.dp else 8.dp))
                            Text(
                                text = "Play It",
                                style = if (isVeryCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (dailyMixSongs.isNotEmpty()) {
                                    viewModel.shuffleSongs(dailyMixSongs)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(buttonHeight),
                            enabled = dailyMixSongs.isNotEmpty(),
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = buttonShapeCorner,
                                bottomStart = 14.dp,
                                bottomEnd = buttonShapeCorner
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_shuffle),
                                contentDescription = "Shuffle",
                                modifier = Modifier.size(if (isVeryCompact) 18.dp else 22.dp)
                            )
                            Spacer(Modifier.width(if (isVeryCompact) 4.dp else 8.dp))
                            Text(
                                text = "Shuffle",
                                style = if (isVeryCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = dailyMixSongs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    val isCurrentPlaying = currentSong?.id == song.id
                    EnhancedSongListItem(
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                        song = song,
                        isCurrentSong = isCurrentPlaying,
                        isPlaying = isCurrentPlaying && isPlaying,
                        showAlbumArt = true,
                        showDuration = !isVeryCompact,
                        isFavorite = favoriteIds.contains(song.id),
                        onPlayNext = { viewModel.playSongNext(song) },
                        onAddToQueue = { viewModel.addSongToQueue(song) },
                        onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                        onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                        onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                        onClick = { viewModel.playSongs(dailyMixSongs, index) }
                    )
                }
            }

            ExpressiveScrollBar(
                listState = lazyListState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp, top = if (isVeryCompact) 60.dp else 80.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ExpressiveDailyMixHeader(
    songs: List<Song>,
    scrollState: LazyListState,
    isCompact: Boolean = false,
    isVeryCompact: Boolean = false,
    horizontalPadding: Dp = 24.dp
) {
    val albumArts = remember(songs) {
        songs.mapNotNull { it.albumArtUriString }.distinct().take(3)
    }
    val totalDuration = remember(songs) { songs.sumOf { it.duration } }

    val parallaxOffset by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) scrollState.firstVisibleItemScrollOffset * 0.45f else 0f
        }
    }

    val headerAlpha by remember {
        derivedStateOf {
            (1f - (scrollState.firstVisibleItemScrollOffset / 500f)).coerceIn(0f, 1f)
        }
    }

    val headerHeight = when {
        isVeryCompact -> 260.dp
        isCompact -> 295.dp
        else -> 340.dp
    }

    val titleFontSize = when {
        isVeryCompact -> 30.sp
        isCompact -> 36.sp
        else -> 44.sp
    }

    val centerSize = when {
        isVeryCompact -> 140.dp
        isCompact -> 175.dp
        else -> 220.dp
    }
    val sideSize = when {
        isVeryCompact -> 110.dp
        isCompact -> 140.dp
        else -> 180.dp
    }
    val overlapSpacing = when {
        isVeryCompact -> (-48).dp
        isCompact -> (-62).dp
        else -> (-80).dp
    }

    val titleStyle = rememberDailyMixTitleStyle(fontSize = titleFontSize)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clipToBounds()
            .graphicsLayer {
                translationY = parallaxOffset
                alpha = headerAlpha
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(overlapSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayArts = remember(albumArts) {
                    when {
                        albumArts.isEmpty() -> listOf(null, null, null)
                        albumArts.size == 1 -> listOf(albumArts[0], albumArts[0], albumArts[0])
                        albumArts.size == 2 -> listOf(albumArts[0], albumArts[1], albumArts[0])
                        else -> albumArts
                    }
                }
                displayArts.forEachIndexed { index, artUri ->
                    val size = if (index == 1) centerSize else sideSize
                    val rotation = when (index) {
                        0 -> -15f
                        1 -> 0f
                        2 -> 15f
                        else -> 0f
                    }
                    val shape = threeShapeSwitch(index, thirdShapeCornerRadius = if (isVeryCompact) 18.dp else 26.dp)

                    Box(
                        modifier = Modifier
                            .size(size)
                            .graphicsLayer { rotationZ = rotation }
                            .clip(shape)
                    ) {
                        AsyncArtworkImage(
                            artworkUri = artUri,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholderIcon = Res.drawable.ic_rounded_music_note,
                            placeholderIconSize = if (isVeryCompact) 32.dp else 48.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface
                        ),
                        startY = 0f,
                        endY = 900f
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Daily Mix",
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                val songCountText = if (songs.size == 1) "1 song" else "${songs.size} songs"
                val durationText = formatTotalDuration(songs)
                Text(
                    text = "$songCountText • $durationText",
                    style = if (isVeryCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberDailyMixTitleStyle(fontSize: TextUnit = 44.sp): TextStyle {
    return remember(fontSize) {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    "font/gflex_variable.ttf",
                    weight = FontWeight(760),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(436),
                        FontVariation.width(102f),
                        FontVariation.Setting("ROND", 100f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(760),
            fontSize = fontSize,
        )
    }
}
