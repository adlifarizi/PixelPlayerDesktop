package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import java.io.File

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.kayoi.pixelplayerdesktop.data.metadata.AudioMetadataDetails
import com.kayoi.pixelplayerdesktop.data.metadata.BatchMetadataResult
import com.kayoi.pixelplayerdesktop.data.metadata.BatchSongMetadataUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.metadata.MetadataEditResult
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.model.SearchResultItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineTrackMetadata
import com.kayoi.pixelplayerdesktop.data.playlist.M3uParseResult
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress
import com.kayoi.pixelplayerdesktop.data.watcher.LibraryWatcherService
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.AlbumArtCacheManager
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DirectoryFilterUtils

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStateHolder(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val scanner: DesktopMusicScanner,
    private val lyricsRepository: LyricsRepository? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val watcherService: LibraryWatcherService? = null
) {

    init {
        watcherService?.start()
    }

    private val _storageFilter = MutableStateFlow(StorageFilter.ALL)
    val storageFilter: StateFlow<StorageFilter> = _storageFilter.asStateFlow()

    fun setStorageFilter(filter: StorageFilter) {
        _storageFilter.value = filter
    }

    fun toggleStorageFilter() {
        _storageFilter.value = when (_storageFilter.value) {
            StorageFilter.ALL -> StorageFilter.ONLINE
            StorageFilter.ONLINE -> StorageFilter.OFFLINE
            StorageFilter.OFFLINE -> StorageFilter.ALL
        }
    }

    private val _songSortOption = MutableStateFlow<SortOption>(SortOption.SongTitleAZ)
    val songSortOption: StateFlow<SortOption> = _songSortOption.asStateFlow()

    private val _albumSortOption = MutableStateFlow<SortOption>(SortOption.AlbumTitleAZ)
    val albumSortOption: StateFlow<SortOption> = _albumSortOption.asStateFlow()

    private val _artistSortOption = MutableStateFlow<SortOption>(SortOption.ArtistNameAZ)
    val artistSortOption: StateFlow<SortOption> = _artistSortOption.asStateFlow()

    private val _playlistSortOption = MutableStateFlow<SortOption>(SortOption.PlaylistNameAZ)
    val playlistSortOption: StateFlow<SortOption> = _playlistSortOption.asStateFlow()

    private val _folderSortOption = MutableStateFlow<SortOption>(SortOption.FolderNameAZ)
    val folderSortOption: StateFlow<SortOption> = _folderSortOption.asStateFlow()

    val initialSetupDone: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.initialSetupDone }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val fetchDeezerArtistImages: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.fetchDeezerArtistImages }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val fetchOnlineLyrics: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.fetchOnlineLyrics }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val autoScanLrcFiles: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.autoScanLrcFiles }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val lyricsSourcePreference: StateFlow<LyricsSourcePreference> = settingsRepository.settingsFlow
        .map { LyricsSourcePreference.fromStorageKey(it.lyricsSourcePreference) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, LyricsSourcePreference.EMBEDDED_FIRST)

    val musicDirectories: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.musicDirectories }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val excludedDirectories: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.excludedDirectories }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val artistDelimiters: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.artistDelimiters }
        .stateIn(coroutineScope, SharingStarted.Eagerly, DEFAULT_ARTIST_DELIMITERS)

    val wordDelimiters: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.wordDelimiters }
        .stateIn(coroutineScope, SharingStarted.Eagerly, DEFAULT_WORD_DELIMITERS)

    val extractArtistFromTitle: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.extractArtistFromTitle }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val isAlbumsListView: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.isAlbumsListView }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val isArtistsListView: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.isArtistsListView }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val isFoldersPlaylistView: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.isFoldersPlaylistView }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val quickAccessItems: StateFlow<List<QuickAccessItem>> = settingsRepository.settingsFlow
        .map { it.quickAccessItems }
        .stateIn(coroutineScope, SharingStarted.Eagerly, QuickAccessItem.DEFAULT_ITEMS)

    val rescanRequired: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.artistSettingsRescanRequired }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val minSongDurationMs: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.minSongDurationMs }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 10000)

    val minTracksPerAlbum: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.minTracksPerAlbum }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 1)

    val albumArtCacheLimitMb: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.albumArtCacheLimitMb }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 200)

    val songs: StateFlow<List<Song>> = combine(
        _songSortOption,
        minSongDurationMs,
        excludedDirectories
    ) { sort, minDur, excluded ->
        Triple(sort, minDur, excluded)
    }.flatMapLatest { (sort, minDur, excluded) ->
        val prefixes = DirectoryFilterUtils.normalizePrefixes(excluded)
        musicRepository.getAllSongsFlow(sort, minDur.toLong()).map { songsList ->
            if (prefixes.isEmpty()) {
                songsList
            } else {
                songsList.filterNot { song ->
                    DirectoryFilterUtils.isPathExcluded(song.path, prefixes)
                }
            }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<List<Album>> = combine(
        _albumSortOption,
        minTracksPerAlbum,
        minSongDurationMs
    ) { sort, minTracks, minDur ->
        Triple(sort, minTracks, minDur)
    }.flatMapLatest { (sort, minTracks, minDur) ->
        musicRepository.getAllAlbumsFlow(sort, minTracks, minDur.toLong())
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val artists: StateFlow<List<Artist>> = combine(
        _artistSortOption,
        minSongDurationMs
    ) { sort, minDur ->
        sort to minDur
    }.flatMapLatest { (sort, minDur) ->
        musicRepository.getAllArtistsFlow(sort, minDur.toLong())
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = _playlistSortOption.flatMapLatest { sort ->
        musicRepository.getAllPlaylistsFlow(sort)
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val favoriteSongs: StateFlow<List<Song>> = combine(
        musicRepository.getFavoriteSongsFlow(),
        excludedDirectories
    ) { list, excluded ->
        if (excluded.isEmpty()) list else {
            val prefixes = DirectoryFilterUtils.normalizePrefixes(excluded)
            list.filterNot { DirectoryFilterUtils.isPathExcluded(it.path, prefixes) }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = combine(
        musicRepository.getRecentlyPlayedSongsFlow(),
        excludedDirectories
    ) { list, excluded ->
        if (excluded.isEmpty()) list else {
            val prefixes = DirectoryFilterUtils.normalizePrefixes(excluded)
            list.filterNot { DirectoryFilterUtils.isPathExcluded(it.path, prefixes) }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val scanProgress: StateFlow<ScanProgress> = scanner.scanProgress

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val songSearchStateHolder = SongSearchStateHolder(
        songsFlow = songs,
        storageFilterFlow = storageFilter,
        searchQueryFlow = _searchQuery,
        coroutineScope = coroutineScope
    )

    val favoriteSearchStateHolder = SongSearchStateHolder(
        songsFlow = favoriteSongs,
        storageFilterFlow = storageFilter,
        searchQueryFlow = _searchQuery,
        coroutineScope = coroutineScope
    )

    val filteredSongs: StateFlow<List<Song>> = songSearchStateHolder.filteredSongs
    val filteredFavoriteSongs: StateFlow<List<Song>> = favoriteSearchStateHolder.filteredSongs

    val albumSearchStateHolder = AlbumSearchStateHolder(
        albumsFlow = albums,
        songsFlow = songs,
        storageFilterFlow = storageFilter,
        searchQueryFlow = _searchQuery,
        coroutineScope = coroutineScope
    )

    val artistSearchStateHolder = ArtistSearchStateHolder(
        artistsFlow = artists,
        songsFlow = songs,
        storageFilterFlow = storageFilter,
        searchQueryFlow = _searchQuery,
        coroutineScope = coroutineScope
    )

    val filteredAlbums: StateFlow<List<Album>> = albumSearchStateHolder.filteredAlbums
    val filteredArtists: StateFlow<List<Artist>> = artistSearchStateHolder.filteredArtists

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    fun setSongSortOption(sort: SortOption) {
        _songSortOption.value = sort
    }

    fun setAlbumSortOption(sort: SortOption) {
        _albumSortOption.value = sort
    }

    fun setArtistSortOption(sort: SortOption) {
        _artistSortOption.value = sort
    }

    fun setPlaylistSortOption(sort: SortOption) {
        _playlistSortOption.value = sort
    }

    fun setFolderSortOption(sort: SortOption) {
        _folderSortOption.value = sort
    }

    fun setAlbumsListView(isListView: Boolean) {
        coroutineScope.launch {
            settingsRepository.setAlbumsListView(isListView)
        }
    }

    fun setArtistsListView(isListView: Boolean) {
        coroutineScope.launch {
            settingsRepository.setArtistsListView(isListView)
        }
    }

    fun setFoldersPlaylistView(isPlaylistView: Boolean) {
        coroutineScope.launch {
            settingsRepository.setFoldersPlaylistView(isPlaylistView)
        }
    }

    fun setInitialSetupDone(done: Boolean) {
        coroutineScope.launch {
            settingsRepository.setInitialSetupDone(done)
        }
    }

    fun setFetchDeezerArtistImages(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setFetchDeezerArtistImages(enabled)
        }
    }

    fun toggleFetchDeezerArtistImages() {
        setFetchDeezerArtistImages(!fetchDeezerArtistImages.value)
    }

    fun setFetchOnlineLyrics(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setFetchOnlineLyrics(enabled)
        }
    }

    fun toggleFetchOnlineLyrics() {
        setFetchOnlineLyrics(!fetchOnlineLyrics.value)
    }

    fun addMusicDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.addMusicDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = false
            )
        }
    }

    fun removeMusicDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.removeMusicDirectory(path)
            musicRepository.deleteSongsInDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = true
            )
        }
    }

    fun addExcludedDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.addExcludedDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = true
            )
        }
    }

    fun removeExcludedDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.removeExcludedDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = false
            )
        }
    }

    fun scanLibrary(directories: List<String>? = null, forceRescan: Boolean = false) {
        coroutineScope.launch {
            val settings = settingsRepository.getSettings()
            val dirs = directories ?: settings.musicDirectories
            scanner.scanDirectories(
                directories = dirs,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                autoScanLrcFiles = settings.autoScanLrcFiles,
                pruneMissing = true,
                forceRescan = forceRescan
            )
            if (forceRescan) {
                settingsRepository.clearArtistSettingsRescanRequired()
            }
            AlbumArtCacheManager.cleanCacheIfNeeded(settings.albumArtCacheLimitMb.toLong())
        }
    }

    fun rebuildDatabase() {
        coroutineScope.launch {
            musicRepository.clearLibrary()
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                autoScanLrcFiles = settings.autoScanLrcFiles,
                pruneMissing = false,
                forceRescan = true
            )
            settingsRepository.clearArtistSettingsRescanRequired()
            AlbumArtCacheManager.cleanCacheIfNeeded(settings.albumArtCacheLimitMb.toLong(), force = true)
        }
    }

    fun setAutoScanLrcFiles(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setAutoScanLrcFiles(enabled)
        }
    }

    fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        coroutineScope.launch {
            settingsRepository.setLyricsSourcePreference(preference)
        }
    }

    fun resetAllLyrics() {
        coroutineScope.launch {
            lyricsRepository?.resetAllLyrics()
        }
    }

    fun setMinSongDuration(durationMs: Int) {
        coroutineScope.launch {
            settingsRepository.setMinSongDuration(durationMs)
        }
    }

    fun setMinTracksPerAlbum(minTracks: Int) {
        coroutineScope.launch {
            settingsRepository.setMinTracksPerAlbum(minTracks)
        }
    }

    fun setAlbumArtCacheLimitMb(limitMb: Int) {
        coroutineScope.launch {
            settingsRepository.setAlbumArtCacheLimitMb(limitMb)
            AlbumArtCacheManager.cleanCacheIfNeeded(limitMb.toLong(), force = true)
        }
    }

    fun addArtistDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.addArtistDelimiter(delimiter)
        }
    }

    fun removeArtistDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.removeArtistDelimiter(delimiter)
        }
    }

    fun resetArtistDelimiters() {
        coroutineScope.launch {
            settingsRepository.resetArtistDelimiters()
        }
    }

    fun addWordDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.addWordDelimiter(delimiter)
        }
    }

    fun removeWordDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.removeWordDelimiter(delimiter)
        }
    }

    fun resetWordDelimiters() {
        coroutineScope.launch {
            settingsRepository.resetWordDelimiters()
        }
    }

    fun setExtractArtistFromTitle(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setExtractArtistFromTitle(enabled)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        coroutineScope.launch {
            if (query.isNotBlank()) {
                val results = musicRepository.search(
                    query = query,
                    minDurationMs = minSongDurationMs.value.toLong(),
                    minTracks = minTracksPerAlbum.value
                )
                val excluded = excludedDirectories.value
                val prefixes = DirectoryFilterUtils.normalizePrefixes(excluded)
                _searchResults.value = if (prefixes.isEmpty()) {
                    results
                } else {
                    results.filterNot { item ->
                        item is SearchResultItem.SongItem && DirectoryFilterUtils.isPathExcluded(item.song.path, prefixes)
                    }
                }
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun createPlaylist(
        name: String,
        songIds: List<String> = emptyList(),
        coverImageUri: String? = null,
        coverColorArgb: Int? = null,
        coverIconName: String? = null,
        coverShapeType: String? = null
    ) {
        coroutineScope.launch {
            musicRepository.createPlaylist(name, songIds, coverImageUri, coverColorArgb, coverIconName, coverShapeType)
        }
    }

    suspend fun savePlaylistCoverImage(sourceFile: File): String? {
        return musicRepository.savePlaylistCoverImage(sourceFile)
    }

    fun deletePlaylist(id: String) {
        coroutineScope.launch {
            musicRepository.deletePlaylist(id)
            settingsRepository.unpinFromQuickAccess("playlist_$id")
        }
    }

    fun deleteSong(song: Song) {
        coroutineScope.launch {
            musicRepository.deleteSong(song)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        coroutineScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    suspend fun setCustomArtistImage(artistId: Long, sourceFile: File): String? {
        return musicRepository.setCustomArtistImage(artistId, sourceFile)
    }

    suspend fun clearCustomArtistImage(artistId: Long) {
        musicRepository.clearCustomArtistImage(artistId)
    }

    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        coroutineScope.launch {
            musicRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        coroutineScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>) {
        coroutineScope.launch {
            musicRepository.reorderPlaylistSongs(playlistId, songIds)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        coroutineScope.launch {
            val existing = musicRepository.getPlaylistById(id) ?: return@launch
            musicRepository.updatePlaylist(existing.copy(name = newName))
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        coroutineScope.launch {
            musicRepository.updatePlaylist(playlist)
        }
    }

    fun importPlaylistFromM3u(file: File, onComplete: ((Result<Pair<Playlist, M3uParseResult>>) -> Unit)? = null) {
        coroutineScope.launch {
            val result = musicRepository.importPlaylistFromM3u(file)
            onComplete?.invoke(result)
        }
    }

    fun exportPlaylistToM3u(playlistId: String, targetFile: File, onComplete: ((Result<Unit>) -> Unit)? = null) {
        coroutineScope.launch {
            val result = musicRepository.exportPlaylistToM3u(playlistId, targetFile)
            onComplete?.invoke(result)
        }
    }

    fun toggleFavorite(song: Song) {
        coroutineScope.launch {
            musicRepository.toggleFavorite(song.id)
        }
    }

    suspend fun updateSongMetadata(
        song: Song,
        metadata: EditableSongMetadata
    ): MetadataEditResult {
        val result = musicRepository.updateSongMetadata(
            song = song,
            metadata = metadata,
            artistDelimiters = artistDelimiters.value,
            wordDelimiters = wordDelimiters.value,
            extractArtistFromTitle = extractArtistFromTitle.value
        )
        if (result.success && result.updatedSong != null) {
            if (_searchQuery.value.isNotBlank()) {
                onSearchQueryChanged(_searchQuery.value)
            }
        }
        return result
    }

    suspend fun readSongMetadataDetails(file: File): Result<AudioMetadataDetails> {
        return musicRepository.readSongMetadataDetails(file)
    }

    suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchMetadataResult {
        val result = musicRepository.batchUpdateMetadata(
            songs = songs,
            update = update,
            artistDelimiters = artistDelimiters.value,
            wordDelimiters = wordDelimiters.value,
            extractArtistFromTitle = extractArtistFromTitle.value,
            onProgress = onProgress
        )
        if (result.successCount > 0 && _searchQuery.value.isNotBlank()) {
            onSearchQueryChanged(_searchQuery.value)
        }
        return result
    }

    suspend fun searchOnlineMetadata(title: String, artist: String): List<OnlineTrackMetadata> {
        return musicRepository.searchOnlineMetadata(title, artist)
    }

    suspend fun searchOnlineMetadataByQuery(query: String): List<OnlineTrackMetadata> {
        return musicRepository.searchOnlineMetadataByQuery(query)
    }

    suspend fun searchOnlineReleases(query: String): List<OnlineReleaseMetadata> {
        return musicRepository.searchOnlineReleases(query)
    }

    suspend fun fetchOnlineCoverArt(releaseId: String): Result<ByteArray?> {
        return musicRepository.fetchOnlineCoverArt(releaseId)
    }

    fun pinToQuickAccess(item: QuickAccessItem, onResult: ((Boolean) -> Unit)? = null) {
        coroutineScope.launch {
            val success = settingsRepository.pinToQuickAccess(item)
            onResult?.invoke(success)
        }
    }

    fun unpinFromQuickAccess(id: String) {
        coroutineScope.launch {
            settingsRepository.unpinFromQuickAccess(id)
        }
    }

    fun setQuickAccessItems(items: List<QuickAccessItem>) {
        coroutineScope.launch {
            settingsRepository.setQuickAccessItems(items)
        }
    }

    fun togglePin(
        id: String,
        title: String,
        type: QuickAccessType,
        targetKey: String,
        isPinned: Boolean,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (isPinned) {
            unpinFromQuickAccess(id)
            onResult?.invoke(true)
        } else {
            pinToQuickAccess(
                QuickAccessItem(
                    id = id,
                    title = title,
                    type = type,
                    targetKey = targetKey
                ),
                onResult = onResult
            )
        }
    }
}
