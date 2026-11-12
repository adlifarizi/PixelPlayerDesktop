package com.kayoi.pixelplayerdesktop.domain.repository

import java.io.File
import kotlinx.coroutines.flow.Flow
import com.kayoi.pixelplayerdesktop.data.metadata.AudioMetadataDetails
import com.kayoi.pixelplayerdesktop.data.metadata.BatchMetadataResult
import com.kayoi.pixelplayerdesktop.data.metadata.BatchSongMetadataUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.metadata.MetadataEditResult
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Genre
import com.kayoi.pixelplayerdesktop.data.model.PlaybackEvent
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SearchFilterType
import com.kayoi.pixelplayerdesktop.data.model.SearchResultItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineTrackMetadata
import com.kayoi.pixelplayerdesktop.data.playlist.M3uParseResult
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS

interface MusicRepository {
    fun getAllSongsFlow(sortOption: SortOption = SortOption.SongTitleAZ, minDurationMs: Long = 0L): Flow<List<Song>>
    suspend fun getAllSongs(sortOption: SortOption = SortOption.SongTitleAZ, minDurationMs: Long = 0L): List<Song>
    suspend fun hasSongs(): Boolean
    suspend fun getSongById(id: String): Song?
    suspend fun getSongsByIds(ids: List<String>): List<Song>
    suspend fun getSongByPath(path: String): Song?
    suspend fun deleteSong(song: Song)
    suspend fun deleteSongsInDirectory(directoryPath: String)
    suspend fun clearLibrary()

    fun getAllAlbumsFlow(sortOption: SortOption = SortOption.AlbumTitleAZ, minTracks: Int = 1, minDurationMs: Long = 0L): Flow<List<Album>>
    suspend fun getAllAlbums(sortOption: SortOption = SortOption.AlbumTitleAZ, minTracks: Int = 1, minDurationMs: Long = 0L): List<Album>
    suspend fun getAlbumById(id: Long, minDurationMs: Long = 0L): Album?
    suspend fun getSongsByAlbum(albumId: Long, minDurationMs: Long = 0L): List<Song>

    fun getAllArtistsFlow(sortOption: SortOption = SortOption.ArtistNameAZ, minDurationMs: Long = 0L): Flow<List<Artist>>
    suspend fun getAllArtists(sortOption: SortOption = SortOption.ArtistNameAZ, minDurationMs: Long = 0L): List<Artist>
    suspend fun getArtistById(id: Long, minDurationMs: Long = 0L): Artist?
    suspend fun getSongsByArtist(artistId: Long, minDurationMs: Long = 0L): List<Song>
    suspend fun setCustomArtistImage(artistId: Long, sourceFile: File): String?
    suspend fun clearCustomArtistImage(artistId: Long)

    fun getAllGenresFlow(): Flow<List<Genre>>
    suspend fun getSongsByGenre(genreName: String): List<Song>

    fun getAllPlaylistsFlow(sortOption: SortOption = SortOption.PlaylistNameAZ): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: String): Playlist?
    suspend fun createPlaylist(
        name: String,
        songIds: List<String> = emptyList(),
        coverImageUri: String? = null,
        coverColorArgb: Int? = null,
        coverIconName: String? = null,
        coverShapeType: String? = null
    ): Playlist
    suspend fun savePlaylistCoverImage(sourceFile: File): String?
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(id: String)
    suspend fun addSongToPlaylist(playlistId: String, songId: String)
    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    suspend fun reorderPlaylistSongs(playlistId: String, songIds: List<String>)
    suspend fun importPlaylistFromM3u(file: File): Result<Pair<Playlist, M3uParseResult>>
    suspend fun exportPlaylistToM3u(playlistId: String, targetFile: File): Result<Unit>

    fun getFavoriteSongsFlow(): Flow<List<Song>>
    suspend fun isFavorite(songId: String): Boolean
    suspend fun toggleFavorite(songId: String): Boolean
    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    fun getRecentlyPlayedSongsFlow(limit: Int = 500): Flow<List<Song>>
    suspend fun recordSongPlayed(songId: String, durationMs: Long = 0L)
    fun getPlaybackEventsFlow(limit: Int = 500): Flow<List<PlaybackEvent>>
    suspend fun getPlaybackEvents(limit: Int = 500): List<PlaybackEvent>
    suspend fun getPlaybackEventsBetween(start: Long, end: Long): List<PlaybackEvent>

    suspend fun search(
        query: String,
        filter: SearchFilterType = SearchFilterType.ALL,
        minDurationMs: Long = 0L,
        minTracks: Int = 1
    ): List<SearchResultItem>

    suspend fun updateSongMetadata(
        song: Song,
        metadata: EditableSongMetadata,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true
    ): MetadataEditResult

    suspend fun updateSongLyrics(song: Song, lyricsText: String): MetadataEditResult

    suspend fun readSongMetadataDetails(file: File): Result<AudioMetadataDetails>
    suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchMetadataResult

    suspend fun searchOnlineMetadata(title: String, artist: String): List<OnlineTrackMetadata>
    suspend fun searchOnlineMetadataByQuery(query: String): List<OnlineTrackMetadata>
    suspend fun searchOnlineReleases(query: String): List<OnlineReleaseMetadata>
    suspend fun fetchOnlineCoverArt(releaseId: String): Result<ByteArray?>
}
