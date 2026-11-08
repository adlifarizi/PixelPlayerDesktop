package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_down
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_off
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_up
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite_border
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_fullscreen
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_pause
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat_one
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_shuffle
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_skip_next
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_skip_previous
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_tab
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip

@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    currentSong: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    totalDurationMs: Long = 0L,
    currentPositionFlow: StateFlow<Long>? = null,
    isShuffle: Boolean = false,
    repeatMode: Int = 0,
    isFavorite: Boolean = false,
    volume: Float = 1.0f,
    isMuted: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onMiniPlayerClick: (() -> Unit)? = null,
    onExpandClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    if (currentSong == null) return

    val colors = MaterialTheme.colorScheme

    val miniPlayerShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

    val playPauseCorner by animateDpAsState(
        targetValue = if (isPlaying) 12.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "MiniPlayerPlayPauseCorner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp)
            .clip(miniPlayerShape),
        color = colors.primaryContainer,
        shape = miniPlayerShape
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val barWidth = maxWidth
            val isNarrow = barWidth < 580.dp
            val isMedium = barWidth < 800.dp
            val isWide = barWidth >= 800.dp
            val centerMaxWidth = when {
                barWidth >= 1150.dp -> 460.dp
                barWidth >= 950.dp -> 380.dp
                barWidth >= 800.dp -> 300.dp
                barWidth >= 680.dp -> 250.dp
                else -> 200.dp
            }
            val centerMinWidth = if (isMedium) 160.dp else 220.dp

            if (isNarrow) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    val narrowInteractionSource = remember { MutableInteractionSource() }
                    val isNarrowHovered by narrowInteractionSource.collectIsHoveredAsState()

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTooltip("Expand Player", modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(
                                        interactionSource = narrowInteractionSource,
                                        indication = null,
                                        onClick = onExpandClick
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncArtworkImage(
                                    artworkUri = currentSong.albumArtUriString,
                                    modifier = Modifier.size(52.dp),
                                    shape = CircleShape,
                                    placeholderIcon = Res.drawable.ic_rounded_music_note,
                                    placeholderIconSize = 26.dp,
                                    containerColor = colors.surfaceVariant,
                                    contentColor = colors.onSurfaceVariant,
                                    contentDescription = "${currentSong.title} cover"
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSong.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 15.sp,
                                            textDecoration = if (isNarrowHovered) TextDecoration.Underline else TextDecoration.None
                                        ),
                                        fontWeight = FontWeight.Bold,
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

                                    Text(
                                        text = currentSong.displayArtist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = colors.onPrimaryContainer.copy(alpha = if (isNarrowHovered) 0.95f else 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTooltip("Previous") {
                            FilledIconButton(
                                onClick = onPreviousClick,
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.onPrimary,
                                    contentColor = colors.primary
                                )
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_skip_previous),
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        AppTooltip(if (isBuffering) "Buffering…" else if (isPlaying) "Pause" else "Play") {
                            FilledIconButton(
                                onClick = onPlayPauseClick,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(playPauseCorner),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                )
                            ) {
                                if (isBuffering) {
                                    ExpressiveCircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = colors.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(if (isPlaying) Res.drawable.ic_rounded_pause else Res.drawable.ic_rounded_play_arrow),
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        AppTooltip("Next") {
                            FilledIconButton(
                                onClick = onNextClick,
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.onPrimary,
                                    contentColor = colors.primary
                                )
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_skip_next),
                                    contentDescription = "Next",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (onMiniPlayerClick != null) {
                            AppTooltip("Mini Player (Ctrl+Shift+M)") {
                                IconButton(
                                    onClick = onMiniPlayerClick,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_tab),
                                        contentDescription = "Mini Player",
                                        tint = colors.onPrimaryContainer.copy(alpha = 0.65f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AppTooltip("Stop & Close") {
                            IconButton(
                                onClick = onCloseClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_close),
                                    contentDescription = "Stop & Close",
                                    tint = colors.onPrimaryContainer.copy(alpha = 0.65f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val artworkSize = if (isMedium) 50.dp else 60.dp
                    val artworkSpacing = if (isMedium) 10.dp else 14.dp
                    val actionButtonSize = if (isMedium) 34.dp else 38.dp
                    val actionIconSize = if (isMedium) 19.dp else 22.dp

                    val infoInteractionSource = remember { MutableInteractionSource() }
                    val isInfoHovered by infoInteractionSource.collectIsHoveredAsState()
                    val actionGapInteractionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (isMedium) 4.dp else 8.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = infoInteractionSource,
                                indication = null,
                                onClick = onExpandClick
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AppTooltip("Expand Player") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                AsyncArtworkImage(
                                    artworkUri = currentSong.albumArtUriString,
                                    modifier = Modifier.size(artworkSize),
                                    shape = CircleShape,
                                    placeholderIcon = Res.drawable.ic_rounded_music_note,
                                    placeholderIconSize = if (isMedium) 24.dp else 30.dp,
                                    containerColor = colors.surfaceVariant,
                                    contentColor = colors.onSurfaceVariant,
                                    contentDescription = "${currentSong.title} cover"
                                )

                                Spacer(modifier = Modifier.width(artworkSpacing))

                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = currentSong.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = if (isMedium) 15.sp else 16.sp,
                                                textDecoration = if (isInfoHovered) TextDecoration.Underline else TextDecoration.None
                                            ),
                                            fontWeight = FontWeight.Bold,
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

                                        Text(
                                            text = currentSong.displayArtist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isMedium) 12.5.sp else 13.5.sp),
                                            color = colors.onPrimaryContainer.copy(alpha = if (isInfoHovered) 0.95f else 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(if (isMedium) 6.dp else 8.dp))

                                    AppTooltip(if (isFavorite) "Remove from favorites" else "Add to favorites") {
                                        IconButton(
                                            onClick = onToggleFavorite,
                                            modifier = Modifier.size(actionButtonSize)
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isFavorite) Res.drawable.ic_rounded_favorite else Res.drawable.ic_rounded_favorite_border),
                                                contentDescription = "Favorite",
                                                tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.75f),
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(min = centerMinWidth, max = centerMaxWidth)
                            .padding(horizontal = if (isMedium) 4.dp else 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isMedium) 4.dp else 8.dp)
                        ) {
                            AppTooltip(if (isShuffle) "Shuffle: On" else "Shuffle: Off") {
                                FilledIconButton(
                                    onClick = onToggleShuffle,
                                    modifier = Modifier.size(if (isMedium) 32.dp else 36.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isShuffle) colors.primary else Color.Transparent,
                                        contentColor = if (isShuffle) colors.onPrimary else colors.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_shuffle),
                                        contentDescription = "Shuffle",
                                        modifier = Modifier.size(if (isMedium) 18.dp else 20.dp)
                                    )
                                }
                            }

                            AppTooltip("Previous") {
                                FilledIconButton(
                                    onClick = onPreviousClick,
                                    modifier = Modifier.size(if (isMedium) 36.dp else 40.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colors.onPrimary,
                                        contentColor = colors.primary
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_skip_previous),
                                        contentDescription = "Previous",
                                        modifier = Modifier.size(if (isMedium) 20.dp else 24.dp)
                                    )
                                }
                            }

                            AppTooltip(if (isBuffering) "Buffering…" else if (isPlaying) "Pause" else "Play") {
                                FilledIconButton(
                                    onClick = onPlayPauseClick,
                                    modifier = Modifier.size(if (isMedium) 36.dp else 40.dp),
                                    shape = RoundedCornerShape(playPauseCorner),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    )
                                ) {
                                    if (isBuffering) {
                                        ExpressiveCircularProgressIndicator(
                                            modifier = Modifier.size(if (isMedium) 18.dp else 22.dp),
                                            color = colors.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(if (isPlaying) Res.drawable.ic_rounded_pause else Res.drawable.ic_rounded_play_arrow),
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            modifier = Modifier.size(if (isMedium) 20.dp else 24.dp)
                                        )
                                    }
                                }
                            }

                            AppTooltip("Next") {
                                FilledIconButton(
                                    onClick = onNextClick,
                                    modifier = Modifier.size(if (isMedium) 36.dp else 40.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colors.onPrimary,
                                        contentColor = colors.primary
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_skip_next),
                                        contentDescription = "Next",
                                        modifier = Modifier.size(if (isMedium) 20.dp else 24.dp)
                                    )
                                }
                            }

                            val repeatLabel = when (repeatMode) {
                                1 -> "Repeat: All"
                                2 -> "Repeat: One"
                                else -> "Repeat: Off"
                            }
                            AppTooltip(repeatLabel) {
                                FilledIconButton(
                                    onClick = onCycleRepeat,
                                    modifier = Modifier.size(if (isMedium) 32.dp else 36.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (repeatMode > 0) colors.primary else Color.Transparent,
                                        contentColor = if (repeatMode > 0) colors.onPrimary else colors.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                ) {
                                    val repIcon = if (repeatMode == 2) Res.drawable.ic_rounded_repeat_one else Res.drawable.ic_rounded_repeat
                                    Icon(
                                        painter = painterResource(repIcon),
                                        contentDescription = "Repeat",
                                        modifier = Modifier.size(if (isMedium) 18.dp else 20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else currentSong.duration
                        MiniPlayerProgressTrack(
                            currentPositionFlow = currentPositionFlow,
                            totalDurationMs = totalDurationMs,
                            effectiveDuration = effectiveDuration,
                            isPlaying = isPlaying,
                            isMedium = isMedium,
                            onSeek = onSeek
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = artworkSize)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = actionGapInteractionSource,
                                indication = null,
                                onClick = onExpandClick
                            )
                            .padding(start = if (isMedium) 4.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (onMiniPlayerClick != null) {
                            AppTooltip("Mini Player (Ctrl+Shift+M)") {
                                IconButton(
                                    onClick = onMiniPlayerClick,
                                    modifier = Modifier.size(actionButtonSize)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_tab),
                                        contentDescription = "Mini Player",
                                        tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                        modifier = Modifier.size(actionIconSize)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        AppTooltip("Queue") {
                            IconButton(
                                onClick = onQueueClick,
                                modifier = Modifier.size(actionButtonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
                                    contentDescription = "Queue",
                                    tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        AppTooltip(if (isMuted) "Unmute" else "Mute") {
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier.size(actionButtonSize)
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
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        }

                        val showVolumeSlider = isWide && barWidth >= 840.dp
                        if (showVolumeSlider) {
                            var isDraggingVolume by remember { mutableStateOf(false) }
                            var localVolume by remember(volume) { mutableFloatStateOf(volume) }
                            val displayVolume = if (isMuted) 0f else if (isDraggingVolume) localVolume else volume
                            val volumeSliderWidth = if (barWidth >= 1050.dp) 96.dp else 72.dp

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
                                modifier = Modifier.width(volumeSliderWidth)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        AppTooltip("Expand Player") {
                            IconButton(
                                onClick = onExpandClick,
                                modifier = Modifier.size(actionButtonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_fullscreen),
                                    contentDescription = "Expand Player",
                                    tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        }

                        AppTooltip("Stop & Close") {
                            IconButton(
                                onClick = onCloseClick,
                                modifier = Modifier.size(actionButtonSize)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_close),
                                    contentDescription = "Stop & Close",
                                    tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    modifier = Modifier.size(if (isMedium) 18.dp else 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressTrack(
    currentPositionFlow: StateFlow<Long>?,
    totalDurationMs: Long,
    effectiveDuration: Long,
    isPlaying: Boolean,
    isMedium: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val positionState = currentPositionFlow?.collectAsState()
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val progress = if (effectiveDuration > 0) {
        val pos = positionState?.value ?: 0L
        (pos.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackCurrentTimeText(
            positionProvider = {
                val fraction = scrubFraction
                if (fraction != null && effectiveDuration > 0) {
                    (fraction * effectiveDuration).toLong()
                } else {
                    positionState?.value ?: 0L
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimaryContainer.copy(alpha = 0.75f)
        )

        Spacer(modifier = Modifier.width(if (isMedium) 4.dp else 8.dp))

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
            inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.20f),
            thumbColor = colors.primary,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(if (isMedium) 4.dp else 8.dp))

        Text(
            text = formatTime(effectiveDuration),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}
