package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Data model for song lyrics.
 *
 * @param plain List of plain-text lines (unsynced).
 * @param synced List of (milliseconds, line) pairs for synced lyrics.
 * @param areFromRemote Indicates whether the lyrics were fetched from a remote source.
 */
@Immutable
@Serializable
data class Lyrics(
    val plain: List<String>? = null,
    val synced: List<SyncedLine>? = null,
    val areFromRemote: Boolean = false
)

@Immutable
@Serializable
data class SyncedLine(
    val time: Int,
    val line: String,
    val words: List<SyncedWord>? = null,
    val translation: String? = null,
    val romanization: String? = null,
    val voice: Int? = null,
    val background: BackgroundVocals? = null
)

@Immutable
@Serializable
data class BackgroundVocals(
    val text: String,
    val words: List<SyncedWord>? = null
)

@Immutable
@Serializable
data class SyncedWord(
    val time: Int,
    val word: String,
    val startsNewWord: Boolean = true,
    val endTime: Int? = null
)
