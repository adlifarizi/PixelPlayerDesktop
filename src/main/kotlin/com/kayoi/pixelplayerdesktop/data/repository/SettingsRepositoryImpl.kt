package com.kayoi.pixelplayerdesktop.data.repository

import java.io.File
import kotlin.math.roundToInt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

import com.kayoi.pixelplayerdesktop.data.model.EqualizerPreset
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider

class SettingsRepositoryImpl(
    private val settingsFile: File = DesktopPathProvider.settingsFile,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : SettingsRepository {

    private val mutex = Mutex()
    private val _settingsFlow: MutableStateFlow<UserSettings>

    init {
        val initialSettings = loadSettingsFromDisk()
        _settingsFlow = MutableStateFlow(initialSettings)
    }

    override val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): UserSettings = settingsFlow.value

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        mutex.withLock {
            val current = _settingsFlow.value
            val updated = transform(current)
            if (current != updated) {
                _settingsFlow.value = updated
                saveSettingsToDisk(updated)
            }
        }
    }

    override suspend fun addMusicDirectory(path: String) {
        updateSettings { settings ->
            if (settings.musicDirectories.contains(path)) {
                settings
            } else {
                settings.copy(musicDirectories = settings.musicDirectories + path)
            }
        }
    }

    override suspend fun removeMusicDirectory(path: String) {
        updateSettings { settings ->
            settings.copy(musicDirectories = settings.musicDirectories.filter { it != path })
        }
    }

    override suspend fun addExcludedDirectory(path: String) {
        updateSettings { settings ->
            if (settings.excludedDirectories.contains(path)) {
                settings
            } else {
                settings.copy(excludedDirectories = settings.excludedDirectories + path)
            }
        }
    }

    override suspend fun removeExcludedDirectory(path: String) {
        updateSettings { settings ->
            settings.copy(excludedDirectories = settings.excludedDirectories.filter { it != path })
        }
    }

    override suspend fun setDynamicColoringEnabled(enabled: Boolean) {
        updateSettings { it.copy(isDynamicColorEnabled = enabled) }
    }

    override suspend fun setDarkMode(isDark: Boolean) {
        updateSettings { it.copy(isDarkMode = isDark) }
    }

    override suspend fun setUiScale(scale: Float) {
        val clamped = ((scale * 100).roundToInt() / 100f).coerceIn(0.70f, 2.00f)
        updateSettings { it.copy(uiScale = clamped) }
    }

    override suspend fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) {
        updateSettings { it.copy(albumArtPaletteStyle = style.storageKey) }
    }

    override suspend fun setAlbumArtColorAccuracy(accuracy: Int) {
        updateSettings { it.copy(albumArtColorAccuracy = AlbumArtColorAccuracy.clamp(accuracy)) }
    }

    override suspend fun setVolume(volume: Float) {
        updateSettings { it.copy(volume = volume.coerceIn(0.0f, 1.0f)) }
    }

    override suspend fun setMuted(muted: Boolean) {
        updateSettings { it.copy(isMuted = muted) }
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        updateSettings { it.copy(isShuffleEnabled = enabled) }
    }

    override suspend fun setRepeatMode(repeatMode: Int) {
        updateSettings { it.copy(repeatMode = repeatMode.coerceIn(0, 2)) }
    }

    override suspend fun setInitialSetupDone(done: Boolean) {
        updateSettings { it.copy(initialSetupDone = done) }
    }

    override suspend fun setFetchDeezerArtistImages(enabled: Boolean) {
        updateSettings { it.copy(fetchDeezerArtistImages = enabled) }
    }

    override suspend fun setFetchOnlineLyrics(enabled: Boolean) {
        updateSettings { it.copy(fetchOnlineLyrics = enabled) }
    }

    override suspend fun setArtistDelimiters(delimiters: List<String>) {
        updateSettings { it.copy(artistDelimiters = delimiters.distinct(), artistSettingsRescanRequired = true) }
    }

    override suspend fun addArtistDelimiter(delimiter: String) {
        val trimmed = delimiter.trim()
        if (trimmed.isEmpty()) return
        updateSettings { settings ->
            if (settings.artistDelimiters.contains(trimmed)) {
                settings
            } else {
                settings.copy(artistDelimiters = settings.artistDelimiters + trimmed, artistSettingsRescanRequired = true)
            }
        }
    }

    override suspend fun removeArtistDelimiter(delimiter: String) {
        updateSettings { settings ->
            if (settings.artistDelimiters.size <= 1) {
                settings
            } else {
                settings.copy(artistDelimiters = settings.artistDelimiters.filter { it != delimiter }, artistSettingsRescanRequired = true)
            }
        }
    }

    override suspend fun resetArtistDelimiters() {
        updateSettings { it.copy(artistDelimiters = com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS, artistSettingsRescanRequired = true) }
    }

    override suspend fun setWordDelimiters(delimiters: List<String>) {
        updateSettings { it.copy(wordDelimiters = delimiters.distinct(), artistSettingsRescanRequired = true) }
    }

    override suspend fun addWordDelimiter(delimiter: String) {
        val trimmed = delimiter.trim()
        if (trimmed.isEmpty()) return
        updateSettings { settings ->
            if (settings.wordDelimiters.any { it.equals(trimmed, ignoreCase = true) }) {
                settings
            } else {
                settings.copy(wordDelimiters = settings.wordDelimiters + trimmed, artistSettingsRescanRequired = true)
            }
        }
    }

    override suspend fun removeWordDelimiter(delimiter: String) {
        updateSettings { settings ->
            settings.copy(wordDelimiters = settings.wordDelimiters.filter { !it.equals(delimiter, ignoreCase = true) }, artistSettingsRescanRequired = true)
        }
    }

    override suspend fun resetWordDelimiters() {
        updateSettings { it.copy(wordDelimiters = com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS, artistSettingsRescanRequired = true) }
    }

    override suspend fun setExtractArtistFromTitle(enabled: Boolean) {
        updateSettings { it.copy(extractArtistFromTitle = enabled, artistSettingsRescanRequired = true) }
    }

    override suspend fun clearArtistSettingsRescanRequired() {
        updateSettings { it.copy(artistSettingsRescanRequired = false) }
    }

    override suspend fun setAlbumsListView(isListView: Boolean) {
        updateSettings { it.copy(isAlbumsListView = isListView) }
    }

    override suspend fun setArtistsListView(isListView: Boolean) {
        updateSettings { it.copy(isArtistsListView = isListView) }
    }

    override suspend fun setFoldersPlaylistView(isPlaylistView: Boolean) {
        updateSettings { it.copy(isFoldersPlaylistView = isPlaylistView) }
    }

    override suspend fun setMinSongDuration(durationMs: Int) {
        updateSettings { it.copy(minSongDurationMs = durationMs.coerceIn(0, 120000)) }
    }

    override suspend fun setMinTracksPerAlbum(minTracks: Int) {
        updateSettings { it.copy(minTracksPerAlbum = minTracks.coerceIn(1, 5)) }
    }

    override suspend fun setAlbumArtCacheLimitMb(limitMb: Int) {
        updateSettings { it.copy(albumArtCacheLimitMb = limitMb.coerceIn(50, 1500)) }
    }

    override suspend fun setAutoScanLrcFiles(enabled: Boolean) {
        updateSettings { it.copy(autoScanLrcFiles = enabled) }
    }

    override suspend fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        updateSettings { it.copy(lyricsSourcePreference = preference.storageKey) }
    }

    override suspend fun setShowPlayerFileInfo(show: Boolean) {
        updateSettings { it.copy(showPlayerFileInfo = show) }
    }

    override suspend fun setNavBarCompactMode(enabled: Boolean) {
        updateSettings { it.copy(navBarCompactMode = enabled) }
    }

    override suspend fun setDefaultTab(tab: String) {
        updateSettings { it.copy(defaultTab = tab) }
    }

    override suspend fun setLyricsAlignment(alignment: String) {
        updateSettings { it.copy(lyricsAlignment = alignment) }
    }

    override suspend fun setLyricsTextSize(size: String) {
        updateSettings { it.copy(lyricsTextSize = size) }
    }

    override suspend fun setLyricsSyncOffset(songId: String, offsetMs: Int) {
        updateSettings { current ->
            val updatedMap = current.lyricsSyncOffsets.toMutableMap()
            if (offsetMs == 0) {
                updatedMap.remove(songId)
            } else {
                updatedMap[songId] = offsetMs
            }
            current.copy(lyricsSyncOffsets = updatedMap)
        }
    }

    override suspend fun setShowLyricsRomanization(enabled: Boolean) {
        updateSettings { it.copy(showLyricsRomanization = enabled) }
    }

    override suspend fun setReplayGainEnabled(enabled: Boolean) {
        updateSettings { it.copy(replayGainEnabled = enabled) }
    }

    override suspend fun setReplayGainMode(mode: String) {
        updateSettings { it.copy(replayGainMode = mode) }
    }

    override suspend fun setCrossfadeEnabled(enabled: Boolean) {
        updateSettings { it.copy(crossfadeEnabled = enabled) }
    }

    override suspend fun setCrossfadeDurationSeconds(durationSeconds: Int) {
        updateSettings { it.copy(crossfadeDurationSeconds = durationSeconds.coerceIn(1, 12)) }
    }

    override suspend fun setAudioFadeOnPlayPause(enabled: Boolean) {
        updateSettings { it.copy(audioFadeOnPlayPause = enabled) }
    }

    override suspend fun setShowQueueHistory(show: Boolean) {
        updateSettings { it.copy(showQueueHistory = show) }
    }

    override suspend fun setCloseToTray(enabled: Boolean) {
        updateSettings { it.copy(closeToTray = enabled) }
    }

    override suspend fun setSelectedAudioDevice(device: String) {
        updateSettings { it.copy(selectedAudioDevice = device) }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        updateSettings { it.copy(equalizerEnabled = enabled) }
    }

    override suspend fun setEqualizerPreset(preset: String) {
        updateSettings { it.copy(equalizerPreset = preset) }
    }

    override suspend fun setEqualizerBands(bands: List<Float>) {
        updateSettings { it.copy(equalizerBands = bands) }
    }

    override suspend fun setEqualizerPreamp(preamp: Float) {
        updateSettings { it.copy(equalizerPreamp = preamp) }
    }

    override suspend fun saveCustomEqualizerPreset(preset: EqualizerPreset) {
        updateSettings { settings ->
            val updated = settings.customEqualizerPresets.filter { !it.name.equals(preset.name, ignoreCase = true) } + preset
            val updatedPinned = if (!settings.pinnedEqualizerPresets.contains(preset.name)) {
                settings.pinnedEqualizerPresets + preset.name
            } else {
                settings.pinnedEqualizerPresets
            }
            settings.copy(
                customEqualizerPresets = updated,
                pinnedEqualizerPresets = updatedPinned
            )
        }
    }

    override suspend fun deleteCustomEqualizerPreset(presetName: String) {
        updateSettings { settings ->
            val updated = settings.customEqualizerPresets.filter { !it.name.equals(presetName, ignoreCase = true) }
            val updatedPinned = settings.pinnedEqualizerPresets.filter { !it.equals(presetName, ignoreCase = true) }
            val newPresetName = if (settings.equalizerPreset.equals(presetName, ignoreCase = true)) "flat" else settings.equalizerPreset
            settings.copy(
                customEqualizerPresets = updated,
                pinnedEqualizerPresets = updatedPinned,
                equalizerPreset = newPresetName
            )
        }
    }

    override suspend fun renameCustomEqualizerPreset(oldName: String, newName: String) {
        updateSettings { settings ->
            val updated = settings.customEqualizerPresets.map {
                if (it.name.equals(oldName, ignoreCase = true)) {
                    it.copy(name = newName, displayName = newName)
                } else it
            }
            val updatedPinned = settings.pinnedEqualizerPresets.map {
                if (it.equals(oldName, ignoreCase = true)) newName else it
            }
            val newActive = if (settings.equalizerPreset.equals(oldName, ignoreCase = true)) newName else settings.equalizerPreset
            settings.copy(
                customEqualizerPresets = updated,
                pinnedEqualizerPresets = updatedPinned,
                equalizerPreset = newActive
            )
        }
    }

    override suspend fun setPinnedEqualizerPresets(pinnedNames: List<String>) {
        updateSettings { it.copy(pinnedEqualizerPresets = pinnedNames) }
    }

    override suspend fun setEqualizerViewMode(mode: String) {
        updateSettings { it.copy(equalizerViewMode = mode) }
    }

    override suspend fun setCheckUpdatesOnStartup(enabled: Boolean) {
        updateSettings { it.copy(checkUpdatesOnStartup = enabled) }
    }

    override suspend fun setLastDismissedUpdateVersion(version: String?) {
        updateSettings { it.copy(lastDismissedUpdateVersion = version) }
    }

    override suspend fun setQuickAccessItems(items: List<QuickAccessItem>) {
        updateSettings { it.copy(quickAccessItems = items) }
    }

    override suspend fun pinToQuickAccess(item: QuickAccessItem): Boolean {
        var success = false
        updateSettings { current ->
            if (current.quickAccessItems.any { it.id == item.id }) {
                success = true
                current.copy(
                    quickAccessItems = current.quickAccessItems.map {
                        if (it.id == item.id) item.copy(order = it.order, isEnabled = true) else it
                    }
                )
            } else {
                val customCount = current.quickAccessItems.count { it.type != QuickAccessType.SMART_PRESET }
                if (item.type != QuickAccessType.SMART_PRESET && customCount >= QuickAccessItem.MAX_PINNED_CUSTOM_ITEMS) {
                    success = false
                    current
                } else {
                    success = true
                    val nextOrder = (current.quickAccessItems.maxOfOrNull { it.order } ?: -1) + 1
                    current.copy(quickAccessItems = current.quickAccessItems + item.copy(order = nextOrder, isEnabled = true))
                }
            }
        }
        return success
    }

    override suspend fun unpinFromQuickAccess(id: String) {
        updateSettings { current ->
            current.copy(
                quickAccessItems = current.quickAccessItems.mapNotNull { itm ->
                    if (itm.id == id) {
                        if (itm.type == QuickAccessType.SMART_PRESET) {
                            itm.copy(isEnabled = false)
                        } else {
                            null
                        }
                    } else {
                        itm
                    }
                }
            )
        }
    }

    private fun loadSettingsFromDisk(): UserSettings {
        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText(Charsets.UTF_8)
                val loaded = json.decodeFromString(UserSettings.serializer(), content)
                loaded.copy(uiScale = loaded.uiScale.coerceIn(0.70f, 2.00f))
            } else {
                val defaultSettings = UserSettings(
                    musicDirectories = DesktopPathProvider.defaultMusicDirectories
                )
                saveSettingsToDiskSync(defaultSettings)
                defaultSettings
            }
        } catch (e: Exception) {
            UserSettings(musicDirectories = DesktopPathProvider.defaultMusicDirectories)
        }
    }

    private suspend fun saveSettingsToDisk(settings: UserSettings) = withContext(ioDispatcher) {
        saveSettingsToDiskSync(settings)
    }

    private fun saveSettingsToDiskSync(settings: UserSettings) {
        try {
            val parent = settingsFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val content = json.encodeToString(UserSettings.serializer(), settings)
            val tempFile = File(settingsFile.parentFile, "${settingsFile.name}.tmp")
            tempFile.writeText(content, Charsets.UTF_8)
            if (tempFile.exists()) {
                if (settingsFile.exists()) {
                    settingsFile.delete()
                }
                tempFile.renameTo(settingsFile)
            }
        } catch (e: Exception) {
            // Log or fallback
        }
    }
}
