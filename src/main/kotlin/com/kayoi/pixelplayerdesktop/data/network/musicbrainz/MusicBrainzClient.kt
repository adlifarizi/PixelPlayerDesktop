package com.kayoi.pixelplayerdesktop.data.network.musicbrainz

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import com.kayoi.pixelplayerdesktop.BuildConfig

data class OnlineTrackMetadata(
    val title: String,
    val artist: String,
    val album: String?,
    val albumArtist: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val genre: String?,
    val releaseId: String?,
    val score: Int = 100
)

data class OnlineReleaseMetadata(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val trackCount: Int?,
    val country: String?,
    val score: Int = 100
)

class MusicBrainzClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    private suspend fun enforceRateLimit() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastRequestTime
            if (diff < 1000L) {
                delay(1000L - diff)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    suspend fun searchRecordings(
        title: String,
        artist: String,
        limit: Int = 10
    ): List<OnlineTrackMetadata> = withContext(ioDispatcher) {
        val query = buildString {
            if (title.isNotBlank()) {
                append("recording:\"${sanitizeLuceneQuery(title)}\"")
            }
            if (artist.isNotBlank()) {
                if (isNotEmpty()) append(" AND ")
                append("artist:\"${sanitizeLuceneQuery(artist)}\"")
            }
        }
        if (query.isBlank()) return@withContext emptyList()
        searchRecordingsByQuery(query, limit)
    }

    suspend fun searchRecordingsByQuery(
        query: String,
        limit: Int = 10
    ): List<OnlineTrackMetadata> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        enforceRateLimit()

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://musicbrainz.org/ws/2/recording/?query=$encodedQuery&limit=$limit&fmt=json"
            val responseText = executeHttpGet(urlString) ?: return@withContext emptyList()

            val root = json.parseToJsonElement(responseText).jsonObject
            val recordings = root["recordings"]?.jsonArray ?: return@withContext emptyList()

            val results = mutableListOf<OnlineTrackMetadata>()
            for (elem in recordings) {
                val rec = elem.jsonObject
                val recTitle = rec["title"]?.jsonPrimitive?.content ?: continue
                val score = rec["score"]?.jsonPrimitive?.intOrNull ?: 100

                val artistCredits = rec["artist-credit"]?.jsonArray
                val recArtist = artistCredits?.joinToString("") {
                    val ac = it.jsonObject
                    val name = ac["name"]?.jsonPrimitive?.content ?: ""
                    val join = ac["joinphrase"]?.jsonPrimitive?.content ?: ""
                    name + join
                }?.trim()?.ifBlank { null } ?: "Unknown Artist"

                val releases = rec["releases"]?.jsonArray
                val firstRelease = releases?.firstOrNull()?.jsonObject
                val albumTitle = firstRelease?.get("title")?.jsonPrimitive?.content?.trim()
                val releaseId = firstRelease?.get("id")?.jsonPrimitive?.content?.trim()
                val releaseDate = firstRelease?.get("date")?.jsonPrimitive?.content?.trim()
                val year = parseYear(releaseDate)

                val albumArtistCredits = firstRelease?.get("artist-credit")?.jsonArray
                val albumArtist = albumArtistCredits?.joinToString("") {
                    val ac = it.jsonObject
                    val name = ac["name"]?.jsonPrimitive?.content ?: ""
                    val join = ac["joinphrase"]?.jsonPrimitive?.content ?: ""
                    name + join
                }?.trim()?.ifBlank { null }

                var trackNumber: Int? = null
                var discNumber: Int? = null
                val media = firstRelease?.get("media")?.jsonArray
                val firstMedia = media?.firstOrNull()?.jsonObject
                discNumber = firstMedia?.get("position")?.jsonPrimitive?.intOrNull
                val tracks = firstMedia?.get("track")?.jsonArray
                val trackObj = tracks?.firstOrNull()?.jsonObject
                trackNumber = trackObj?.get("number")?.jsonPrimitive?.content?.toIntOrNull()

                val tags = rec["tags"]?.jsonArray
                val genre = tags?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content?.trim()

                results.add(
                    OnlineTrackMetadata(
                        title = recTitle,
                        artist = recArtist,
                        album = albumTitle,
                        albumArtist = albumArtist,
                        year = year,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        genre = genre,
                        releaseId = releaseId,
                        score = score
                    )
                )
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchReleases(
        query: String,
        limit: Int = 10
    ): List<OnlineReleaseMetadata> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        enforceRateLimit()

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://musicbrainz.org/ws/2/release/?query=$encodedQuery&limit=$limit&fmt=json"
            val responseText = executeHttpGet(urlString) ?: return@withContext emptyList()

            val root = json.parseToJsonElement(responseText).jsonObject
            val releases = root["releases"]?.jsonArray ?: return@withContext emptyList()

            val results = mutableListOf<OnlineReleaseMetadata>()
            for (elem in releases) {
                val rel = elem.jsonObject
                val id = rel["id"]?.jsonPrimitive?.content ?: continue
                val title = rel["title"]?.jsonPrimitive?.content ?: continue
                val score = rel["score"]?.jsonPrimitive?.intOrNull ?: 100
                val date = rel["date"]?.jsonPrimitive?.content
                val country = rel["country"]?.jsonPrimitive?.content
                val trackCount = rel["track-count"]?.jsonPrimitive?.intOrNull

                val artistCredits = rel["artist-credit"]?.jsonArray
                val artist = artistCredits?.joinToString("") {
                    val ac = it.jsonObject
                    val name = ac["name"]?.jsonPrimitive?.content ?: ""
                    val join = ac["joinphrase"]?.jsonPrimitive?.content ?: ""
                    name + join
                }?.trim()?.ifBlank { null } ?: "Unknown Artist"

                results.add(
                    OnlineReleaseMetadata(
                        id = id,
                        title = title,
                        artist = artist,
                        year = parseYear(date),
                        trackCount = trackCount,
                        country = country,
                        score = score
                    )
                )
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCoverArt(releaseId: String): Result<ByteArray?> = withContext(ioDispatcher) {
        if (releaseId.isBlank()) return@withContext Result.success(null)
        var currentUrl = "https://coverartarchive.org/release/$releaseId/front-500"
        var redirects = 0

        while (redirects < 5) {
            try {
                val conn = URI(currentUrl).toURL().openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 6000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", USER_AGENT)

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    val location = conn.getHeaderField("Location")
                        ?: return@withContext Result.failure(IOException("Missing redirect location"))
                    currentUrl = location
                    redirects++
                    continue
                }

                if (status == HttpURLConnection.HTTP_OK) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    return@withContext Result.success(bytes)
                }

                if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                    return@withContext Result.success(null)
                }

                return@withContext Result.failure(IOException("HTTP error $status"))
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        Result.failure(IOException("Too many redirects"))
    }

    private fun executeHttpGet(urlString: String): String? {
        val conn = URI(urlString).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 6000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json")

        return if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    }

    internal fun sanitizeLuceneQuery(raw: String): String {
        val specialChars = listOf('\\', '+', '-', '!', '(', ')', ':', '^', '[', ']', '\"', '{', '}', '~', '*', '?', '|', '&', '/')
        val sb = StringBuilder()
        for (ch in raw) {
            if (specialChars.contains(ch)) {
                sb.append('\\')
            }
            sb.append(ch)
        }
        return sb.toString().trim()
    }

    internal fun parseYear(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(raw) ?: Regex("""\d{4}""").find(raw)
        return match?.value?.toIntOrNull() ?: raw.trim().take(4).toIntOrNull()
    }

    companion object {
        private val USER_AGENT = "PixelPlayerDesktop/${BuildConfig.APP_VERSION_NAME} ( https://github.com/kayoi/pixelplayer-desktop )"
    }
}
