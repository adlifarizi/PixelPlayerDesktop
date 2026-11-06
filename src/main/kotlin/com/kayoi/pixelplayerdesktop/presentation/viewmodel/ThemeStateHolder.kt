package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import kotlin.math.roundToInt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor

class ThemeStateHolder(
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    init {
        coroutineScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val style = AlbumArtPaletteStyle.fromStorageKey(settings.albumArtPaletteStyle)
                val acc = AlbumArtColorAccuracy.clamp(settings.albumArtColorAccuracy)
                if (DynamicColorProcessor.defaultPaletteStyle != style || DynamicColorProcessor.defaultColorAccuracyLevel != acc) {
                    DynamicColorProcessor.defaultPaletteStyle = style
                    DynamicColorProcessor.defaultColorAccuracyLevel = acc
                    DynamicColorProcessor.clearCache()
                }
            }
        }
    }

    val isDarkMode: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.isDarkMode }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val uiScale: StateFlow<Float> = settingsRepository.settingsFlow
        .map { it.uiScale }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 1.0f)

    fun setUiScale(scale: Float) {
        coroutineScope.launch {
            settingsRepository.setUiScale(scale)
        }
    }

    private var lastScaleAdjustTimestamp = 0L

    fun adjustUiScale(delta: Float) {
        val now = System.currentTimeMillis()
        if (now - lastScaleAdjustTimestamp < 120L) return
        lastScaleAdjustTimestamp = now
        val target = ((uiScale.value + delta) * 10).roundToInt() / 10f
        setUiScale(target)
    }

    fun resetUiScale() {
        setUiScale(1.0f)
    }

    val isDynamicColorEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.isDynamicColorEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val lastKnownAccentColor: StateFlow<Int?> = settingsRepository.settingsFlow
        .map { it.lastKnownAccentColor }
        .stateIn(coroutineScope, SharingStarted.Eagerly, settingsRepository.settingsFlow.value.lastKnownAccentColor)

    fun updateLastKnownAccentColor(colorArgb: Int) {
        coroutineScope.launch {
            settingsRepository.updateSettings { it.copy(lastKnownAccentColor = colorArgb) }
        }
    }

    val albumArtPaletteStyle: StateFlow<AlbumArtPaletteStyle> = settingsRepository.settingsFlow
        .map { AlbumArtPaletteStyle.fromStorageKey(it.albumArtPaletteStyle) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, AlbumArtPaletteStyle.default)

    val albumArtColorAccuracy: StateFlow<Int> = settingsRepository.settingsFlow
        .map { AlbumArtColorAccuracy.clamp(it.albumArtColorAccuracy) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, AlbumArtColorAccuracy.DEFAULT)

    val showPlayerFileInfo: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.showPlayerFileInfo }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val navBarCompactMode: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.navBarCompactMode }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val defaultTab: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.defaultTab }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "HOME")

    val lyricsAlignment: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.lyricsAlignment }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "left")

    val lyricsTextSize: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.lyricsTextSize }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "medium")

    val showLyricsRomanization: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.showLyricsRomanization }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    fun setShowLyricsRomanization(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setShowLyricsRomanization(enabled)
        }
    }

    fun setDarkMode(dark: Boolean) {
        coroutineScope.launch {
            settingsRepository.setDarkMode(dark)
        }
    }

    fun toggleDarkMode() {
        setDarkMode(!isDarkMode.value)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setDynamicColoringEnabled(enabled)
        }
    }

    fun toggleDynamicColor() {
        setDynamicColorEnabled(!isDynamicColorEnabled.value)
    }

    fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) {
        coroutineScope.launch {
            DynamicColorProcessor.defaultPaletteStyle = style
            DynamicColorProcessor.clearCache()
            settingsRepository.setAlbumArtPaletteStyle(style)
        }
    }

    fun setAlbumArtColorAccuracy(accuracy: Int) {
        coroutineScope.launch {
            DynamicColorProcessor.defaultColorAccuracyLevel = accuracy
            DynamicColorProcessor.clearCache()
            settingsRepository.setAlbumArtColorAccuracy(accuracy)
        }
    }

    fun setShowPlayerFileInfo(show: Boolean) {
        coroutineScope.launch {
            settingsRepository.setShowPlayerFileInfo(show)
        }
    }

    fun toggleShowPlayerFileInfo() {
        setShowPlayerFileInfo(!showPlayerFileInfo.value)
    }

    fun setNavBarCompactMode(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setNavBarCompactMode(enabled)
        }
    }

    fun toggleNavBarCompactMode() {
        setNavBarCompactMode(!navBarCompactMode.value)
    }

    fun setDefaultTab(tab: String) {
        coroutineScope.launch {
            settingsRepository.setDefaultTab(tab)
        }
    }

    fun setLyricsAlignment(alignment: String) {
        coroutineScope.launch {
            settingsRepository.setLyricsAlignment(alignment)
        }
    }

    fun setLyricsTextSize(size: String) {
        coroutineScope.launch {
            settingsRepository.setLyricsTextSize(size)
        }
    }
}
