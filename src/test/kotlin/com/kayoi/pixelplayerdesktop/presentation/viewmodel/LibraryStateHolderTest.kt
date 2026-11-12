package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import java.io.File

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStateHolderTest {

    @Test
    fun `songs and favorites filter out songs in excluded directories`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_lib_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_lib_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val allowedSong = Song(
            id = "song_allowed",
            title = "Allowed Track",
            artist = "Artist A",
            album = "Album A",
            path = File("/home/user/music/allowed/song.mp3").absolutePath,
            duration = 180000L,
            isFavorite = true
        )
        val excludedSong = Song(
            id = "song_excluded",
            title = "Excluded Track",
            artist = "Unknown Artist",
            album = "Unknown Album",
            path = File("/home/user/music/excluded/song.mp3").absolutePath,
            duration = 180000L,
            isFavorite = true
        )

        musicDao.upsertSongs(listOf(allowedSong, excludedSong))
        favDao.toggleFavorite("song_allowed")
        favDao.toggleFavorite("song_excluded")

        val directSongs = musicDao.getAllSongs()
        val directFavs = favDao.getFavoriteSongIds()
        println("DIRECT: songs=${directSongs.size}, favs=${directFavs.size}")

        val libraryStateHolder = LibraryStateHolder(
            musicRepository = musicRepo,
            settingsRepository = settingsRepo,
            scanner = scanner,
            coroutineScope = backgroundScope
        )

        val initialSongs = libraryStateHolder.songs.first { it.isNotEmpty() }
        val initialFavs = libraryStateHolder.favoriteSongs.first { it.isNotEmpty() }

        assertEquals(2, initialSongs.size)
        assertEquals(2, initialFavs.size)

        settingsRepo.addExcludedDirectory(File("/home/user/music/excluded").absolutePath)

        val filteredSongs = libraryStateHolder.songs.first { it.size == 1 }
        val filteredFavs = libraryStateHolder.favoriteSongs.first { it.size == 1 }

        assertEquals("Allowed Track", filteredSongs.first().title)
        assertEquals("Allowed Track", filteredFavs.first().title)
    }

    @Test
    fun `deletePlaylist automatically unpins playlist from quick access`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_del_pl_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_del_pl_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val pl = musicRepo.createPlaylist("Favorites Mix", emptyList())

        settingsRepo.pinToQuickAccess(
            QuickAccessItem(
                id = "playlist_${pl.id}",
                title = pl.name,
                type = QuickAccessType.PLAYLIST,
                targetKey = pl.id
            )
        )
        val pinnedItems = settingsRepo.getSettings().quickAccessItems
        assertEquals(4, pinnedItems.size)
        assertNotNull(pinnedItems.find { it.id == "playlist_${pl.id}" })

        val libraryStateHolder = LibraryStateHolder(
            musicRepository = musicRepo,
            settingsRepository = settingsRepo,
            scanner = scanner,
            coroutineScope = backgroundScope
        )

        libraryStateHolder.deletePlaylist(pl.id)

        val updatedSettings = settingsRepo.settingsFlow.first { it.quickAccessItems.size == 3 }
        assertEquals(3, updatedSettings.quickAccessItems.size)
        assertTrue(updatedSettings.quickAccessItems.none { it.id == "playlist_${pl.id}" })
    }

    @Test
    fun `renamePlaylist updates quick access item title`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_rename_pl_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_rename_pl_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val pl = musicRepo.createPlaylist("Old Name", emptyList())

        settingsRepo.pinToQuickAccess(
            QuickAccessItem(
                id = "playlist_${pl.id}",
                title = pl.name,
                type = QuickAccessType.PLAYLIST,
                targetKey = pl.id
            )
        )

        val libraryStateHolder = LibraryStateHolder(
            musicRepository = musicRepo,
            settingsRepository = settingsRepo,
            scanner = scanner,
            coroutineScope = backgroundScope
        )

        libraryStateHolder.renamePlaylist(pl.id, "New Name")

        val updatedSettings = settingsRepo.settingsFlow.first {
            it.quickAccessItems.any { item -> item.id == "playlist_${pl.id}" && item.title == "New Name" }
        }
        val item = updatedSettings.quickAccessItems.find { it.id == "playlist_${pl.id}" }
        assertNotNull(item)
        assertEquals("New Name", item?.title)
    }

    @Test
    fun `sanitizeQuickAccessItems prunes deleted playlist and updates renamed playlist`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_sanitize_pl_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_sanitize_pl_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val pl = musicRepo.createPlaylist("Real Playlist", emptyList())

        settingsRepo.pinToQuickAccess(
            QuickAccessItem(
                id = "playlist_${pl.id}",
                title = "Stale Name",
                type = QuickAccessType.PLAYLIST,
                targetKey = pl.id
            )
        )
        settingsRepo.pinToQuickAccess(
            QuickAccessItem(
                id = "playlist_deleted_123",
                title = "Deleted Playlist",
                type = QuickAccessType.PLAYLIST,
                targetKey = "deleted_123"
            )
        )

        val libraryStateHolder = LibraryStateHolder(
            musicRepository = musicRepo,
            settingsRepository = settingsRepo,
            scanner = scanner,
            coroutineScope = backgroundScope
        )

        libraryStateHolder.sanitizeQuickAccessItems()

        val updated = settingsRepo.getSettings().quickAccessItems
        assertTrue(updated.none { it.id == "playlist_deleted_123" })
        val realItem = updated.find { it.id == "playlist_${pl.id}" }
        assertNotNull(realItem)
        assertEquals("Real Playlist", realItem?.title)
    }
}
