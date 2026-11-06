package com.kayoi.pixelplayerdesktop.data.repository

import java.io.File

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.tag.FieldKey

import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.network.lyrics.LrcLibClient
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.AudioFileReaderUtils
import com.kayoi.pixelplayerdesktop.utils.LrcParser
import com.kayoi.pixelplayerdesktop.utils.MultiLangRomanizer

class LyricsRepositoryImpl(
    private val lyricsDao: LyricsDao,
    private val lrcLibClient: LrcLibClient = LrcLibClient(),
    private val settingsRepository: SettingsRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LyricsRepository {

    override suspend fun getLyrics(
        songId: String,
        songPath: String,
        title: String,
        artist: String
    ): Lyrics? = withContext(ioDispatcher) {
        val cached = lyricsDao.getLyrics(songId)
        if (cached != null) {
            val enriched = MultiLangRomanizer.enrichLyrics(cached)
            if (enriched != cached) {
                lyricsDao.insertOrUpdateLyrics(songId, enriched)
            }
            return@withContext enriched
        }

        val settings = settingsRepository?.getSettings()
        val preference = settings?.let {
            LyricsSourcePreference.fromStorageKey(it.lyricsSourcePreference)
        } ?: LyricsSourcePreference.EMBEDDED_FIRST
        val allowOnline = settings?.fetchOnlineLyrics ?: false
        val file = File(songPath)

        val fetchers: List<suspend () -> Lyrics?> = when (preference) {
            LyricsSourcePreference.EMBEDDED_FIRST -> listOfNotNull(
                { fetchEmbedded(file) },
                if (allowOnline) ({ fetchOnline(title, artist) }) else null,
                { fetchLocal(file) }
            )
            LyricsSourcePreference.API_FIRST -> listOfNotNull(
                if (allowOnline) ({ fetchOnline(title, artist) }) else null,
                { fetchEmbedded(file) },
                { fetchLocal(file) }
            )
            LyricsSourcePreference.LOCAL_FIRST -> listOfNotNull(
                { fetchLocal(file) },
                { fetchEmbedded(file) },
                if (allowOnline) ({ fetchOnline(title, artist) }) else null
            )
        }

        for (fetcher in fetchers) {
            val lyrics = fetcher()
            if (lyrics != null && (!lyrics.synced.isNullOrEmpty() || !lyrics.plain.isNullOrEmpty())) {
                val enriched = MultiLangRomanizer.enrichLyrics(lyrics)
                lyricsDao.insertOrUpdateLyrics(songId, enriched)
                return@withContext enriched
            }
        }

        null
    }

    private fun fetchLocal(file: File): Lyrics? {
        if (!file.exists()) return null
        val lrcCandidate = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
        val ttmlCandidate = File(file.parentFile, "${file.nameWithoutExtension}.ttml")
        val lyricsFile = when {
            lrcCandidate.exists() && lrcCandidate.isFile -> lrcCandidate
            ttmlCandidate.exists() && ttmlCandidate.isFile -> ttmlCandidate
            else -> null
        }
        if (lyricsFile != null) {
            return runCatching {
                val text = lyricsFile.readText(Charsets.UTF_8)
                LrcParser.parse(text).copy(areFromRemote = false)
            }.getOrNull()
        }
        return null
    }

    private fun fetchEmbedded(file: File): Lyrics? {
        if (!file.exists()) return null
        return runCatching {
            val audioFile = AudioFileReaderUtils.readAudioFile(file)
            val tag = audioFile.tag ?: return@runCatching null

            var firstPlainLyrics: Lyrics? = null
            val candidates = buildList {
                val standardTag = tag.getFirst(FieldKey.LYRICS)
                if (!standardTag.isNullOrBlank()) add(standardTag)
                listOf("LYRICS", "SYNCEDLYRICS", "TTML", "UNSYNCEDLYRICS").forEach { key ->
                    runCatching { tag.getFirst(key) }.getOrNull()?.let {
                        if (it.isNotBlank()) add(it)
                    }
                }
            }.distinct()

            for (field in candidates) {
                val parsed = LrcParser.parse(field).copy(areFromRemote = false)
                if (!parsed.synced.isNullOrEmpty()) return@runCatching parsed
                if (firstPlainLyrics == null && !parsed.plain.isNullOrEmpty()) {
                    firstPlainLyrics = parsed
                }
            }
            firstPlainLyrics
        }.getOrNull()
    }

    private suspend fun fetchOnline(title: String, artist: String): Lyrics? {
        if (title.isBlank() || artist.isBlank()) return null
        val onlineLyrics = lrcLibClient.fetchLyrics(title = title, artist = artist)
        return onlineLyrics?.copy(areFromRemote = true)
    }

    override suspend fun saveLyrics(songId: String, lyrics: Lyrics) {
        val enriched = MultiLangRomanizer.enrichLyrics(lyrics)
        lyricsDao.insertOrUpdateLyrics(songId, enriched)
    }

    override suspend fun deleteLyrics(songId: String) = withContext(ioDispatcher) {
        lyricsDao.deleteLyrics(songId)
    }

    override suspend fun resetAllLyrics() = withContext(ioDispatcher) {
        lyricsDao.deleteAllLyrics()
    }

    override suspend fun searchOnlineLyrics(query: String): List<com.kayoi.pixelplayerdesktop.data.network.lyrics.LyricsSearchResultItem> = withContext(ioDispatcher) {
        lrcLibClient.searchCandidates(query)
    }
}
