package com.kayoi.pixelplayerdesktop.data.database

import java.io.File
import java.sql.ResultSet
import java.sql.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.ArtistRef
import com.kayoi.pixelplayerdesktop.data.model.Genre
import com.kayoi.pixelplayerdesktop.data.model.PlaybackEvent
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SearchFilterType
import com.kayoi.pixelplayerdesktop.data.model.SearchResultItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.model.generateArtistId

private val artistRefListSerializer = ListSerializer(ArtistRef.serializer())
private val stringListSerializer = ListSerializer(String.serializer())

class MusicDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getAllSongs(
        sortOption: SortOption = SortOption.SongTitleAZ,
        minDurationMs: Long = 0L
    ): List<Song> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.SongTitleAZ -> "title COLLATE NOCASE ASC"
            SortOption.SongTitleZA -> "title COLLATE NOCASE DESC"
            SortOption.SongArtist -> "artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, track_number ASC"
            SortOption.SongArtistDesc -> "artist COLLATE NOCASE DESC, album COLLATE NOCASE ASC, track_number ASC"
            SortOption.SongAlbum -> "album COLLATE NOCASE ASC, track_number ASC"
            SortOption.SongAlbumDesc -> "album COLLATE NOCASE DESC, track_number ASC"
            SortOption.SongDateAdded -> "date_added DESC"
            SortOption.SongDateAddedAsc -> "date_added ASC"
            SortOption.SongDuration -> "duration DESC"
            SortOption.SongDurationAsc -> "duration ASC"
            else -> "title COLLATE NOCASE ASC"
        }

        database.withConnection(readOnly = true) { conn ->
            val sql = if (minDurationMs > 0L) {
                "SELECT * FROM songs WHERE duration >= ? ORDER BY $orderByClause"
            } else {
                "SELECT * FROM songs ORDER BY $orderByClause"
            }
            conn.prepareStatement(sql).use { stmt ->
                if (minDurationMs > 0L) {
                    stmt.setLong(1, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong(json))
                    }
                    songs
                }
            }
        }
    }

    suspend fun hasSongs(): Boolean = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT 1 FROM songs LIMIT 1").use { rs ->
                    rs.next()
                }
            }
        }
    }

    suspend fun getSongById(id: String): Song? = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT * FROM songs WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toSong(json) else null
                }
            }
        }
    }

    suspend fun getSongByPath(path: String): Song? = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT * FROM songs WHERE path = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, path)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toSong(json) else null
                }
            }
        }
    }

    suspend fun getSongsByIds(ids: List<String>): List<Song> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        val resultMap = mutableMapOf<String, Song>()
        database.withConnection(readOnly = true) { conn ->
            ids.chunked(500).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                val sql = "SELECT * FROM songs WHERE id IN ($placeholders)"
                conn.prepareStatement(sql).use { stmt ->
                    chunk.forEachIndexed { index, id ->
                        stmt.setString(index + 1, id)
                    }
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val song = rs.toSong(json)
                            resultMap[song.id] = song
                        }
                    }
                }
            }
        }
        ids.mapNotNull { resultMap[it] }
    }

    suspend fun upsertSongs(songs: List<Song>) = withContext(ioDispatcher) {
        if (songs.isEmpty()) return@withContext
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                val sql = """
                    INSERT INTO songs (
                        id, title, artist, artist_id, artists_json, album, album_id, album_artist,
                        path, content_uri_string, album_art_uri_string, duration, genre,
                        lyrics, is_favorite, track_number, disc_number, year, date_added,
                        date_modified, mime_type, bitrate, sample_rate, navidrome_id, jellyfin_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(path) DO UPDATE SET
                        title = excluded.title,
                        artist = excluded.artist,
                        artist_id = excluded.artist_id,
                        artists_json = excluded.artists_json,
                        album = excluded.album,
                        album_id = excluded.album_id,
                        album_artist = excluded.album_artist,
                        content_uri_string = excluded.content_uri_string,
                        album_art_uri_string = excluded.album_art_uri_string,
                        duration = excluded.duration,
                        genre = excluded.genre,
                        lyrics = excluded.lyrics,
                        track_number = excluded.track_number,
                        disc_number = excluded.disc_number,
                        year = excluded.year,
                        date_modified = excluded.date_modified,
                        mime_type = excluded.mime_type,
                        bitrate = excluded.bitrate,
                        sample_rate = excluded.sample_rate;
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    for (song in songs) {
                        stmt.setString(1, song.id)
                        stmt.setString(2, song.title)
                        stmt.setString(3, song.artist)
                        stmt.setLong(4, song.artistId)
                        val artistsJson = json.encodeToString(
                            artistRefListSerializer,
                            song.artists
                        )
                        stmt.setString(5, artistsJson)
                        stmt.setString(6, song.album)
                        stmt.setLong(7, song.albumId)
                        stmt.setString(8, song.albumArtist)
                        stmt.setString(9, song.path)
                        stmt.setString(10, song.contentUriString)
                        stmt.setString(11, song.albumArtUriString)
                        stmt.setLong(12, song.duration)
                        stmt.setString(13, song.genre)
                        stmt.setString(14, song.lyrics)
                        stmt.setInt(15, if (song.isFavorite) 1 else 0)
                        stmt.setInt(16, song.trackNumber)
                        if (song.discNumber != null) stmt.setInt(17, song.discNumber) else stmt.setNull(17, Types.INTEGER)
                        stmt.setInt(18, song.year)
                        stmt.setLong(19, song.dateAdded)
                        stmt.setLong(20, song.dateModified)
                        stmt.setString(21, song.mimeType)
                        if (song.bitrate != null) stmt.setInt(22, song.bitrate) else stmt.setNull(22, Types.INTEGER)
                        if (song.sampleRate != null) stmt.setInt(23, song.sampleRate) else stmt.setNull(23, Types.INTEGER)
                        stmt.setString(24, song.navidromeId)
                        stmt.setString(25, song.jellyfinId)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                val deleteSongArtistsSql = "DELETE FROM song_artists WHERE song_id = ?"
                val insertArtistSql = "INSERT OR IGNORE INTO artists (id, name, track_count) VALUES (?, ?, 0)"
                val insertSongArtistSql = "INSERT OR REPLACE INTO song_artists (song_id, artist_id, is_primary) VALUES (?, ?, ?)"

                val affectedArtistIds = mutableSetOf<Long>()
                val songIds = songs.map { it.id }
                songIds.chunked(500).forEach { chunk ->
                    val placeholders = chunk.joinToString(",") { "?" }
                    val query = "SELECT DISTINCT artist_id FROM song_artists WHERE song_id IN ($placeholders)"
                    conn.prepareStatement(query).use { stmt ->
                        chunk.forEachIndexed { index, id -> stmt.setString(index + 1, id) }
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                affectedArtistIds.add(rs.getLong(1))
                            }
                        }
                    }
                }

                conn.prepareStatement(deleteSongArtistsSql).use { deleteStmt ->
                    for (song in songs) {
                        deleteStmt.setString(1, song.id)
                        deleteStmt.addBatch()
                    }
                    deleteStmt.executeBatch()
                }

                val songArtistsMap = songs.associateWith { song ->
                    if (song.artists.isNotEmpty()) {
                        song.artists.map { ref ->
                            if (ref.id != 0L) ref else ref.copy(id = generateArtistId(ref.name))
                        }
                    } else {
                        val aid = if (song.artistId != 0L) song.artistId else generateArtistId(song.artist)
                        listOf(ArtistRef(id = aid, name = song.artist, isPrimary = true))
                    }
                }

                for ((_, allArtists) in songArtistsMap) {
                    for (ref in allArtists) {
                        affectedArtistIds.add(ref.id)
                    }
                }

                conn.prepareStatement(insertArtistSql).use { artistStmt ->
                    for ((_, allArtists) in songArtistsMap) {
                        for (ref in allArtists) {
                            artistStmt.setLong(1, ref.id)
                            artistStmt.setString(2, ref.name)
                            artistStmt.addBatch()
                        }
                    }
                    artistStmt.executeBatch()
                }

                conn.prepareStatement(insertSongArtistSql).use { songArtistStmt ->
                    for ((song, allArtists) in songArtistsMap) {
                        for (ref in allArtists) {
                            songArtistStmt.setString(1, song.id)
                            songArtistStmt.setLong(2, ref.id)
                            songArtistStmt.setInt(3, if (ref.isPrimary) 1 else 0)
                            songArtistStmt.addBatch()
                        }
                    }
                    songArtistStmt.executeBatch()
                }

                if (affectedArtistIds.isNotEmpty()) {
                    affectedArtistIds.toList().chunked(500).forEach { chunk ->
                        val placeholders = chunk.joinToString(",") { "?" }
                        val updateSql = """
                            UPDATE artists 
                            SET track_count = (SELECT COUNT(*) FROM song_artists WHERE song_artists.artist_id = artists.id)
                            WHERE id IN ($placeholders)
                        """.trimIndent()
                        conn.prepareStatement(updateSql).use { stmt ->
                            chunk.forEachIndexed { index, id -> stmt.setLong(index + 1, id) }
                            stmt.executeUpdate()
                        }

                        val deleteSql = "DELETE FROM artists WHERE track_count = 0 AND id IN ($placeholders)"
                        conn.prepareStatement(deleteSql).use { stmt ->
                            chunk.forEachIndexed { index, id -> stmt.setLong(index + 1, id) }
                            stmt.executeUpdate()
                        }
                    }
                }

                conn.commit()
                database.notifyTableChanged("songs")
                database.notifyTableChanged("artists")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun updateSongPath(id: String, newPath: String, newContentUri: String) = withContext(ioDispatcher) {
        database.withConnection { conn ->
            val sql = "UPDATE songs SET path = ?, content_uri_string = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, newPath)
                stmt.setString(2, newContentUri)
                stmt.setString(3, id)
                stmt.executeUpdate()
            }
        }
        database.notifyTableChanged("songs")
    }

    suspend fun deleteSongsNotInPaths(
        existingPaths: Set<String>,
        scannedDirectories: List<String>
    ) = withContext(ioDispatcher) {
        if (scannedDirectories.isEmpty()) {
            return@withContext
        }

        val normalizedDirs = scannedDirectories.map { dir ->
            val abs = File(dir).absolutePath
            if (abs.endsWith(File.separator)) abs else abs + File.separator
        }

        database.withConnection { conn ->
            val pathsInScannedDirs = mutableSetOf<String>()
            val placeholders = normalizedDirs.joinToString(" OR ") { "path LIKE ? || '%'" }
            val sql = "SELECT path FROM songs WHERE navidrome_id IS NULL AND jellyfin_id IS NULL AND ($placeholders)"
            conn.prepareStatement(sql).use { stmt ->
                normalizedDirs.forEachIndexed { index, prefix ->
                    stmt.setString(index + 1, prefix)
                }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val path = rs.getString("path")
                        if (normalizedDirs.any { prefix -> path.startsWith(prefix) }) {
                            pathsInScannedDirs.add(path)
                        }
                    }
                }
            }

            val pathsToDelete = pathsInScannedDirs - existingPaths
            if (pathsToDelete.isNotEmpty()) {
                conn.autoCommit = false
                try {
                    conn.prepareStatement("DELETE FROM songs WHERE path = ? AND navidrome_id IS NULL AND jellyfin_id IS NULL").use { stmt ->
                        for (path in pathsToDelete) {
                            stmt.setString(1, path)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate("DELETE FROM song_artists WHERE song_id NOT IN (SELECT id FROM songs)")
                        stmt.executeUpdate("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM song_artists)")
                    }
                    conn.commit()
                    database.notifyTableChanged("songs")
                    database.notifyTableChanged("artists")
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        }
    }

    suspend fun deleteSongsByPaths(paths: Collection<String>) = withContext(ioDispatcher) {
        if (paths.isEmpty()) return@withContext
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("DELETE FROM songs WHERE path = ? AND navidrome_id IS NULL AND jellyfin_id IS NULL").use { stmt ->
                    for (path in paths) {
                        stmt.setString(1, path)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM song_artists WHERE song_id NOT IN (SELECT id FROM songs)")
                    stmt.executeUpdate("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM song_artists)")
                }
                conn.commit()
                database.notifyTableChanged("songs")
                database.notifyTableChanged("artists")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun deleteSongsInDirectory(directoryPath: String) = withContext(ioDispatcher) {
        val abs = File(directoryPath).absolutePath
        val prefix = if (abs.endsWith(File.separator)) abs else abs + File.separator
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("DELETE FROM songs WHERE path LIKE ? || '%' AND navidrome_id IS NULL AND jellyfin_id IS NULL").use { stmt ->
                    stmt.setString(1, prefix)
                    stmt.executeUpdate()
                }
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM song_artists WHERE song_id NOT IN (SELECT id FROM songs)")
                    stmt.executeUpdate("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM song_artists)")
                }
                conn.commit()
                database.notifyTableChanged("songs")
                database.notifyTableChanged("artists")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun clearNavidromeSongs() = withContext(ioDispatcher) {
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM songs WHERE navidrome_id IS NOT NULL")
                    stmt.executeUpdate("DELETE FROM song_artists WHERE song_id NOT IN (SELECT id FROM songs)")
                    stmt.executeUpdate("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM song_artists)")
                }
                conn.commit()
                database.notifyTableChanged("songs")
                database.notifyTableChanged("artists")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun clearJellyfinSongs() = withContext(ioDispatcher) {
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM songs WHERE jellyfin_id IS NOT NULL")
                    stmt.executeUpdate("DELETE FROM song_artists WHERE song_id NOT IN (SELECT id FROM songs)")
                    stmt.executeUpdate("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM song_artists)")
                }
                conn.commit()
                database.notifyTableChanged("songs")
                database.notifyTableChanged("artists")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun deleteSong(song: Song) = withContext(ioDispatcher) {
        runCatching {
            val file = File(song.path)
            if (file.exists()) {
                file.delete()
            }
        }
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("DELETE FROM songs WHERE id = ?").use { stmt ->
                    stmt.setString(1, song.id)
                    stmt.executeUpdate()
                }
                conn.prepareStatement("DELETE FROM song_artists WHERE song_id = ?").use { stmt ->
                    stmt.setString(1, song.id)
                    stmt.executeUpdate()
                }
                conn.prepareStatement("DELETE FROM favorites WHERE song_id = ?").use { stmt ->
                    stmt.setString(1, song.id)
                    stmt.executeUpdate()
                }
                conn.prepareStatement("DELETE FROM lyrics_cache WHERE song_id = ?").use { stmt ->
                    stmt.setString(1, song.id)
                    stmt.executeUpdate()
                }
                conn.prepareStatement("SELECT id, song_ids_json FROM playlists WHERE song_ids_json LIKE ?").use { stmt ->
                    stmt.setString(1, "%${song.id}%")
                    stmt.executeQuery().use { rs ->
                        val updates = mutableListOf<Pair<String, String>>()
                        while (rs.next()) {
                            val plId = rs.getString("id")
                            val raw = rs.getString("song_ids_json") ?: "[]"
                            val ids = try {
                                json.decodeFromString(stringListSerializer, raw)
                            } catch (e: Exception) {
                                emptyList()
                            }
                            if (ids.contains(song.id)) {
                                val newIds = ids.filter { it != song.id }
                                updates.add(plId to json.encodeToString(stringListSerializer, newIds))
                            }
                        }
                        if (updates.isNotEmpty()) {
                            conn.prepareStatement("UPDATE playlists SET song_ids_json = ? WHERE id = ?").use { updateStmt ->
                                for ((plId, newJson) in updates) {
                                    updateStmt.setString(1, newJson)
                                    updateStmt.setString(2, plId)
                                    updateStmt.addBatch()
                                }
                                updateStmt.executeBatch()
                            }
                        }
                    }
                }
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("""
                        UPDATE artists 
                        SET track_count = (SELECT COUNT(*) FROM song_artists WHERE song_artists.artist_id = artists.id)
                    """.trimIndent())
                    stmt.executeUpdate("DELETE FROM artists WHERE track_count = 0")
                }
                conn.commit()
                database.notifyTableChanged("songs")
                database.notifyTableChanged("artists")
                database.notifyTableChanged("favorites")
                database.notifyTableChanged("playlists")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun getAllAlbums(
        sortOption: SortOption = SortOption.AlbumTitleAZ,
        minTracks: Int = 1,
        minDurationMs: Long = 0L
    ): List<Album> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.AlbumTitleAZ -> "album COLLATE NOCASE ASC"
            SortOption.AlbumTitleZA -> "album COLLATE NOCASE DESC"
            SortOption.AlbumArtist -> "artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC"
            SortOption.AlbumArtistDesc -> "artist COLLATE NOCASE DESC, album COLLATE NOCASE ASC"
            SortOption.AlbumReleaseYear -> "max_year DESC"
            SortOption.AlbumReleaseYearAsc -> "max_year ASC"
            SortOption.AlbumDateAdded -> "max_date_added DESC"
            SortOption.AlbumSizeAsc -> "song_count ASC"
            SortOption.AlbumSizeDesc -> "song_count DESC"
            else -> "album COLLATE NOCASE ASC"
        }

        database.withConnection(readOnly = true) { conn ->
            val durationFilter = if (minDurationMs > 0L) "AND duration >= ?" else ""
            val sql = """
                SELECT
                    album_id,
                    album,
                    artist,
                    album_artist,
                    MAX(year) as max_year,
                    MAX(date_added) as max_date_added,
                    MAX(album_art_uri_string) as album_art_uri_string,
                    COUNT(*) as song_count
                FROM songs
                WHERE album != '' $durationFilter
                GROUP BY album, artist
                HAVING COUNT(*) >= ?
                ORDER BY $orderByClause
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                var paramIndex = 1
                if (minDurationMs > 0L) {
                    stmt.setLong(paramIndex++, minDurationMs)
                }
                stmt.setInt(paramIndex, minTracks)
                stmt.executeQuery().use { rs ->
                    val albums = mutableListOf<Album>()
                    while (rs.next()) {
                        albums.add(
                            Album(
                                id = rs.getLong("album_id"),
                                title = rs.getString("album"),
                                artist = rs.getString("artist"),
                                albumArtist = rs.getString("album_artist"),
                                year = rs.getInt("max_year"),
                                dateAdded = rs.getLong("max_date_added"),
                                albumArtUriString = rs.getString("album_art_uri_string"),
                                songCount = rs.getInt("song_count")
                            )
                        )
                    }
                    albums
                }
            }
        }
    }

    suspend fun getAlbumById(albumId: Long, minDurationMs: Long = 0L): Album? = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val durationFilter = if (minDurationMs > 0L) "AND duration >= ?" else ""
            val sql = """
                SELECT
                    album_id,
                    album,
                    artist,
                    album_artist,
                    MAX(year) as max_year,
                    MAX(date_added) as max_date_added,
                    MAX(album_art_uri_string) as album_art_uri_string,
                    COUNT(*) as song_count
                FROM songs
                WHERE album_id = ? $durationFilter
                GROUP BY album, artist
                LIMIT 1
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, albumId)
                if (minDurationMs > 0L) {
                    stmt.setLong(2, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        Album(
                            id = rs.getLong("album_id"),
                            title = rs.getString("album"),
                            artist = rs.getString("artist"),
                            albumArtist = rs.getString("album_artist"),
                            year = rs.getInt("max_year"),
                            dateAdded = rs.getLong("max_date_added"),
                            albumArtUriString = rs.getString("album_art_uri_string"),
                            songCount = rs.getInt("song_count")
                        )
                    } else null
                }
            }
        }
    }

    suspend fun getSongsByAlbum(albumId: Long, minDurationMs: Long = 0L): List<Song> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val durationFilter = if (minDurationMs > 0L) "AND duration >= ?" else ""
            val sql = "SELECT * FROM songs WHERE album_id = ? $durationFilter ORDER BY disc_number ASC, track_number ASC, title COLLATE NOCASE ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, albumId)
                if (minDurationMs > 0L) {
                    stmt.setLong(2, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong(json))
                    }
                    songs
                }
            }
        }
    }

    suspend fun getAllArtists(
        sortOption: SortOption = SortOption.ArtistNameAZ,
        minDurationMs: Long = 0L
    ): List<Artist> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.ArtistNameAZ -> "a.name COLLATE NOCASE ASC"
            SortOption.ArtistNameZA -> "a.name COLLATE NOCASE DESC"
            SortOption.ArtistNumSongsDesc -> "song_count DESC, a.name COLLATE NOCASE ASC"
            SortOption.ArtistNumSongsAsc -> "song_count ASC, a.name COLLATE NOCASE ASC"
            else -> "a.name COLLATE NOCASE ASC"
        }

        database.withConnection(readOnly = true) { conn ->
            val durationFilter = if (minDurationMs > 0L) "WHERE s.duration >= ?" else ""
            val sql = """
                SELECT
                    a.id as artist_id,
                    a.name as artist,
                    COUNT(sa.song_id) as song_count,
                    a.image_url,
                    a.custom_image_uri
                FROM artists a
                JOIN song_artists sa ON a.id = sa.artist_id
                JOIN songs s ON sa.song_id = s.id
                $durationFilter
                GROUP BY a.id, a.name
                HAVING song_count > 0
                ORDER BY $orderByClause
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                if (minDurationMs > 0L) {
                    stmt.setLong(1, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    val artists = mutableListOf<Artist>()
                    while (rs.next()) {
                        artists.add(
                            Artist(
                                id = rs.getLong("artist_id"),
                                name = rs.getString("artist"),
                                songCount = rs.getInt("song_count"),
                                imageUrl = rs.getString("image_url"),
                                customImageUri = rs.getString("custom_image_uri")
                            )
                        )
                    }
                    if (artists.isNotEmpty()) {
                        return@withContext artists
                    }
                }
            }

            // Fallback to songs table if artists table hasn't been populated yet
            val fallbackDurationFilter = if (minDurationMs > 0L) "AND duration >= ?" else ""
            val fallbackSql = """
                SELECT
                    artist_id,
                    artist,
                    COUNT(*) as song_count
                FROM songs
                WHERE artist != '' $fallbackDurationFilter
                GROUP BY artist
                HAVING song_count > 0
                ORDER BY artist COLLATE NOCASE ASC
            """.trimIndent()

            conn.prepareStatement(fallbackSql).use { stmt ->
                if (minDurationMs > 0L) {
                    stmt.setLong(1, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    val artists = mutableListOf<Artist>()
                    while (rs.next()) {
                        artists.add(
                            Artist(
                                id = rs.getLong("artist_id"),
                                name = rs.getString("artist"),
                                songCount = rs.getInt("song_count")
                            )
                        )
                    }
                    artists
                }
            }
        }
    }

    suspend fun getArtistById(artistId: Long, minDurationMs: Long = 0L): Artist? = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val durationFilter = if (minDurationMs > 0L) "AND s.duration >= ?" else ""
            val sql = """
                SELECT
                    a.id as artist_id,
                    a.name as artist,
                    COUNT(sa.song_id) as song_count,
                    a.image_url,
                    a.custom_image_uri
                FROM artists a
                JOIN song_artists sa ON a.id = sa.artist_id
                JOIN songs s ON sa.song_id = s.id
                WHERE a.id = ? $durationFilter
                GROUP BY a.id, a.name
                LIMIT 1
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, artistId)
                if (minDurationMs > 0L) {
                    stmt.setLong(2, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return@withContext Artist(
                            id = rs.getLong("artist_id"),
                            name = rs.getString("artist"),
                            songCount = rs.getInt("song_count"),
                            imageUrl = rs.getString("image_url"),
                            customImageUri = rs.getString("custom_image_uri")
                        )
                    }
                }
            }

            // Fallback to songs table
            val fallbackDurationFilter = if (minDurationMs > 0L) "AND duration >= ?" else ""
            val fallbackSql = """
                SELECT
                    artist_id,
                    artist,
                    COUNT(*) as song_count
                FROM songs
                WHERE artist_id = ? $fallbackDurationFilter
                GROUP BY artist
                LIMIT 1
            """.trimIndent()

            conn.prepareStatement(fallbackSql).use { stmt ->
                stmt.setLong(1, artistId)
                if (minDurationMs > 0L) {
                    stmt.setLong(2, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        Artist(
                            id = rs.getLong("artist_id"),
                            name = rs.getString("artist"),
                            songCount = rs.getInt("song_count")
                        )
                    } else null
                }
            }
        }
    }

    suspend fun updateArtistCustomImage(artistId: Long, customImageUri: String?) = withContext(ioDispatcher) {
        database.withConnection { conn ->
            val sql = "UPDATE artists SET custom_image_uri = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                if (customImageUri != null) {
                    stmt.setString(1, customImageUri)
                } else {
                    stmt.setNull(1, Types.VARCHAR)
                }
                stmt.setLong(2, artistId)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("artists")
        }
    }

    suspend fun getSongsByArtist(artistId: Long, minDurationMs: Long = 0L): List<Song> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val durationFilter = if (minDurationMs > 0L) "AND s.duration >= ?" else ""
            val sql = """
                SELECT s.* FROM songs s
                JOIN song_artists sa ON s.id = sa.song_id
                WHERE sa.artist_id = ? $durationFilter
                ORDER BY s.year DESC, s.album COLLATE NOCASE ASC, s.track_number ASC
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, artistId)
                if (minDurationMs > 0L) {
                    stmt.setLong(2, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong(json))
                    }
                    if (songs.isNotEmpty()) {
                        return@withContext songs
                    }
                }
            }

            // Fallback for primary artistId
            val fallbackDurationFilter = if (minDurationMs > 0L) "AND duration >= ?" else ""
            val fallbackSql = "SELECT * FROM songs WHERE artist_id = ? $fallbackDurationFilter ORDER BY year DESC, album COLLATE NOCASE ASC, track_number ASC"
            conn.prepareStatement(fallbackSql).use { stmt ->
                stmt.setLong(1, artistId)
                if (minDurationMs > 0L) {
                    stmt.setLong(2, minDurationMs)
                }
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong(json))
                    }
                    songs
                }
            }
        }
    }

    suspend fun getAllGenres(): List<Genre> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = """
                SELECT genre, COUNT(*) as count
                FROM songs
                WHERE genre IS NOT NULL AND genre != ''
                GROUP BY genre
                ORDER BY genre COLLATE NOCASE ASC
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val genres = mutableListOf<Genre>()
                    while (rs.next()) {
                        val name = rs.getString("genre")
                        genres.add(Genre(id = name, name = name))
                    }
                    genres
                }
            }
        }
    }

    suspend fun getSongsByGenre(genreName: String): List<Song> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT * FROM songs WHERE genre = ? ORDER BY title COLLATE NOCASE ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, genreName)
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong(json))
                    }
                    songs
                }
            }
        }
    }

    suspend fun recordSongPlayed(songId: String, durationMs: Long = 0L) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            val sql = "UPDATE songs SET play_count = play_count + 1, last_played_timestamp = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, now)
                stmt.setString(2, songId)
                stmt.executeUpdate()
            }

            val eventSql = "INSERT INTO playback_events (song_id, timestamp, duration_ms) VALUES (?, ?, ?)"
            conn.prepareStatement(eventSql).use { stmt ->
                stmt.setString(1, songId)
                stmt.setLong(2, now)
                stmt.setLong(3, durationMs)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("recently_played")
            database.notifyTableChanged("playback_events")
        }
    }

    suspend fun getPlaybackEvents(limit: Int = 500): List<PlaybackEvent> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT id, song_id, timestamp, duration_ms FROM playback_events ORDER BY timestamp DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<PlaybackEvent>()
                    while (rs.next()) {
                        list.add(
                            PlaybackEvent(
                                id = rs.getLong("id"),
                                songId = rs.getString("song_id"),
                                timestamp = rs.getLong("timestamp"),
                                durationMs = rs.getLong("duration_ms")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getPlaybackEventsBetween(start: Long, end: Long): List<PlaybackEvent> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT id, song_id, timestamp, duration_ms FROM playback_events WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, start)
                stmt.setLong(2, end)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<PlaybackEvent>()
                    while (rs.next()) {
                        list.add(
                            PlaybackEvent(
                                id = rs.getLong("id"),
                                songId = rs.getString("song_id"),
                                timestamp = rs.getLong("timestamp"),
                                durationMs = rs.getLong("duration_ms")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    suspend fun getRecentlyPlayedSongs(limit: Int = 500): List<Song> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT * FROM songs WHERE last_played_timestamp > 0 ORDER BY last_played_timestamp DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong(json))
                    }
                    songs
                }
            }
        }
    }

    suspend fun search(
        query: String,
        filter: SearchFilterType = SearchFilterType.ALL,
        minDurationMs: Long = 0L,
        minTracks: Int = 1
    ): List<SearchResultItem> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val q = "%$query%"
        val results = mutableListOf<SearchResultItem>()

        database.withConnection(readOnly = true) { conn ->
            if (filter == SearchFilterType.ALL || filter == SearchFilterType.SONGS) {
                val songSql = if (minDurationMs > 0L) {
                    "SELECT * FROM songs WHERE (title LIKE ? OR artist LIKE ? OR album LIKE ?) AND duration >= ? LIMIT 50"
                } else {
                    "SELECT * FROM songs WHERE (title LIKE ? OR artist LIKE ? OR album LIKE ?) LIMIT 50"
                }
                conn.prepareStatement(songSql).use { stmt ->
                    stmt.setString(1, q)
                    stmt.setString(2, q)
                    stmt.setString(3, q)
                    if (minDurationMs > 0L) {
                        stmt.setLong(4, minDurationMs)
                    }
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(SearchResultItem.SongItem(rs.toSong(json)))
                        }
                    }
                }
            }

            if (filter == SearchFilterType.ALL || filter == SearchFilterType.ALBUMS) {
                val albumSql = """
                    SELECT album_id, album, artist, album_artist, MAX(year) as max_year, MAX(date_added) as max_date_added, album_art_uri_string, COUNT(*) as song_count
                    FROM songs WHERE album LIKE ? GROUP BY album, artist HAVING COUNT(*) >= ? LIMIT 20
                """.trimIndent()
                conn.prepareStatement(albumSql).use { stmt ->
                    stmt.setString(1, q)
                    stmt.setInt(2, minTracks)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(
                                SearchResultItem.AlbumItem(
                                    Album(
                                        id = rs.getLong("album_id"),
                                        title = rs.getString("album"),
                                        artist = rs.getString("artist"),
                                        albumArtist = rs.getString("album_artist"),
                                        year = rs.getInt("max_year"),
                                        dateAdded = rs.getLong("max_date_added"),
                                        albumArtUriString = rs.getString("album_art_uri_string"),
                                        songCount = rs.getInt("song_count")
                                    )
                                )
                            )
                        }
                    }
                }
            }

            if (filter == SearchFilterType.ALL || filter == SearchFilterType.ARTISTS) {
                val artistSql = """
                    SELECT a.id as artist_id, a.name as artist, COUNT(sa.song_id) as song_count, a.image_url, a.custom_image_uri
                    FROM artists a
                    JOIN song_artists sa ON a.id = sa.artist_id
                    WHERE a.name LIKE ?
                    GROUP BY a.id, a.name
                    LIMIT 20
                """.trimIndent()
                conn.prepareStatement(artistSql).use { stmt ->
                    stmt.setString(1, q)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(
                                SearchResultItem.ArtistItem(
                                    Artist(
                                        id = rs.getLong("artist_id"),
                                        name = rs.getString("artist"),
                                        songCount = rs.getInt("song_count"),
                                        imageUrl = rs.getString("image_url"),
                                        customImageUri = rs.getString("custom_image_uri")
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
        results
    }

    suspend fun clearLibrary() = withContext(ioDispatcher) {
        database.withConnection { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    stmt.execute("DELETE FROM song_artists")
                    stmt.execute("DELETE FROM artists")
                    stmt.execute("DELETE FROM songs")
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
        database.notifyTableChanged("songs")
    }
}

private fun ResultSet.toSong(json: Json): Song {
    val discNumber = getInt("disc_number")
    val wasNullDisc = wasNull()
    val bitrate = getInt("bitrate")
    val wasNullBitrate = wasNull()
    val sampleRate = getInt("sample_rate")
    val wasNullSampleRate = wasNull()

    val artistsJson = runCatching { getString("artists_json") }.getOrNull()
    val artists = if (!artistsJson.isNullOrBlank()) {
        runCatching {
            json.decodeFromString(
                artistRefListSerializer,
                artistsJson
            )
        }.getOrDefault(emptyList())
    } else {
        emptyList()
    }

    return Song(
        id = getString("id"),
        title = getString("title"),
        artist = getString("artist"),
        artistId = getLong("artist_id"),
        album = getString("album"),
        albumId = getLong("album_id"),
        albumArtist = getString("album_artist"),
        path = getString("path"),
        contentUriString = getString("content_uri_string"),
        albumArtUriString = getString("album_art_uri_string"),
        duration = getLong("duration"),
        genre = getString("genre"),
        lyrics = getString("lyrics"),
        isFavorite = getInt("is_favorite") == 1,
        trackNumber = getInt("track_number"),
        discNumber = if (wasNullDisc) null else discNumber,
        year = getInt("year"),
        dateAdded = getLong("date_added"),
        dateModified = getLong("date_modified"),
        mimeType = getString("mime_type"),
        bitrate = if (wasNullBitrate) null else bitrate,
        sampleRate = if (wasNullSampleRate) null else sampleRate,
        navidromeId = getString("navidrome_id"),
        jellyfinId = getString("jellyfin_id"),
        artists = artists,
        playCount = runCatching { getInt("play_count") }.getOrDefault(0),
        lastPlayedTimestamp = runCatching { getLong("last_played_timestamp") }.getOrDefault(0L)
    )
}
