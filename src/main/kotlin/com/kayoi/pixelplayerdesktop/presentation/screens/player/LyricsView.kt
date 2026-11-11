package com.kayoi.pixelplayerdesktop.presentation.screens.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import com.kayoi.pixelplayerdesktop.data.model.SyncedWord
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_lyrics
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_search
import com.kayoi.pixelplayerdesktop.presentation.components.ContainedLoadingIndicator
import com.kayoi.pixelplayerdesktop.presentation.components.LocalWindowMinimized
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.BubblesLine
import com.kayoi.pixelplayerdesktop.utils.MultiLangRomanizer

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    isLoading: Boolean = false,
    currentPositionFlow: StateFlow<Long>? = null,
    onSeekToMs: (Long) -> Unit,
    onSearchLyrics: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    currentPositionMs: Long = 0L,
    alignment: String = "left",
    textSize: String = "medium",
    syncOffsetMs: Int = 0,
    showRomanization: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val positionMsState = currentPositionFlow?.collectAsState()
    val lyricTextAlign = when (alignment.lowercase()) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        else -> TextAlign.Start
    }
    val horizontalAlignment = when (alignment.lowercase()) {
        "center" -> Alignment.CenterHorizontally
        "right" -> Alignment.End
        else -> Alignment.Start
    }
    val lyricTransformOrigin = when (alignment.lowercase()) {
        "center" -> TransformOrigin(0.5f, 0.5f)
        "right" -> TransformOrigin(1f, 0.5f)
        else -> TransformOrigin(0f, 0.5f)
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContainedLoadingIndicator(
                    containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                    indicatorColor = colors.primary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Loading lyrics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    if (lyrics == null || (lyrics.synced.isNullOrEmpty() && lyrics.plain.isNullOrEmpty())) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(Res.drawable.ic_lyrics),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No lyrics found for this song",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (onSearchLyrics != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onSearchLyrics,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_search),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Lyrics")
                    }
                }
            }
        }
        return
    }

    if (!lyrics.synced.isNullOrEmpty()) {
        val syncedLines = lyrics.synced
        val isMinimized = LocalWindowMinimized.current
        val hasWordTimings = remember(syncedLines) { syncedLines.any { !it.words.isNullOrEmpty() } }
        val rawPosition = (positionMsState?.value ?: currentPositionMs) + syncOffsetMs
        val interpolatedPosition = rememberInterpolatedPlaybackPosition(
            sampledPositionMs = rawPosition,
            isPlaying = isPlaying,
            enabled = hasWordTimings && !isMinimized
        )

        val activeIndex by remember(syncedLines, positionMsState, syncOffsetMs) {
            derivedStateOf {
                val currentPos = (positionMsState?.value ?: currentPositionMs) + syncOffsetMs
                syncedLines.indexOfLast { it.time <= currentPos }
            }
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val totalHeight = maxHeight
            val isCompact = totalHeight < 280.dp
            val isVeryCompact = totalHeight < 190.dp

            val topPadding = when {
                isVeryCompact -> 8.dp
                isCompact -> 14.dp
                else -> 28.dp
            }
            val bottomPadding = when {
                isVeryCompact -> 16.dp
                isCompact -> 28.dp
                else -> 64.dp
            }
            val itemSpacing = if (isCompact) 8.dp else 12.dp
            val (baseMainSize, baseRomanSize) = when (textSize.lowercase()) {
                "small" -> 18.sp to 13.5.sp
                "large" -> 28.sp to 19.5.sp
                else -> 23.sp to 16.5.sp
            }
            val mainFontSize = when {
                isVeryCompact -> (baseMainSize.value - 4f).sp
                isCompact -> (baseMainSize.value - 2f).sp
                else -> baseMainSize
            }
            val romanizationFontSize = when {
                isCompact -> (baseRomanSize.value - 3f).sp
                else -> baseRomanSize
            }

            LaunchedEffect(activeIndex, isCompact, isVeryCompact) {
                if (activeIndex in syncedLines.indices) {
                    val targetIndex = when {
                        isVeryCompact -> activeIndex
                        isCompact -> (activeIndex - 1).coerceAtLeast(0)
                        else -> (activeIndex - 2).coerceAtLeast(0)
                    }
                    listState.animateScrollToItem(targetIndex)
                } else if (activeIndex < 0) {
                    listState.animateScrollToItem(0)
                }
            }

            val hasKana = remember(syncedLines) {
                syncedLines.any { line -> line.line.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 16.dp else 24.dp),
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
            itemsIndexed(syncedLines) { index, line ->
                val isActive = activeIndex >= 0 && index == activeIndex
                val isPast = activeIndex >= 0 && index < activeIndex

                if (line.line.isBlank()) {
                    BubblesLine(
                        isActive = isActive,
                        isPlaying = isPlaying,
                        color = colors.primary,
                        alignment = alignment,
                        onSeek = { onSeekToMs(((line.time - syncOffsetMs).toLong()).coerceAtLeast(0L)) }
                    )
                } else {
                    val scaleAnim = animateFloatAsState(
                        targetValue = if (isActive) 1f else 17f / 23f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "lyricScale"
                    )

                    val alphaAnim = animateFloatAsState(
                        targetValue = when {
                            isActive -> 1f
                            isPast -> 0.85f
                            else -> 0.38f
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "lyricAlpha"
                    )

                    val textColor = if (isActive) {
                        colors.primary
                    } else if (isPast) {
                        colors.onSurface
                    } else {
                        colors.onSurfaceVariant
                    }

                    val romanization = remember(line, hasKana) {
                        line.romanization?.takeIf { it.isNotBlank() }
                            ?: if (MultiLangRomanizer.isScriptThatNeedsRomanization(line.line)) {
                                MultiLangRomanizer.romanizeText(line.line, hasKana)
                            } else null
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSeekToMs(((line.time - syncOffsetMs).toLong()).coerceAtLeast(0L)) }
                            .padding(vertical = 4.dp, horizontal = 10.dp)
                            .graphicsLayer {
                                scaleX = scaleAnim.value
                                scaleY = scaleAnim.value
                                alpha = alphaAnim.value
                                transformOrigin = lyricTransformOrigin
                            },
                        horizontalAlignment = horizontalAlignment
                    ) {
                        if (line.words.isNullOrEmpty() || !isActive) {
                            if (line.line.isNotBlank()) {
                                Text(
                                    text = line.line,
                                    fontSize = mainFontSize,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = textColor,
                                    textAlign = lyricTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            WordSyncedLineContent(
                                line = line,
                                isActive = isActive,
                                isPast = isPast,
                                positionMs = interpolatedPosition,
                                nextLineStartMs = syncedLines.getOrNull(index + 1)?.time ?: 0,
                                alignment = alignment,
                                activeColor = colors.primary,
                                inactiveColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
                                pastColor = colors.onSurface,
                                fontSize = mainFontSize
                            )
                        }

                        if (!line.background?.text.isNullOrBlank()) {
                            Text(
                                text = line.background.text,
                                fontSize = if (line.line.isBlank()) mainFontSize else romanizationFontSize,
                                fontWeight = if (line.line.isBlank() && isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = if (isActive) 0.85f else 0.65f),
                                textAlign = lyricTextAlign,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (line.line.isBlank()) 0.dp else if (isCompact) 2.dp else 3.dp)
                            )
                        }

                        if (showRomanization && !romanization.isNullOrBlank()) {
                            Text(
                                text = romanization,
                                fontSize = romanizationFontSize,
                                fontWeight = FontWeight.Normal,
                                color = textColor.copy(alpha = if (isActive) 0.85f else 0.75f),
                                textAlign = lyricTextAlign,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (isCompact) 2.dp else 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
} else if (!lyrics.plain.isNullOrEmpty()) {
        val hasKana = remember(lyrics.plain) {
            lyrics.plain.any { line -> line.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } }
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val totalHeight = maxHeight
            val isCompact = totalHeight < 280.dp
            val topPadding = if (isCompact) 12.dp else 28.dp
            val bottomPadding = if (isCompact) 20.dp else 56.dp
            val itemSpacing = if (isCompact) 8.dp else 12.dp

            val (plainMainFontSize, plainRomanFontSize) = when (textSize.lowercase()) {
                "small" -> 15.sp to 12.5.sp
                "large" -> 21.sp to 17.sp
                else -> 18.sp to 14.5.sp
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 18.dp else 28.dp),
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
            items(lyrics.plain) { rawLine ->
                val parts = remember(rawLine) { rawLine.split("\n") }
                val primary = parts.firstOrNull().orEmpty()
                val romanization = remember(rawLine, primary, hasKana) {
                    if (parts.size > 1 && MultiLangRomanizer.isScriptThatNeedsRomanization(primary)) {
                        parts[1]
                    } else if (MultiLangRomanizer.isScriptThatNeedsRomanization(primary)) {
                        MultiLangRomanizer.romanizeText(primary, hasKana)
                    } else null
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = horizontalAlignment
                ) {
                    Text(
                        text = primary,
                        fontSize = plainMainFontSize,
                        color = colors.onSurface.copy(alpha = 0.85f),
                        textAlign = lyricTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showRomanization && !romanization.isNullOrBlank()) {
                        Text(
                            text = romanization,
                            fontSize = plainRomanFontSize,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = lyricTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun WordSyncedLineContent(
    line: SyncedLine,
    isActive: Boolean,
    isPast: Boolean,
    positionMs: Long,
    nextLineStartMs: Int,
    alignment: String,
    activeColor: Color,
    inactiveColor: Color,
    pastColor: Color,
    fontSize: TextUnit = 23.sp,
    modifier: Modifier = Modifier
) {
    val words = line.words ?: return
    val clusters = remember(words) { clusterSyncedWords(words) }
    val lineEndTime = remember(line, nextLineStartMs) {
        resolveLineEndTimeMs(line, nextLineStartMs)
    }

    val visualStates = if (isActive) {
        resolveWordVisualStates(
            words = words,
            positionMs = positionMs,
            lineStartTimeMs = line.time.toLong(),
            lineEndTimeMs = lineEndTime
        )
    } else {
        null
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = when (alignment.lowercase()) {
            "center" -> Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            "right" -> Arrangement.spacedBy(4.dp, Alignment.End)
            else -> Arrangement.spacedBy(4.dp, Alignment.Start)
        },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        clusters.forEach { cluster ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                cluster.words.forEachIndexed { clusterOffset, word ->
                    val wordIndex = cluster.startIndex + clusterOffset
                    val visualState = visualStates?.getOrNull(wordIndex)
                    key("${line.time}_${word.time}_${word.word}_$wordIndex") {
                        LyricWordSpan(
                            word = word,
                            visualState = visualState,
                            isActive = isActive,
                            isPast = isPast,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            pastColor = pastColor,
                            fontSize = fontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricWordSpan(
    word: SyncedWord,
    visualState: LyricWordVisualState?,
    isActive: Boolean,
    isPast: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    pastColor: Color,
    fontSize: TextUnit = 23.sp,
    modifier: Modifier = Modifier
) {
    val isHighlighted = if (isActive) {
        visualState?.phase != LyricWordPhase.Future
    } else {
        isPast
    }
    val scale = if (isActive && visualState != null) {
        resolveWordScale(visualState, motionEnabled = true)
    } else 1f

    val textColor = when {
        isHighlighted && isActive -> activeColor
        isPast -> pastColor
        else -> inactiveColor
    }

    Text(
        text = word.word,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
    )
}
