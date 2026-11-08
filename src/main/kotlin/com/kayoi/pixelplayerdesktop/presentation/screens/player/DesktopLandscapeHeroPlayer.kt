package com.kayoi.pixelplayerdesktop.presentation.screens.player

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_down
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_off
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_up
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite_border
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_pause
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat_one
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_shuffle
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_skip_next
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_skip_previous
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.ArtistRef
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.ArtworkSize
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveCircularProgressIndicator
import com.kayoi.pixelplayerdesktop.presentation.components.PlaybackCurrentTimeText
import com.kayoi.pixelplayerdesktop.presentation.components.WavySliderExpressive
import com.kayoi.pixelplayerdesktop.presentation.components.formatTime
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip
import com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal fun formatAudioMetaLabel(song: Song): String = AudioMetaUtils.formatAudioMetaLabel(song)

@Composable
fun DesktopLandscapeHeroPlayer(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    totalDurationMs: Long,
    currentPositionFlow: StateFlow<Long>? = null,
    controlActionFlow: SharedFlow<String>? = null,
    isShuffle: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    volume: Float,
    isMuted: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onArtistClick: ((ArtistRef) -> Unit)? = null,
    onAlbumClick: (() -> Unit)? = null,
    showPlayerFileInfo: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        val heroWidth = maxWidth
        val isStackedMode = heroWidth < 460.dp

        if (isStackedMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTooltip(if (onAlbumClick != null) "View album" else "") {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (onAlbumClick != null) {
                                        Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable(onClick = onAlbumClick)
                                    } else Modifier
                                )
                        ) {
                            AsyncArtworkImage(
                                artworkUri = song.albumArtUriString,
                                modifier = Modifier.fillMaxSize(),
                                placeholderIcon = Res.drawable.ic_rounded_music_note,
                                placeholderIconSize = 36.dp,
                                containerColor = colors.primaryContainer,
                                contentColor = colors.onPrimaryContainer,
                                contentDescription = "${song.title} artwork"
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        lineHeight = 23.sp
                                    ),
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = if (isPlaying) TextOverflow.Clip else TextOverflow.Ellipsis,
                                    modifier = Modifier.then(
                                        if (isPlaying) {
                                            Modifier.basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                initialDelayMillis = 1500,
                                                repeatDelayMillis = 2000
                                            )
                                        } else Modifier
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                InteractiveArtistNames(
                                    song = song,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    onArtistClick = onArtistClick
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val metaLabel = formatAudioMetaLabel(song)
                                if (showPlayerFileInfo && metaLabel.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = colors.onPrimaryContainer.copy(alpha = 0.15f),
                                        contentColor = colors.onPrimaryContainer
                                    ) {
                                        Text(
                                            text = metaLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            val stackedFavCornerRadius by animateDpAsState(
                                targetValue = if (isFavorite) 8.dp else 16.dp,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "stackedFavCorner"
                            )
                            val stackedFavBgColor by animateColorAsState(
                                targetValue = if (isFavorite) colors.primary else colors.onPrimary.copy(alpha = 0.45f),
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "stackedFavBg"
                            )
                            val stackedFavTint by animateColorAsState(
                                targetValue = if (isFavorite) colors.onPrimary else colors.primary,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "stackedFavTint"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                AppTooltip(if (isFavorite) "Remove from favorites" else "Add to favorites") {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(stackedFavCornerRadius))
                                            .background(stackedFavBgColor)
                                            .clickable(onClick = onToggleFavorite),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(if (isFavorite) Res.drawable.ic_rounded_favorite else Res.drawable.ic_rounded_favorite_border),
                                            contentDescription = "Favorite",
                                            tint = stackedFavTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                AppTooltip(if (isMuted) "Unmute" else "Mute") {
                                    IconButton(
                                        onClick = onToggleMute,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        val volIcon = when {
                                            isMuted || volume == 0f -> Res.drawable.ic_automirrored_rounded_volume_off
                                            volume < 0.5f -> Res.drawable.ic_automirrored_rounded_volume_down
                                            else -> Res.drawable.ic_automirrored_rounded_volume_up
                                        }
                                        Icon(
                                            painter = painterResource(volIcon),
                                            contentDescription = "Volume",
                                            tint = if (isMuted) colors.error else colors.onPrimaryContainer.copy(alpha = 0.75f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HeroPlayerProgressTrack(
                    currentPositionFlow = currentPositionFlow,
                    totalDurationMs = totalDurationMs,
                    song = song,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    isStacked = true
                )

                PlaybackControlsRow(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onPlayPause = onPlayPause,
                    onPrev = onPrev,
                    onNext = onNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    controlActionFlow = controlActionFlow,
                    buttonHeight = 52.dp,
                    shuffleRepeatSize = 40.dp,
                    colors = colors
                )
            }
        } else {
            val isCompactHero = heroWidth < 700.dp
            val favButtonSize = if (isCompactHero) 36.dp else 42.dp
            val favCornerRadius by animateDpAsState(
                targetValue = if (isFavorite) 11.dp else (favButtonSize / 2),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "favCorner"
            )
            val favBgColor by animateColorAsState(
                targetValue = if (isFavorite) colors.primary else colors.onPrimary.copy(alpha = 0.45f),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "favBg"
            )
            val favTint by animateColorAsState(
                targetValue = if (isFavorite) colors.onPrimary else colors.primary,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "favTint"
            )
            val artworkSize = when {
                heroWidth < 500.dp -> 105.dp
                heroWidth < 650.dp -> 135.dp
                heroWidth < 850.dp -> 175.dp
                else -> 215.dp
            }
            val gap = if (isCompactHero) 12.dp else 20.dp
            val titleFontSize = if (isCompactHero) 19.sp else 26.sp
            val titleLineHeight = if (isCompactHero) 24.sp else 32.sp
            val controlButtonHeight = if (isCompactHero) 48.dp else 56.dp
            val shuffleRepeatSize = if (isCompactHero) 36.dp else 42.dp
            val volumeBarHeight = if (isCompactHero) 140.dp else 180.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactHero) 16.dp else 22.dp,
                        vertical = if (isCompactHero) 14.dp else 18.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTooltip(if (onAlbumClick != null) "View album" else "") {
                        Box(
                            modifier = Modifier
                                .size(artworkSize)
                                .clip(RoundedCornerShape(20.dp))
                                .then(
                                    if (onAlbumClick != null) {
                                        Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable(onClick = onAlbumClick)
                                    } else Modifier
                                )
                        ) {
                            AsyncArtworkImage(
                                artworkUri = song.albumArtUriString,
                                targetSize = ArtworkSize.LARGE,
                                modifier = Modifier.fillMaxSize(),
                                placeholderIcon = Res.drawable.ic_rounded_music_note,
                                placeholderIconSize = if (isCompactHero) 40.dp else 56.dp,
                                containerColor = colors.primaryContainer,
                                contentColor = colors.onPrimaryContainer,
                                contentDescription = "${song.title} artwork"
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = titleFontSize,
                                        lineHeight = titleLineHeight
                                    ),
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = if (isPlaying) TextOverflow.Clip else TextOverflow.Ellipsis,
                                    modifier = Modifier.then(
                                        if (isPlaying) {
                                            Modifier.basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                initialDelayMillis = 1500,
                                                repeatDelayMillis = 2000
                                            )
                                        } else Modifier
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                InteractiveArtistNames(
                                    song = song,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = if (isCompactHero) 13.5.sp else 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    onArtistClick = onArtistClick
                                )
                            }

                            AppTooltip(if (isFavorite) "Remove from favorites" else "Add to favorites") {
                                Box(
                                    modifier = Modifier
                                        .size(favButtonSize)
                                        .clip(RoundedCornerShape(favCornerRadius))
                                        .background(favBgColor)
                                        .clickable(onClick = onToggleFavorite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(if (isFavorite) Res.drawable.ic_rounded_favorite else Res.drawable.ic_rounded_favorite_border),
                                        contentDescription = "Favorite",
                                        tint = favTint,
                                        modifier = Modifier.size(if (isCompactHero) 20.dp else 24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isCompactHero) 8.dp else 14.dp))

                        HeroPlayerProgressTrack(
                            currentPositionFlow = currentPositionFlow,
                            totalDurationMs = totalDurationMs,
                            song = song,
                            isPlaying = isPlaying,
                            onSeek = onSeek,
                            isStacked = false,
                            showPlayerFileInfo = showPlayerFileInfo
                        )

                        Spacer(modifier = Modifier.height(if (isCompactHero) 8.dp else 14.dp))

                        PlaybackControlsRow(
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            onPlayPause = onPlayPause,
                            onPrev = onPrev,
                            onNext = onNext,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeat,
                            controlActionFlow = controlActionFlow,
                            buttonHeight = controlButtonHeight,
                            shuffleRepeatSize = shuffleRepeatSize,
                            colors = colors
                        )
                    }

                    VerticalVolumeBar(
                        volume = volume,
                        isMuted = isMuted,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .height(volumeBarHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalVolumeBar(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isDraggingVolume by remember { mutableStateOf(false) }
    var localVolume by remember(volume) { mutableFloatStateOf(volume) }
    val displayVolume = if (isMuted) 0f else if (isDraggingVolume) localVolume else volume

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(44.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val sliderLength = maxHeight

            Slider(
                value = displayVolume.coerceIn(0f, 1f),
                onValueChange = {
                    isDraggingVolume = true
                    localVolume = it
                    onVolumeChange(it)
                },
                onValueChangeFinished = {
                    isDraggingVolume = false
                    onVolumeChange(localVolume)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.20f)
                ),
                modifier = Modifier
                    .requiredWidth(sliderLength)
                    .graphicsLayer {
                        rotationZ = 270f
                    }
            )
        }

        AppTooltip(if (isMuted) "Unmute" else "Mute") {
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier.size(36.dp)
            ) {
                val volIcon = when {
                    isMuted || displayVolume == 0f -> Res.drawable.ic_automirrored_rounded_volume_off
                    displayVolume < 0.5f -> Res.drawable.ic_automirrored_rounded_volume_down
                    else -> Res.drawable.ic_automirrored_rounded_volume_up
                }
                Icon(
                    painter = painterResource(volIcon),
                    contentDescription = "Volume",
                    tint = if (isMuted) colors.error else colors.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PlaybackControlsRow(
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    controlActionFlow: SharedFlow<String>? = null,
    buttonHeight: Dp = 56.dp,
    shuffleRepeatSize: Dp = 42.dp,
    colors: ColorScheme,
    modifier: Modifier = Modifier
) {
    var lastClickedButton by remember { mutableStateOf<String?>(null) }
    var triggerCount by remember { mutableLongStateOf(0L) }

    LaunchedEffect(controlActionFlow) {
        controlActionFlow?.collect { action ->
            lastClickedButton = action
            triggerCount++
        }
    }

    LaunchedEffect(triggerCount) {
        if (lastClickedButton != null) {
            delay(220L)
            lastClickedButton = null
        }
    }

    fun weightFor(button: String): Float = when (lastClickedButton) {
        button -> 1.25f
        null -> 1.0f
        else -> 0.72f
    }

    val prevWeight by animateFloatAsState(
        targetValue = weightFor("PREVIOUS"),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "prevWeightDesk"
    )
    val playWeight by animateFloatAsState(
        targetValue = weightFor("PLAY_PAUSE"),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "playWeightDesk"
    )
    val nextWeight by animateFloatAsState(
        targetValue = weightFor("NEXT"),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "nextWeightDesk"
    )

    val playPauseCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 22.dp else 38.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "playCornerDesk"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTooltip(if (isShuffle) "Shuffle: On" else "Shuffle: Off") {
            Box(
                modifier = Modifier
                    .size(shuffleRepeatSize)
                    .clip(CircleShape)
                    .background(if (isShuffle) colors.primary else colors.onPrimary.copy(alpha = 0.45f))
                    .clickable(onClick = onToggleShuffle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_shuffle),
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) colors.onPrimary else colors.primary,
                    modifier = Modifier.size(if (shuffleRepeatSize < 40.dp) 18.dp else 20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTooltip("Previous", modifier = Modifier.weight(prevWeight).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colors.onPrimary)
                        .clickable {
                            lastClickedButton = "PREVIOUS"
                            triggerCount++
                            onPrev()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_skip_previous),
                        contentDescription = "Previous Song",
                        tint = colors.primary,
                        modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp)
                    )
                }
            }

            AppTooltip(if (isBuffering) "Buffering..." else if (isPlaying) "Pause" else "Play", modifier = Modifier.weight(playWeight * 1.35f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(playPauseCornerRadius))
                        .background(colors.primary)
                        .clickable {
                            lastClickedButton = "PLAY_PAUSE"
                            triggerCount++
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        ExpressiveCircularProgressIndicator(
                            modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(if (isPlaying) Res.drawable.ic_rounded_pause else Res.drawable.ic_rounded_play_arrow),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(if (buttonHeight < 50.dp) 28.dp else 34.dp)
                        )
                    }
                }
            }

            AppTooltip("Next", modifier = Modifier.weight(nextWeight).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colors.onPrimary)
                        .clickable {
                            lastClickedButton = "NEXT"
                            triggerCount++
                            onNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_skip_next),
                        contentDescription = "Next Song",
                        tint = colors.primary,
                        modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp)
                    )
                }
            }
        }

        val repIcon = if (repeatMode == 2) Res.drawable.ic_rounded_repeat_one else Res.drawable.ic_rounded_repeat
        val repLabel = when (repeatMode) {
            1 -> "Repeat: All"
            2 -> "Repeat: One"
            else -> "Repeat: Off"
        }
        AppTooltip(repLabel) {
            Box(
                modifier = Modifier
                    .size(shuffleRepeatSize)
                    .clip(CircleShape)
                    .background(if (repeatMode > 0) colors.primary else colors.onPrimary.copy(alpha = 0.45f))
                    .clickable(onClick = onCycleRepeat),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(repIcon),
                    contentDescription = "Repeat",
                    tint = if (repeatMode > 0) colors.onPrimary else colors.primary,
                    modifier = Modifier.size(if (shuffleRepeatSize < 40.dp) 18.dp else 20.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroPlayerProgressTrack(
    currentPositionFlow: StateFlow<Long>?,
    totalDurationMs: Long,
    song: Song,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    isStacked: Boolean,
    showPlayerFileInfo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val positionState = currentPositionFlow?.collectAsState()
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else song.duration
    val progress = if (effectiveDuration > 0) {
        val pos = positionState?.value ?: 0L
        (pos.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val currentTimeProvider = {
        val fraction = scrubFraction
        if (fraction != null && effectiveDuration > 0) {
            (fraction * effectiveDuration).toLong()
        } else {
            positionState?.value ?: 0L
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        WavySliderExpressive(
            value = scrubFraction ?: progress,
            onValueChange = { scrubFraction = it },
            onValueChangeFinished = {
                val fraction = scrubFraction
                scrubFraction = null
                if (fraction != null) {
                    onSeek(fraction)
                }
            },
            isPlaying = isPlaying,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.22f),
            thumbColor = colors.primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        if (isStacked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackCurrentTimeText(
                    positionProvider = currentTimeProvider,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                )

                Text(
                    text = formatTime(effectiveDuration),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaybackCurrentTimeText(
                        positionProvider = currentTimeProvider,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    Text(
                        text = formatTime(effectiveDuration),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                val metaLabel = formatAudioMetaLabel(song)
                if (showPlayerFileInfo && metaLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = colors.onPrimaryContainer.copy(alpha = 0.15f),
                        contentColor = colors.onPrimaryContainer
                    ) {
                        Text(
                            text = metaLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveArtistNames(
    song: Song,
    style: TextStyle,
    color: Color,
    onArtistClick: ((ArtistRef) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val artists = song.resolvedArtists
    val annotatedString = remember(artists, color, onArtistClick) {
        buildAnnotatedString {
            artists.forEachIndexed { index, artistRef ->
                if (onArtistClick != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = artistRef.id.toString(),
                            styles = TextLinkStyles(
                                style = SpanStyle(color = color),
                                hoveredStyle = SpanStyle(
                                    color = color.copy(alpha = 1f),
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = { onArtistClick(artistRef) }
                        )
                    ) {
                        append(artistRef.name)
                    }
                } else {
                    append(artistRef.name)
                }
                if (index < artists.lastIndex) {
                    append(", ")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
