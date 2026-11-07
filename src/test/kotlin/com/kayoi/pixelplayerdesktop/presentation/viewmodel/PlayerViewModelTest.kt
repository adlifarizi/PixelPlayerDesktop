package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.data.player.mpv.MpvAudioEngine
import com.kayoi.pixelplayerdesktop.data.repository.LyricsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @Test
    fun testPlayerViewModelOrchestration() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_vm_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_vm_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)
        val lyricsDao = LyricsDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val lyricsRepo = LyricsRepositoryImpl(lyricsDao, settingsRepository = settingsRepo, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val audioEngine = MpvAudioEngine(coroutineScope = backgroundScope)
        val queueManager = QueueManager()

        val testScope = CoroutineScope(backgroundScope.coroutineContext + testDispatcher)
        val playbackStateHolder = PlaybackStateHolder(audioEngine, queueManager, musicRepo, lyricsRepo, settingsRepo, coroutineScope = testScope)
        val libraryStateHolder = LibraryStateHolder(musicRepo, settingsRepo, scanner, coroutineScope = backgroundScope)
        val themeStateHolder = ThemeStateHolder(settingsRepo, coroutineScope = backgroundScope)
        val statsStateHolder = StatsStateHolder(musicRepo, coroutineScope = backgroundScope)

        val viewModel = PlayerViewModel(playbackStateHolder, libraryStateHolder, themeStateHolder, statsStateHolder)

        val song = Song(
            id = "s1",
            title = "Test Song",
            artist = "Artist",
            path = "/tmp/test.mp3",
            duration = 200000L
        )

        musicDao.upsertSongs(listOf(song))
        viewModel.playSong(song)

        assertEquals("Test Song", viewModel.currentSong.value?.title)

        viewModel.onSearchQueryChanged("Test")
        assertEquals("Test", viewModel.searchQuery.value)
    }

    @Test
    fun testPlaybackSpeedOrchestration() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_vm_speed_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_vm_speed_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)
        val lyricsDao = LyricsDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val lyricsRepo = LyricsRepositoryImpl(lyricsDao, settingsRepository = settingsRepo, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val audioEngine = MpvAudioEngine(coroutineScope = backgroundScope)
        val queueManager = QueueManager()

        val testScope = CoroutineScope(backgroundScope.coroutineContext + testDispatcher)
        val playbackStateHolder = PlaybackStateHolder(audioEngine, queueManager, musicRepo, lyricsRepo, settingsRepo, coroutineScope = testScope)
        val libraryStateHolder = LibraryStateHolder(musicRepo, settingsRepo, scanner, coroutineScope = backgroundScope)
        val themeStateHolder = ThemeStateHolder(settingsRepo, coroutineScope = backgroundScope)
        val statsStateHolder = StatsStateHolder(musicRepo, coroutineScope = backgroundScope)

        val viewModel = PlayerViewModel(playbackStateHolder, libraryStateHolder, themeStateHolder, statsStateHolder)

        val song1 = Song(id = "s1", title = "Song 1", artist = "Artist", path = "/tmp/song1.mp3", duration = 100000L)
        val song2 = Song(id = "s2", title = "Song 2", artist = "Artist", path = "/tmp/song2.mp3", duration = 100000L)
        musicDao.upsertSongs(listOf(song1, song2))

        assertEquals(1.0f, viewModel.playbackSpeed.value)
        assertFalse(viewModel.isCurrentSongSpeedSaved.value)

        viewModel.playSong(song1)
        assertEquals("Song 1", viewModel.currentSong.value?.title)

        viewModel.setPlaybackSpeed(1.25f)
        assertEquals(1.25f, viewModel.playbackSpeed.value)
        assertFalse(viewModel.isCurrentSongSpeedSaved.value)

        viewModel.setRememberSpeedForCurrentSong(true)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.isCurrentSongSpeedSaved.value)
        assertEquals(1, viewModel.savedSongSpeedsCount.value)
        assertEquals(1.25f, settingsRepo.getSettings().songPlaybackSpeeds["s1"])

        viewModel.playSong(song2)
        testScheduler.advanceUntilIdle()
        assertEquals(1.0f, viewModel.playbackSpeed.value)
        assertFalse(viewModel.isCurrentSongSpeedSaved.value)

        viewModel.playSong(song1)
        testScheduler.advanceUntilIdle()
        assertEquals(1.25f, viewModel.playbackSpeed.value)
        assertTrue(viewModel.isCurrentSongSpeedSaved.value)

        viewModel.resetPlaybackSpeed()
        testScheduler.advanceUntilIdle()
        assertEquals(1.0f, viewModel.playbackSpeed.value)
        assertFalse(viewModel.isCurrentSongSpeedSaved.value)
        assertEquals(0, viewModel.savedSongSpeedsCount.value)
    }
}
