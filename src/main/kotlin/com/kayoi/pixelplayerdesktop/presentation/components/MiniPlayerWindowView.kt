package com.kayoi.pixelplayerdesktop.presentation.components

import kotlin.math.roundToLong

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_open_in_new
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite_border
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_pause
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_push_pin
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat_one
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_shuffle
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_skip_next
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_skip_previous
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.LocalPixelPlayerDarkTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun MiniPlayerProgressBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorScheme = MaterialTheme.colorScheme
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = (if (isDragging) dragFraction else value).coerceIn(0f, 1f)
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.5.dp,
        label = "miniPlayerThumbRadius"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(fraction)
                    onValueChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onValueChange(fraction)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onValueChange(fraction)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        val centerY = size.height / 2f
        val trackHeightPx = 4.dp.toPx()
        val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

        drawRoundRect(
            color = colors.onPrimaryContainer.copy(alpha = 0.20f),
            topLeft = Offset(0f, centerY - trackHeightPx / 2f),
            size = Size(size.width, trackHeightPx),
            cornerRadius = cornerRadius
        )

        val activeWidth = size.width * displayFraction
        if (activeWidth > 0f) {
            drawRoundRect(
                color = colors.primary,
                topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                size = Size(activeWidth, trackHeightPx),
                cornerRadius = cornerRadius
            )
        }

        val thumbXPx = (size.width * displayFraction).coerceIn(0f, size.width)
        drawCircle(
            color = colors.primary,
            radius = thumbRadius.toPx(),
            center = Offset(thumbXPx, centerY)
        )
    }
}

@Composable
fun MiniPlayerWindowView(
    viewModel: PlayerViewModel,
    isAlwaysOnTop: Boolean,
    onToggleAlwaysOnTop: () -> Unit,
    onRestoreMainWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
    val paletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val colorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()
    val isDarkTheme = LocalPixelPlayerDarkTheme.current

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

    val colors = songColorScheme ?: MaterialTheme.colorScheme
    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()

    val isFavorite = remember(favoriteSongs, currentSong) {
        currentSong != null && favoriteSongs.any { it.id == currentSong?.id }
    }

    val totalDurationMs = playerInfo.totalDurationMs.coerceAtLeast(1L)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDraggingSlider) {
        dragSliderFraction
    } else {
        (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
    }

    val displayPositionMs = if (isDraggingSlider) {
        (dragSliderFraction * totalDurationMs).roundToLong()
    } else {
        currentPositionMs
    }

    val playPauseCorner by animateDpAsState(
        targetValue = if (playerInfo.isPlaying) 10.dp else 20.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "MiniPlayerPlayPauseCorner"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncArtworkImage(
                    artworkUri = currentSong?.albumArtUriString,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholderIcon = Res.drawable.ic_rounded_music_note,
                    placeholderIconSize = 22.dp,
                    containerColor = colors.onPrimaryContainer.copy(alpha = 0.12f),
                    contentColor = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    contentDescription = currentSong?.title ?: "No artwork"
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentSong?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentSong?.artist ?: "PixelPlayer",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                AppTooltip(if (isFavorite) "Remove Favorite" else "Add Favorite") {
                    IconButton(
                        onClick = { viewModel.toggleFavoriteCurrentSong() },
                        modifier = Modifier.size(30.dp),
                        enabled = currentSong != null
                    ) {
                        Icon(
                            painter = painterResource(if (isFavorite) Res.drawable.ic_rounded_favorite else Res.drawable.ic_rounded_favorite_border),
                            contentDescription = "Favorite",
                            tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AppTooltip(if (isAlwaysOnTop) "Always on Top: ON" else "Always on Top: OFF") {
                    IconButton(
                        onClick = onToggleAlwaysOnTop,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_push_pin),
                            contentDescription = "Always on Top",
                            tint = if (isAlwaysOnTop) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AppTooltip("Restore Full Player") {
                    IconButton(
                        onClick = onRestoreMainWindow,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_automirrored_rounded_open_in_new),
                            contentDescription = "Restore Full Player",
                            tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                MiniPlayerProgressBar(
                    value = currentFraction,
                    onValueChange = {
                        isDraggingSlider = true
                        dragSliderFraction = it
                    },
                    onValueChangeFinished = {
                        viewModel.seekTo((dragSliderFraction * totalDurationMs).roundToLong())
                        isDraggingSlider = false
                    },
                    colors = colors
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(displayPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Text(
                        text = formatDuration(totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTooltip("Shuffle") {
                    IconButton(
                        onClick = { viewModel.toggleShuffle() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_shuffle),
                            contentDescription = "Shuffle",
                            tint = if (playerInfo.isShuffleEnabled) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTooltip("Previous", modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { viewModel.playPrevious() },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.onPrimary,
                            contentColor = colors.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_skip_previous),
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AppTooltip(if (playerInfo.isPlaying) "Pause" else "Play", modifier = Modifier.weight(1.3f)) {
                        Surface(
                            onClick = { viewModel.togglePlayPause() },
                            shape = RoundedCornerShape(playPauseCorner),
                            color = colors.primary,
                            contentColor = colors.onPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playerInfo.isBuffering) {
                                    ExpressiveCircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = colors.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(if (playerInfo.isPlaying) Res.drawable.ic_rounded_pause else Res.drawable.ic_rounded_play_arrow),
                                        contentDescription = if (playerInfo.isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    AppTooltip("Next", modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { viewModel.playNext() },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.onPrimary,
                            contentColor = colors.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_skip_next),
                                    contentDescription = "Next",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                AppTooltip(
                    when (playerInfo.repeatMode) {
                        1 -> "Repeat All"
                        2 -> "Repeat One"
                        else -> "Repeat Off"
                    }
                ) {
                    IconButton(
                        onClick = { viewModel.cycleRepeatMode() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        val icon = if (playerInfo.repeatMode == 2) Res.drawable.ic_rounded_repeat_one else Res.drawable.ic_rounded_repeat
                        val tint = if (playerInfo.repeatMode != 0) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.6f)
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
