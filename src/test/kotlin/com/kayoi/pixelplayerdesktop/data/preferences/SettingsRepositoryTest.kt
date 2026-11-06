package com.kayoi.pixelplayerdesktop.data.preferences

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl

class SettingsRepositoryTest {

    @Test
    fun testSettingsLoadAndSave() = runTest {
        val tempFile = File.createTempFile("test_settings", ".json")
        tempFile.deleteOnExit()

        val repository = SettingsRepositoryImpl(settingsFile = tempFile)

        // Initial check
        val initial = repository.getSettings()
        assertTrue(initial.isDynamicColorEnabled)
        assertTrue(initial.audioFadeOnPlayPause)
        assertEquals(1.0f, initial.uiScale)

        // Update settings
        repository.setDynamicColoringEnabled(false)
        repository.setAudioFadeOnPlayPause(false)
        repository.setVolume(0.75f)
        repository.setUiScale(1.25f)
        repository.addMusicDirectory("/test/music")
        repository.setAlbumsListView(true)
        repository.setArtistsListView(true)
        repository.setFoldersPlaylistView(true)
        repository.setLyricsAlignment("center")
        repository.setLyricsSyncOffset("song_123", 500)
        repository.setLyricsSyncOffset("song_456", -300)

        val updated = repository.getSettings()
        assertEquals(false, updated.isDynamicColorEnabled)
        assertEquals(false, updated.audioFadeOnPlayPause)
        assertEquals(0.75f, updated.volume)
        assertEquals(1.25f, updated.uiScale)
        assertTrue(updated.musicDirectories.contains("/test/music"))
        assertEquals(true, updated.isAlbumsListView)
        assertEquals(true, updated.isArtistsListView)
        assertEquals(true, updated.isFoldersPlaylistView)
        assertEquals("center", updated.lyricsAlignment)
        assertEquals(500, updated.lyricsSyncOffsets["song_123"])
        assertEquals(-300, updated.lyricsSyncOffsets["song_456"])

        // Reset offset to 0 should remove the entry
        repository.setLyricsSyncOffset("song_123", 0)
        val resetOffsets = repository.getSettings().lyricsSyncOffsets
        assertEquals(null, resetOffsets["song_123"])
        assertEquals(-300, resetOffsets["song_456"])

        repository.setUiScale(0.50f)
        assertEquals(0.70f, repository.getSettings().uiScale)
        repository.setUiScale(2.50f)
        assertEquals(2.00f, repository.getSettings().uiScale)
        repository.setUiScale(1.20f)

        // Create new repository instance from the same file to verify persistence
        val reloaded = SettingsRepositoryImpl(settingsFile = tempFile)
        val reloadedSettings = reloaded.getSettings()
        assertEquals(false, reloadedSettings.isDynamicColorEnabled)
        assertEquals(false, reloadedSettings.audioFadeOnPlayPause)
        assertEquals(0.75f, reloadedSettings.volume)
        assertEquals(1.20f, reloadedSettings.uiScale)
        assertTrue(reloadedSettings.musicDirectories.contains("/test/music"))
        assertEquals(true, reloadedSettings.isAlbumsListView)
        assertEquals(true, reloadedSettings.isArtistsListView)
        assertEquals(true, reloadedSettings.isFoldersPlaylistView)
        assertEquals("center", reloadedSettings.lyricsAlignment)
        assertEquals(-300, reloadedSettings.lyricsSyncOffsets["song_456"])
    }

    @Test
    fun testQuickAccessItems() = runTest {
        val tempFile = File.createTempFile("test_quick_access", ".json")
        tempFile.deleteOnExit()

        val repository = SettingsRepositoryImpl(settingsFile = tempFile)

        val initial = repository.getSettings().quickAccessItems
        assertEquals(3, initial.size)
        assertEquals(QuickAccessItem.PRESET_RECENTLY_ADDED, initial[0].id)

        val albumPin = QuickAccessItem(
            id = "album_123",
            title = "Abbey Road",
            type = QuickAccessType.ALBUM,
            targetKey = "123"
        )
        repository.pinToQuickAccess(albumPin)

        val withPin = repository.getSettings().quickAccessItems
        assertEquals(4, withPin.size)
        assertEquals(3, withPin.last().order)
        assertEquals("Abbey Road", withPin.last().title)

        repository.unpinFromQuickAccess("album_123")
        val afterUnpin = repository.getSettings().quickAccessItems
        assertEquals(3, afterUnpin.size)
        assertTrue(afterUnpin.none { it.id == "album_123" })

        repository.unpinFromQuickAccess(QuickAccessItem.PRESET_RECENTLY_ADDED)
        val afterPresetUnpin = repository.getSettings().quickAccessItems
        assertEquals(3, afterPresetUnpin.size)
        val preset = afterPresetUnpin.find { it.id == QuickAccessItem.PRESET_RECENTLY_ADDED }
        assertNotNull(preset)
        assertFalse(preset.isEnabled)

        val presetItem = QuickAccessItem.DEFAULT_ITEMS.first { it.id == QuickAccessItem.PRESET_RECENTLY_ADDED }
        repository.pinToQuickAccess(presetItem)
        val afterPresetRepin = repository.getSettings().quickAccessItems
        val repinnedPreset = afterPresetRepin.find { it.id == QuickAccessItem.PRESET_RECENTLY_ADDED }
        assertNotNull(repinnedPreset)
        assertTrue(repinnedPreset.isEnabled)

        assertTrue(repository.pinToQuickAccess(albumPin))
        val reloaded = SettingsRepositoryImpl(settingsFile = tempFile)
        val reloadedItems = reloaded.getSettings().quickAccessItems
        assertEquals(4, reloadedItems.size)
        assertEquals("Abbey Road", reloadedItems.last().title)

        val updatedAlbumPin = albumPin.copy(title = "Abbey Road (Remastered)")
        assertTrue(repository.pinToQuickAccess(updatedAlbumPin))
        assertEquals("Abbey Road (Remastered)", repository.getSettings().quickAccessItems.find { it.id == "album_123" }?.title)
    }

    @Test
    fun testMaxPinnedCustomItemsLimit() = runTest {
        val tempFile = File.createTempFile("test_limit_settings", ".json")
        tempFile.deleteOnExit()
        val repository = SettingsRepositoryImpl(settingsFile = tempFile)

        assertEquals(3, repository.getSettings().quickAccessItems.size)

        for (i in 1..10) {
            val result = repository.pinToQuickAccess(
                QuickAccessItem(
                    id = "album_$i",
                    title = "Album $i",
                    type = QuickAccessType.ALBUM,
                    targetKey = "$i"
                )
            )
            assertTrue(result)
        }
        assertEquals(13, repository.getSettings().quickAccessItems.size)

        val rejectedResult = repository.pinToQuickAccess(
            QuickAccessItem(
                id = "album_11",
                title = "Album 11",
                type = QuickAccessType.ALBUM,
                targetKey = "11"
            )
        )
        assertFalse(rejectedResult)
        assertEquals(13, repository.getSettings().quickAccessItems.size)
    }

    @Test
    fun testPlaylistPinAndUnpin() = runTest {
        val tempFile = File.createTempFile("test_playlist_pin", ".json")
        tempFile.deleteOnExit()
        val repository = SettingsRepositoryImpl(settingsFile = tempFile)

        val playlistPin = QuickAccessItem(
            id = "playlist_pl-99",
            title = "My Synthwave Mix",
            type = QuickAccessType.PLAYLIST,
            targetKey = "pl-99"
        )
        repository.pinToQuickAccess(playlistPin)

        val items = repository.getSettings().quickAccessItems
        assertEquals(4, items.size)
        val pinned = items.find { it.id == "playlist_pl-99" }
        assertNotNull(pinned)
        assertEquals("My Synthwave Mix", pinned.title)
        assertEquals(QuickAccessType.PLAYLIST, pinned.type)
        assertEquals("pl-99", pinned.targetKey)

        repository.unpinFromQuickAccess("playlist_pl-99")
        val itemsAfterUnpin = repository.getSettings().quickAccessItems
        assertEquals(3, itemsAfterUnpin.size)
        assertTrue(itemsAfterUnpin.none { it.id == "playlist_pl-99" })
    }
}
