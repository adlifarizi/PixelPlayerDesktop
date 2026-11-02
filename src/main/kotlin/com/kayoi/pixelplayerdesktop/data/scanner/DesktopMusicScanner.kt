package com.kayoi.pixelplayerdesktop.data.scanner

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.gagravarr.opus.OpusFile
import org.gagravarr.opus.OpusTags
import org.jaudiotagger.tag.FieldKey

import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.model.ArtistRef
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.generateArtistId
import com.kayoi.pixelplayerdesktop.utils.AdtsParser
import com.kayoi.pixelplayerdesktop.utils.AudioFileReaderUtils
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import com.kayoi.pixelplayerdesktop.utils.LrcParser
import com.kayoi.pixelplayerdesktop.utils.collectArtistNames
import com.kayoi.pixelplayerdesktop.utils.isDirectoryOrParentExcluded

sealed interface ScanProgress {
    data object Idle : ScanProgress
    data class Scanning(val currentFile: String, val scannedCount: Int, val totalDiscovered: Int) : ScanProgress
    data class Completed(val totalScanned: Int, val addedOrUpdated: Int, val durationMs: Long) : ScanProgress
    data class Error(val message: String) : ScanProgress
}
private const val PROGRESS_UPDATE_INTERVAL_MS = 100L
private val TRACK_NUMBER_PREFIX_REGEX = Regex("""^\d{1,3}[\s.\-_]+\s*""")
private val QUALITY_SUFFIX_REGEX = Regex(
    """\s*[\(\[]\s*(?:\d+\s*k(?:bps)?|official(?:\s+(?:audio|video|music\s+video|lyric\s+video))?|lyrics?)\s*[\)\]]""",
    RegexOption.IGNORE_CASE
)

/**
 * High-performance audio library scanner for local desktop storage.
 * Supports incremental scanning via file modification timestamps and multi-format audio tagging.
 */
class DesktopMusicScanner(
    private val musicDao: MusicDao,
    private val lyricsDao: LyricsDao? = null,
    private val artworkCacheDir: File = DesktopPathProvider.artworkCacheDir,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val supportedExtensions = setOf("mp3", "flac", "ogg", "opus", "m4a", "wav", "aac", "wma")

    private val commonArtworkNames = listOf(
        "cover.jpg", "cover.png", "cover.jpeg",
        "folder.jpg", "folder.png", "folder.jpeg",
        "album.jpg", "album.png", "album.jpeg",
        "artwork.jpg", "artwork.png", "artwork.jpeg",
        "front.jpg", "front.png", "front.jpeg",
        "thumb.jpg", "thumbnail.jpg"
    )

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    suspend fun scanDirectories(
        directories: List<String>,
        excludedDirectories: List<String> = emptyList(),
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true,
        autoScanLrcFiles: Boolean = false,
        pruneMissing: Boolean = true,
        forceRescan: Boolean = false
    ): ScanProgress = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        try {
            val normalizedExcluded = excludedDirectories.map { File(it).absolutePath }

            // 1. Discover all audio files across directories
            val discoveredFiles = mutableListOf<File>()
            val scannedDirectories = mutableListOf<String>()
            for (dirPath in directories) {
                try {
                    val dir = File(dirPath)
                    if (dir.exists() && dir.isDirectory) {
                        val rootDirAbs = dir.absolutePath
                        val isRootExcluded = isDirectoryOrParentExcluded(rootDirAbs, normalizedExcluded)
                        if (isRootExcluded) continue

                        Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
                            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                                return try {
                                    val currentDirAbs = dir.toFile().absolutePath
                                    val isExcluded = isDirectoryOrParentExcluded(currentDirAbs, normalizedExcluded)
                                    if (isExcluded) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
                                } catch (_: Exception) {
                                    FileVisitResult.SKIP_SUBTREE
                                }
                            }

                            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                                try {
                                    val f = file.toFile()
                                    val ext = f.extension.lowercase()
                                    if (supportedExtensions.contains(ext)) {
                                        discoveredFiles.add(f)
                                    }
                                } catch (_: Exception) { }
                                return FileVisitResult.CONTINUE
                            }

                            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                                // Gracefully skip inaccessible or malformed files/directories
                                return FileVisitResult.CONTINUE
                            }

                            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                                return FileVisitResult.CONTINUE
                            }
                        })
                        scannedDirectories.add(rootDirAbs)
                    }
                } catch (_: Exception) {
                    // Safely ignore failures accessing a specific directory
                }
            }

            val totalDiscovered = discoveredFiles.size
            _scanProgress.value = ScanProgress.Scanning(
                currentFile = "Starting scan...",
                scannedCount = 0,
                totalDiscovered = totalDiscovered
            )

            // 2. Fetch existing songs for incremental check
            val existingSongs = musicDao.getAllSongs().associateBy { it.path }
            val songsToUpsert = mutableListOf<Song>()
            val allDiscoveredPaths = mutableSetOf<String>()

            var scannedCount = 0
            var lastProgressUpdateTime = 0L
            for (file in discoveredFiles) {
                try {
                    scannedCount++
                    val path = file.absolutePath
                    allDiscoveredPaths.add(path)

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastProgressUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS || scannedCount == totalDiscovered) {
                        lastProgressUpdateTime = currentTime
                        _scanProgress.value = ScanProgress.Scanning(
                            currentFile = file.name,
                            scannedCount = scannedCount,
                            totalDiscovered = totalDiscovered
                        )
                    }

                    val lastModified = file.lastModified()
                    val existing = existingSongs[path]

                    if (!forceRescan && existing != null && existing.dateModified == lastModified && !(existing.duration == 0L && file.extension.equals("aac", ignoreCase = true))) {
                        continue
                    }

                    // Extract tags
                    val song = extractSongMetadata(file, existing?.id, artistDelimiters, wordDelimiters, extractArtistFromTitle)
                    if (song != null) {
                        songsToUpsert.add(song)
                        if (autoScanLrcFiles && lyricsDao != null) {
                            scanAndCacheLrcFile(file, song.id)
                        }
                    }
                } catch (_: Exception) {
                    // Gracefully skip metadata extraction errors for single corrupted files
                }
            }

            // 3. Batch save to DB in chunks
            if (songsToUpsert.isNotEmpty()) {
                songsToUpsert.chunked(500).forEach { batch ->
                    musicDao.upsertSongs(batch)
                }
            }

            // 4. Prune missing files if enabled
            if (pruneMissing && scannedDirectories.isNotEmpty()) {
                musicDao.deleteSongsNotInPaths(allDiscoveredPaths, scannedDirectories)
            }

            val duration = System.currentTimeMillis() - startTime
            val completed = ScanProgress.Completed(
                totalScanned = totalDiscovered,
                addedOrUpdated = songsToUpsert.size,
                durationMs = duration
            )
            _scanProgress.value = completed
            completed
        } catch (e: Exception) {
            val error = ScanProgress.Error(e.message ?: "Unknown scanning error")
            _scanProgress.value = error
            error
        }
    }

    private fun extractSongMetadata(
        file: File,
        existingId: String? = null,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true
    ): Song? {
        val ext = file.extension.lowercase()
        return when (ext) {
            "opus" -> extractOpusMetadata(file, existingId, artistDelimiters, wordDelimiters, extractArtistFromTitle)
            "aac" -> extractAacMetadata(file, existingId, artistDelimiters, wordDelimiters, extractArtistFromTitle)
            else -> extractGeneralMetadata(file, existingId, artistDelimiters, wordDelimiters, extractArtistFromTitle)
        }
    }

    private fun extractOpusMetadata(
        file: File,
        existingId: String?,
        artistDelimiters: List<String>,
        wordDelimiters: List<String>,
        extractArtistFromTitle: Boolean
    ): Song? {
        return try {
            var opusFile: OpusFile? = null
            try {
                opusFile = OpusFile(file)
                val tags = opusFile.tags
                val info = opusFile.info

                val (inferredTitle, inferredArtist) = inferTitleAndArtistFromFilename(file.nameWithoutExtension)

                val title = tags?.title?.trim()?.ifBlank { null }
                    ?: getVorbisComment(tags, "title")?.trim()?.ifBlank { null }
                    ?: inferredTitle
                    ?: file.nameWithoutExtension

                val rawArtist = tags?.artist?.trim()?.ifBlank { null }
                    ?: getVorbisComment(tags, "artist")?.trim()?.ifBlank { null }
                    ?: inferredArtist
                    ?: "Unknown Artist"

                val parsedArtists = collectArtistNames(
                    rawArtistName = rawArtist,
                    title = title,
                    artistDelimiters = artistDelimiters,
                    wordDelimiters = wordDelimiters,
                    extractFromTitle = extractArtistFromTitle
                )
                val artist = parsedArtists.firstOrNull()?.ifBlank { null } ?: rawArtist

                val artistRefs = parsedArtists.mapIndexed { index, name ->
                    ArtistRef(
                        id = generateArtistId(name),
                        name = name.trim(),
                        isPrimary = (index == 0)
                    )
                }

                val album = tags?.album?.trim()?.ifBlank { null }
                    ?: getVorbisComment(tags, "album")?.trim()?.ifBlank { null }
                    ?: "Unknown Album"

                val albumArtist = getVorbisComment(tags, "albumartist")?.trim()?.ifBlank { null }
                    ?: getVorbisComment(tags, "album_artist")?.trim()?.ifBlank { null }

                val genre = tags?.genre?.trim()?.ifBlank { null }
                    ?: getVorbisComment(tags, "genre")?.trim()?.ifBlank { null }

                val lyrics = getVorbisComment(tags, "lyrics")?.trim()?.ifBlank { null }
                    ?: getVorbisComment(tags, "unsyncedlyrics")?.trim()?.ifBlank { null }

                val trackNumber = tags?.trackNumber?.toIntOrNull()
                    ?: getVorbisComment(tags, "tracknumber")?.toIntOrNull()
                    ?: getVorbisComment(tags, "track")?.toIntOrNull()
                    ?: 0

                val discNumber = getVorbisComment(tags, "discnumber")?.toIntOrNull()
                    ?: getVorbisComment(tags, "disc")?.toIntOrNull()

                val year = parseYear(tags?.date)
                    ?: parseYear(getVorbisComment(tags, "date"))
                    ?: parseYear(getVorbisComment(tags, "year"))
                    ?: parseYear(getVorbisComment(tags, "originaldate"))
                    ?: parseYear(getVorbisComment(tags, "original_date"))
                    ?: 0

                val durationMs = getOpusDurationMs(file)
                val sampleRate = info?.sampleRate ?: 48000

                // Extract artwork from Vorbis comment tags or folder art
                val artworkUri = extractOpusArtworkToCache(tags, file)

                val songId = existingId ?: UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString()
                val artistId = generateArtistId(artist)
                val albumId = "$album-$artist".hashCode().toLong()

                Song(
                    id = songId,
                    title = title,
                    artist = artist,
                    artistId = artistId,
                    album = album,
                    albumId = albumId,
                    albumArtist = albumArtist,
                    path = file.absolutePath,
                    contentUriString = file.toURI().toString(),
                    albumArtUriString = artworkUri,
                    duration = durationMs,
                    genre = genre,
                    lyrics = lyrics,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    year = year,
                    dateAdded = System.currentTimeMillis(),
                    dateModified = file.lastModified(),
                    mimeType = "audio/opus",
                    bitrate = null,
                    sampleRate = sampleRate,
                    artists = artistRefs
                )
            } finally {
                opusFile?.close()
            }
        } catch (e: Exception) {
            fallbackSong(file, existingId)
        }
    }

    private fun parseYear(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(raw) ?: Regex("""\d{4}""").find(raw)
        return match?.value?.toIntOrNull() ?: raw.trim().take(4).toIntOrNull()
    }

    private fun extractAacMetadata(
        file: File,
        existingId: String?,
        artistDelimiters: List<String>,
        wordDelimiters: List<String>,
        extractArtistFromTitle: Boolean
    ): Song? {
        val result = AdtsParser.parse(file) ?: return extractGeneralMetadata(
            file,
            existingId,
            artistDelimiters,
            wordDelimiters,
            extractArtistFromTitle
        )

        val tag = result.tag
        val audioInfo = result.audioInfo

        val (inferredTitle, inferredArtist) = inferTitleAndArtistFromFilename(file.nameWithoutExtension)

        val title = tag?.getFirst(FieldKey.TITLE)?.trim()?.ifBlank { null }
            ?: inferredTitle
            ?: file.nameWithoutExtension

        val rawArtist = tag?.getFirst(FieldKey.ARTIST)?.trim()?.ifBlank { null }
            ?: inferredArtist
            ?: "Unknown Artist"

        val parsedArtists = collectArtistNames(
            rawArtistName = rawArtist,
            title = title,
            artistDelimiters = artistDelimiters,
            wordDelimiters = wordDelimiters,
            extractFromTitle = extractArtistFromTitle
        )
        val artist = parsedArtists.firstOrNull()?.ifBlank { null } ?: rawArtist

        val artistRefs = parsedArtists.mapIndexed { index, name ->
            ArtistRef(
                id = generateArtistId(name),
                name = name.trim(),
                isPrimary = (index == 0)
            )
        }

        val album = tag?.getFirst(FieldKey.ALBUM)?.trim()?.ifBlank { null } ?: "Unknown Album"
        val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)?.trim()?.ifBlank { null }
        val genre = tag?.getFirst(FieldKey.GENRE)?.trim()?.ifBlank { null }
        val lyrics = tag?.getFirst(FieldKey.LYRICS)?.trim()?.ifBlank { null }

        val year = parseYear(tag?.getFirst(FieldKey.YEAR))
            ?: parseYear(tag?.getFirst(FieldKey.ORIGINAL_YEAR))
            ?: 0
        val trackNumber = tag?.getFirst(FieldKey.TRACK)?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
        val discNumber = tag?.getFirst(FieldKey.DISC_NO)?.substringBefore('/')?.trim()?.toIntOrNull()

        val artworkUri = extractArtworkToCache(tag, file)

        val songId = existingId ?: UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString()
        val artistId = generateArtistId(artist)
        val albumId = "$album-$artist".hashCode().toLong()

        return Song(
            id = songId,
            title = title,
            artist = artist,
            artistId = artistId,
            album = album,
            albumId = albumId,
            albumArtist = albumArtist,
            path = file.absolutePath,
            contentUriString = file.toURI().toString(),
            albumArtUriString = artworkUri,
            duration = audioInfo.durationMs,
            genre = genre,
            lyrics = lyrics,
            trackNumber = trackNumber,
            discNumber = discNumber,
            year = year,
            dateAdded = System.currentTimeMillis(),
            dateModified = file.lastModified(),
            mimeType = "audio/aac",
            bitrate = audioInfo.bitrate,
            sampleRate = audioInfo.sampleRate,
            artists = artistRefs
        )
    }

    private fun inferTitleAndArtistFromFilename(filename: String): Pair<String?, String?> {
        val withoutQuality = filename.replace(QUALITY_SUFFIX_REGEX, "").trim()
        val cleaned = withoutQuality.replace(TRACK_NUMBER_PREFIX_REGEX, "").trim().ifBlank { withoutQuality }

        if (!cleaned.contains(" - ")) {
            return Pair(cleaned.ifBlank { null }, null)
        }

        val parts = cleaned.split(" - ", limit = 2)
        if (parts.size != 2) {
            return Pair(cleaned.ifBlank { null }, null)
        }

        val part0 = parts[0].trim()
        val part1 = parts[1].trim()

        val part0HasTitleHint = part0.contains("(") || part0.contains("[")
        val part1HasTitleHint = part1.contains("(") || part1.contains("[")

        return if (part0HasTitleHint && !part1HasTitleHint) {
            Pair(part0.ifBlank { null }, part1.ifBlank { null })
        } else {
            Pair(part1.ifBlank { null }, part0.ifBlank { null })
        }
    }

    private fun extractGeneralMetadata(
        file: File,
        existingId: String?,
        artistDelimiters: List<String>,
        wordDelimiters: List<String>,
        extractArtistFromTitle: Boolean
    ): Song? {
        return try {
            val audioFile = AudioFileReaderUtils.readAudioFile(file)
            val tag = audioFile.tag
            val header = audioFile.audioHeader

            val (inferredTitle, inferredArtist) = inferTitleAndArtistFromFilename(file.nameWithoutExtension)

            val title = tag?.getFirst(FieldKey.TITLE)?.trim()?.ifBlank { null }
                ?: inferredTitle
                ?: file.nameWithoutExtension
            val rawArtist = tag?.getFirst(FieldKey.ARTIST)?.trim()?.ifBlank { null }
                ?: inferredArtist
                ?: "Unknown Artist"

            val parsedArtists = collectArtistNames(
                rawArtistName = rawArtist,
                title = title,
                artistDelimiters = artistDelimiters,
                wordDelimiters = wordDelimiters,
                extractFromTitle = extractArtistFromTitle
            )
            val artist = parsedArtists.firstOrNull()?.ifBlank { null } ?: rawArtist

            val artistRefs = parsedArtists.mapIndexed { index, name ->
                ArtistRef(
                    id = generateArtistId(name),
                    name = name.trim(),
                    isPrimary = (index == 0)
                )
            }

            val album = tag?.getFirst(FieldKey.ALBUM)?.trim()?.ifBlank { null } ?: "Unknown Album"
            val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)?.trim()?.ifBlank { null }
            val genre = tag?.getFirst(FieldKey.GENRE)?.trim()?.ifBlank { null }
            val lyrics = tag?.getFirst(FieldKey.LYRICS)?.trim()?.ifBlank { null }

            val year = parseYear(tag?.getFirst(FieldKey.YEAR))
                ?: parseYear(tag?.getFirst(FieldKey.ORIGINAL_YEAR))
                ?: 0
            val trackNumber = tag?.getFirst(FieldKey.TRACK)?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
            val discNumber = tag?.getFirst(FieldKey.DISC_NO)?.substringBefore('/')?.trim()?.toIntOrNull()

            val durationMs = (header?.trackLength?.toLong() ?: 0L) * 1000L
            val bitrate = header?.bitRateAsNumber?.toInt()
            val sampleRate = header?.sampleRateAsNumber

            // Extract artwork to cache (embedded first, then folder cover fallback)
            val artworkUri = extractArtworkToCache(tag, file)

            val songId = existingId ?: UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString()
            val artistId = generateArtistId(artist)
            val albumId = "$album-$artist".hashCode().toLong()

            Song(
                id = songId,
                title = title,
                artist = artist,
                artistId = artistId,
                album = album,
                albumId = albumId,
                albumArtist = albumArtist,
                path = file.absolutePath,
                contentUriString = file.toURI().toString(),
                albumArtUriString = artworkUri,
                duration = durationMs,
                genre = genre,
                lyrics = lyrics,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year,
                dateAdded = System.currentTimeMillis(),
                dateModified = file.lastModified(),
                mimeType = "audio/${file.extension.lowercase()}",
                bitrate = bitrate,
                sampleRate = sampleRate,
                artists = artistRefs
            )
        } catch (e: Exception) {
            fallbackSong(file, existingId)
        }
    }

    private fun getVorbisComment(tags: OpusTags?, key: String): String? {
        if (tags == null) return null
        return tags.allComments.entries
            .firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?.value
            ?.firstOrNull()
    }

    /**
     * Extracts precise duration for Opus audio by locating the final Ogg page header (`OggS`)
     * in the tail of the file and converting its 64-bit granule position at standard 48kHz sample rate.
     */
    private fun getOpusDurationMs(file: File): Long {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val length = raf.length()
                if (length < 28) return 0L
                val searchSize = minOf(65536L, length).toInt()
                val buffer = ByteArray(searchSize)
                raf.seek(length - searchSize)
                raf.readFully(buffer)

                // Search backwards for OggS magic number (0x4F 0x67 0x67 0x53)
                for (i in (searchSize - 14) downTo 0) {
                    if (buffer[i] == 'O'.code.toByte() &&
                        buffer[i + 1] == 'g'.code.toByte() &&
                        buffer[i + 2] == 'g'.code.toByte() &&
                        buffer[i + 3] == 'S'.code.toByte()
                    ) {
                        val granulePos = (buffer[i + 6].toLong() and 0xFF) or
                                ((buffer[i + 7].toLong() and 0xFF) shl 8) or
                                ((buffer[i + 8].toLong() and 0xFF) shl 16) or
                                ((buffer[i + 9].toLong() and 0xFF) shl 24) or
                                ((buffer[i + 10].toLong() and 0xFF) shl 32) or
                                ((buffer[i + 11].toLong() and 0xFF) shl 40) or
                                ((buffer[i + 12].toLong() and 0xFF) shl 48) or
                                ((buffer[i + 13].toLong() and 0xFF) shl 56)

                        if (granulePos > 0) {
                            return (granulePos * 1000L) / 48000L
                        }
                    }
                }
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun extractOpusArtworkToCache(tags: OpusTags?, audioFile: File): String? {
        if (tags != null) {
            // Check METADATA_BLOCK_PICTURE embedded in Vorbis comment tags
            val pictureBlock = getVorbisComment(tags, "metadata_block_picture")
            if (!pictureBlock.isNullOrBlank()) {
                val imageBytes = parseFlacPictureBlock(pictureBlock)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    val cached = saveBytesToArtworkCache(imageBytes)
                    if (cached != null) return cached
                }
            }

            // Check legacy Base64 COVERART Vorbis tag
            val coverArt = getVorbisComment(tags, "coverart")
            if (!coverArt.isNullOrBlank()) {
                try {
                    val imageBytes = Base64.getDecoder().decode(coverArt.trim())
                    if (imageBytes.isNotEmpty()) {
                        val cached = saveBytesToArtworkCache(imageBytes)
                        if (cached != null) return cached
                    }
                } catch (_: Exception) { }
            }
        }

        // Fallback to co-located directory images (e.g. cover.jpg, folder.jpg)
        return findFolderArtworkCached(audioFile.parentFile)
    }

    private fun extractArtworkToCache(tag: org.jaudiotagger.tag.Tag?, audioFile: File): String? {
        if (tag != null) {
            val artwork = tag.firstArtwork
            val binaryData = artwork?.binaryData
            if (binaryData != null && binaryData.isNotEmpty()) {
                val cached = saveBytesToArtworkCache(binaryData)
                if (cached != null) return cached
            }
        }

        return findFolderArtworkCached(audioFile.parentFile)
    }

    /**
     * Decodes binary FLAC picture block structure containing mime type, description, and raw image bytes.
     */
    private fun parseFlacPictureBlock(base64Data: String): ByteArray? {
        return try {
            val bytes = Base64.getDecoder().decode(base64Data.trim())
            if (bytes.size < 32) return null
            val buffer = ByteBuffer.wrap(bytes)
            val type = buffer.int
            val mimeLen = buffer.int
            if (mimeLen < 0 || mimeLen > bytes.size - 8) return null
            val mimeBytes = ByteArray(mimeLen)
            buffer.get(mimeBytes)
            val descLen = buffer.int
            if (descLen < 0 || descLen > bytes.size - buffer.position()) return null
            val descBytes = ByteArray(descLen)
            buffer.get(descBytes)
            val width = buffer.int
            val height = buffer.int
            val depth = buffer.int
            val colors = buffer.int
            val dataLen = buffer.int
            if (dataLen <= 0 || dataLen > bytes.size - buffer.position()) return null
            val imgData = ByteArray(dataLen)
            buffer.get(imgData)
            imgData
        } catch (e: Exception) {
            null
        }
    }

    private fun findFolderArtworkCached(parentDir: File?): String? {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory) return null
        val files = parentDir.listFiles() ?: return null
        for (name in commonArtworkNames) {
            val found = files.firstOrNull { it.name.equals(name, ignoreCase = true) && it.length() > 0 }
            if (found != null) {
                return found.absolutePath
            }
        }
        return null
    }

    /**
     * Persists artwork binary to local cache directory keyed by SHA-256 hash for deduplication.
     */
    private fun saveBytesToArtworkCache(binaryData: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(binaryData).joinToString("") { "%02x".format(it) }
            val cachedFile = File(artworkCacheDir, "$hash.jpg")
            if (!cachedFile.exists()) {
                cachedFile.writeBytes(binaryData)
            }
            cachedFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun fallbackSong(file: File, existingId: String?): Song {
        val songId = existingId ?: UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString()
        val folderArt = findFolderArtworkCached(file.parentFile)
        val (inferredTitle, inferredArtist) = inferTitleAndArtistFromFilename(file.nameWithoutExtension)
        val title = inferredTitle ?: file.nameWithoutExtension
        val artist = inferredArtist ?: "Unknown Artist"
        val artistId = generateArtistId(artist)
        return Song(
            id = songId,
            title = title,
            artist = artist,
            artistId = artistId,
            album = "Unknown Album",
            albumId = "Unknown Album".hashCode().toLong(),
            path = file.absolutePath,
            contentUriString = file.toURI().toString(),
            albumArtUriString = folderArt,
            duration = 0L,
            genre = null,
            lyrics = null,
            trackNumber = 0,
            discNumber = null,
            year = 0,
            dateAdded = System.currentTimeMillis(),
            dateModified = file.lastModified(),
            mimeType = "audio/${file.extension.lowercase()}"
        )
    }

    private suspend fun scanAndCacheLrcFile(audioFile: File, songId: String) {
        val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
        if (lrcFile.exists() && lrcFile.isFile) {
            runCatching {
                val lrcText = lrcFile.readText(Charsets.UTF_8)
                val parsed = LrcParser.parse(lrcText)
                lyricsDao?.insertOrUpdateLyrics(songId, parsed)
            }
        }
    }
}
