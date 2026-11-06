package com.kayoi.pixelplayerdesktop.presentation.screens.player

import kotlin.math.roundToLong

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import com.kayoi.pixelplayerdesktop.data.model.SyncedWord

internal data class SyncedWordCluster(
    val startIndex: Int,
    val words: List<SyncedWord>
)

internal enum class LyricWordPhase {
    Future,
    Active,
    Completed
}

internal data class LyricWordVisualState(
    val phase: LyricWordPhase,
    val progress: Float
)

internal const val MAX_WORD_EMPHASIS_DURATION_MS = 240L
internal const val ACTIVE_WORD_SCALE_DELTA = 0.08f

internal fun clusterSyncedWords(words: List<SyncedWord>): List<SyncedWordCluster> {
    if (words.isEmpty()) return emptyList()

    val clusters = mutableListOf<SyncedWordCluster>()
    var currentWords = mutableListOf<SyncedWord>()
    var currentStartIndex = 0

    words.forEachIndexed { index, word ->
        if (word.startsNewWord && currentWords.isNotEmpty()) {
            clusters += SyncedWordCluster(startIndex = currentStartIndex, words = currentWords.toList())
            currentWords = mutableListOf()
            currentStartIndex = index
        } else if (currentWords.isEmpty()) {
            currentStartIndex = index
        }

        currentWords += word
    }

    if (currentWords.isNotEmpty()) {
        clusters += SyncedWordCluster(startIndex = currentStartIndex, words = currentWords.toList())
    }

    return clusters
}

internal fun resolveWordVisualStates(
    words: List<SyncedWord>,
    positionMs: Long,
    lineStartTimeMs: Long,
    lineEndTimeMs: Long
): List<LyricWordVisualState> {
    if (words.isEmpty()) return emptyList()
    if (positionMs < lineStartTimeMs || positionMs >= lineEndTimeMs) {
        return List(words.size) { LyricWordVisualState(LyricWordPhase.Future, 0f) }
    }

    val activeIndex = words.indexOfLast { it.time.toLong() <= positionMs }
    if (activeIndex < 0) {
        return List(words.size) { LyricWordVisualState(LyricWordPhase.Future, 0f) }
    }

    val activeWord = words[activeIndex]
    val activeWordStart = activeWord.time.toLong()
    val nextWordStart = words.getOrNull(activeIndex + 1)?.time?.toLong() ?: lineEndTimeMs
    val targetWordEnd = activeWord.endTime?.toLong() ?: nextWordStart
    val normalizedEnd = normalizeWordEndTime(
        currentWordTimeMs = activeWordStart,
        nextWordTimeMs = targetWordEnd,
        lineEndTimeMs = lineEndTimeMs
    )
    val emphasisEnd = minOf(normalizedEnd, activeWordStart + MAX_WORD_EMPHASIS_DURATION_MS)
    val emphasisDuration = (emphasisEnd - activeWordStart).coerceAtLeast(1L)
    val activeProgress = (
        (positionMs - activeWordStart).toFloat() / emphasisDuration.toFloat()
    ).coerceIn(0f, 1f)
    val isPastActiveWordEnd = activeWord.endTime?.let { positionMs >= it.toLong() } ?: false

    return List(words.size) { index ->
        when {
            index < activeIndex || (index == activeIndex && isPastActiveWordEnd) -> {
                LyricWordVisualState(LyricWordPhase.Completed, 1f)
            }
            index == activeIndex -> LyricWordVisualState(LyricWordPhase.Active, activeProgress)
            else -> LyricWordVisualState(LyricWordPhase.Future, 0f)
        }
    }
}

internal fun resolveWordScale(
    state: LyricWordVisualState,
    motionEnabled: Boolean = true
): Float {
    if (!motionEnabled || state.phase != LyricWordPhase.Active) return 1f
    return 1f + ACTIVE_WORD_SCALE_DELTA * (1f - state.progress.coerceIn(0f, 1f))
}

internal fun normalizeWordEndTime(
    currentWordTimeMs: Long,
    nextWordTimeMs: Long,
    lineEndTimeMs: Long
): Long {
    val minEnd = currentWordTimeMs + 1L
    val boundedLineEnd = lineEndTimeMs.coerceAtLeast(minEnd)
    return nextWordTimeMs.coerceIn(minEnd, boundedLineEnd)
}

internal fun resolveLineEndTimeMs(line: SyncedLine, nextLineStartMs: Int): Long {
    val baseEnd = if (nextLineStartMs > 0) nextLineStartMs.toLong() else Long.MAX_VALUE
    val lastWordStart = line.words?.maxOfOrNull { it.time.toLong() } ?: line.time.toLong()
    return if (nextLineStartMs > 0) maxOf(baseEnd, lastWordStart + 1L) else lastWordStart + 5000L
}

@Composable
internal fun rememberInterpolatedPlaybackPosition(
    sampledPositionMs: Long,
    isPlaying: Boolean,
    enabled: Boolean,
    playbackSpeed: Float = 1f
): Long {
    var interpolatedPositionMs by remember {
        mutableLongStateOf(sampledPositionMs)
    }

    if (!enabled) {
        return sampledPositionMs
    }

    LaunchedEffect(sampledPositionMs, isPlaying, playbackSpeed, enabled) {
        val anchorPositionMs = sampledPositionMs.coerceAtLeast(0L)
        val safePlaybackSpeed = playbackSpeed.takeIf { it.isFinite() && it > 0f } ?: 1f
        interpolatedPositionMs = anchorPositionMs
        if (!isPlaying || !enabled) return@LaunchedEffect

        val anchorNano = System.nanoTime()
        while (true) {
            delay(33L)
            val nowNano = System.nanoTime()
            val elapsedMs = ((nowNano - anchorNano) / 1_000_000L).coerceAtLeast(0L)
            interpolatedPositionMs = anchorPositionMs + (elapsedMs * safePlaybackSpeed).roundToLong()
        }
    }

    return interpolatedPositionMs
}
