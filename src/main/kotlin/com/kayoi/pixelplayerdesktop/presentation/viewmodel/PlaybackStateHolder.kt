package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import java.io.File

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.kayoi.pixelplayerdesktop.data.model.EqualizerPreset
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.PlayerInfo
import com.kayoi.pixelplayerdesktop.data.model.QueueItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.player.PlaybackUriResolver
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.domain.player.AudioDevice
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import com.kayoi.pixelplayerdesktop.domain.player.SleepTimerSession
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.LrcParser
import com.kayoi.pixelplayerdesktop.utils.MultiLangRomanizer

class PlaybackStateHolder(
    private val audioEngine: AudioEngine,
    private val queueManager: QueueManager,
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
    private val listeningStatsTracker: DesktopListeningStatsTracker = DesktopListeningStatsTracker(musicRepository),
    private val playbackUriResolver: PlaybackUriResolver? = null,
    val sleepTimerSession: SleepTimerSession = SleepTimerSession(),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private fun resolvePath(song: Song): String {
        return playbackUriResolver?.resolve(song) ?: song.path
    }

    val replayGainEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.replayGainEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val replayGainMode: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.replayGainMode }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "track")

    val crossfadeEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.crossfadeEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val crossfadeDurationSeconds: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.crossfadeDurationSeconds }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 3)

    val audioFadeOnPlayPause: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.audioFadeOnPlayPause }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val showQueueHistory: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.showQueueHistory }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val closeToTray: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.closeToTray }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    fun setReplayGainEnabled(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setReplayGainEnabled(enabled) }
    }

    fun setReplayGainMode(mode: String) {
        coroutineScope.launch { settingsRepository.setReplayGainMode(mode) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDurationSeconds(durationSeconds: Int) {
        coroutineScope.launch { settingsRepository.setCrossfadeDurationSeconds(durationSeconds) }
    }

    fun setAudioFadeOnPlayPause(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setAudioFadeOnPlayPause(enabled) }
    }

    fun setShowQueueHistory(show: Boolean) {
        coroutineScope.launch { settingsRepository.setShowQueueHistory(show) }
    }

    fun setCloseToTray(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setCloseToTray(enabled) }
    }

    val selectedAudioDevice: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.selectedAudioDevice }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "auto")

    private val _availableAudioDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableAudioDevices: StateFlow<List<AudioDevice>> = _availableAudioDevices.asStateFlow()

    fun refreshAudioDevices() {
        _availableAudioDevices.value = audioEngine.getAudioDevices()
    }

    fun setSelectedAudioDevice(device: String) {
        coroutineScope.launch {
            settingsRepository.setSelectedAudioDevice(device)
        }
    }

    val equalizerEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.equalizerEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val equalizerPreset: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.equalizerPreset }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "flat")

    val equalizerBands: StateFlow<List<Float>> = settingsRepository.settingsFlow
        .map { it.equalizerBands }
        .stateIn(coroutineScope, SharingStarted.Eagerly, List(10) { 0f })

    val equalizerPreamp: StateFlow<Float> = settingsRepository.settingsFlow
        .map { it.equalizerPreamp }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 0f)

    val customEqualizerPresets: StateFlow<List<EqualizerPreset>> = settingsRepository.settingsFlow
        .map { it.customEqualizerPresets }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val pinnedEqualizerPresets: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.pinnedEqualizerPresets }
        .stateIn(coroutineScope, SharingStarted.Eagerly, EqualizerPreset.DEFAULT_PINNED)

    val equalizerViewMode: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.equalizerViewMode }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "SLIDERS")

    fun setEqualizerEnabled(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setEqualizerEnabled(enabled) }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        coroutineScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    equalizerPreset = preset.name,
                    equalizerBands = preset.bandLevels,
                    equalizerPreamp = preset.preamp
                )
            }
        }
    }

    fun setEqualizerBand(index: Int, level: Float) {
        coroutineScope.launch {
            val current = settingsRepository.settingsFlow.value.equalizerBands.toMutableList()
            if (index in 0 until current.size) {
                current[index] = level.coerceIn(EqualizerPreset.MIN_LEVEL, EqualizerPreset.MAX_LEVEL)
                settingsRepository.updateSettings {
                    it.copy(
                        equalizerBands = current,
                        equalizerPreset = "custom"
                    )
                }
            }
        }
    }

    fun setEqualizerPreamp(preamp: Float) {
        coroutineScope.launch {
            settingsRepository.setEqualizerPreamp(preamp.coerceIn(EqualizerPreset.MIN_PREAMP, EqualizerPreset.MAX_PREAMP))
        }
    }

    fun saveCustomEqualizerPreset(name: String) {
        coroutineScope.launch {
            val currentSettings = settingsRepository.settingsFlow.value
            val newPreset = EqualizerPreset.custom(
                name = name,
                displayName = name,
                bandLevels = currentSettings.equalizerBands,
                preamp = currentSettings.equalizerPreamp
            )
            settingsRepository.saveCustomEqualizerPreset(newPreset)
            settingsRepository.setEqualizerPreset(name)
        }
    }

    fun deleteCustomEqualizerPreset(name: String) {
        coroutineScope.launch {
            settingsRepository.deleteCustomEqualizerPreset(name)
        }
    }

    fun renameCustomEqualizerPreset(oldName: String, newName: String) {
        coroutineScope.launch {
            settingsRepository.renameCustomEqualizerPreset(oldName, newName)
        }
    }

    fun updatePinnedEqualizerPresets(pinnedNames: List<String>) {
        coroutineScope.launch {
            settingsRepository.setPinnedEqualizerPresets(pinnedNames)
        }
    }

    fun setEqualizerViewMode(mode: String) {
        coroutineScope.launch {
            settingsRepository.setEqualizerViewMode(mode)
        }
    }

    private val _playerInfo = MutableStateFlow(PlayerInfo())
    val playerInfo: StateFlow<PlayerInfo> = _playerInfo.asStateFlow()

    val currentPositionMs: StateFlow<Long> = audioEngine.currentPositionMs
    val playbackSpeed: StateFlow<Float> = audioEngine.playbackSpeed
    private var sessionPlaybackSpeed: Float = 1.0f
    private var baseSessionSpeedBeforeSong: Float = 1.0f

    val isCurrentSongSpeedSaved: StateFlow<Boolean> = combine(
        queueManager.currentSong,
        settingsRepository.settingsFlow
    ) { song, settings ->
        if (song == null) false else settings.songPlaybackSpeeds.containsKey(song.id)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val savedSongSpeedsCount: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.songPlaybackSpeeds.size }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 0)

    private val _lyricsSearchUiState = MutableStateFlow<LyricsSearchUiState>(LyricsSearchUiState.Idle)
    val lyricsSearchUiState: StateFlow<LyricsSearchUiState> = _lyricsSearchUiState.asStateFlow()

    fun resetLyricsSearchState() {
        _lyricsSearchUiState.value = LyricsSearchUiState.Idle
    }

    fun searchLyrics(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch {
            _lyricsSearchUiState.value = LyricsSearchUiState.Loading
            try {
                val results = lyricsRepository.searchOnlineLyrics(query)
                if (results.isEmpty()) {
                    _lyricsSearchUiState.value = LyricsSearchUiState.NotFound(query)
                } else {
                    _lyricsSearchUiState.value = LyricsSearchUiState.PickResult(query, results)
                }
            } catch (e: Exception) {
                _lyricsSearchUiState.value = LyricsSearchUiState.Error(e.message ?: "Failed to search lyrics")
            }
        }
    }

    fun applyLyrics(lyrics: Lyrics) {
        val currentSong = queueManager.currentSong.value ?: return
        coroutineScope.launch {
            val enriched = MultiLangRomanizer.enrichLyrics(lyrics)
            lyricsRepository.saveLyrics(currentSong.id, enriched)
            _playerInfo.update { it.copy(lyrics = enriched) }
            _lyricsSearchUiState.value = LyricsSearchUiState.Idle
        }
    }

    fun importLyricsFromFile(file: File) {
        val currentSong = queueManager.currentSong.value ?: return
        if (!file.exists() || !file.isFile) return
        coroutineScope.launch {
            val content = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return@launch
            val parsed = LrcParser.parse(content).copy(areFromRemote = false)
            val enriched = MultiLangRomanizer.enrichLyrics(parsed)
            lyricsRepository.saveLyrics(currentSong.id, enriched)
            _playerInfo.update { it.copy(lyrics = enriched) }
            _lyricsSearchUiState.value = LyricsSearchUiState.Idle
        }
    }

    fun resetCurrentSongLyrics() {
        val currentSong = queueManager.currentSong.value ?: return
        coroutineScope.launch {
            lyricsRepository.deleteLyrics(currentSong.id)
            settingsRepository.setLyricsSyncOffset(currentSong.id, 0)
            val fresh = lyricsRepository.getLyrics(
                songId = currentSong.id,
                songPath = currentSong.path,
                title = currentSong.title,
                artist = currentSong.artist
            )
            _playerInfo.update { it.copy(lyrics = fresh, lyricsSyncOffsetMs = 0) }
            _lyricsSearchUiState.value = LyricsSearchUiState.Idle
        }
    }

    fun setLyricsSyncOffset(offsetMs: Int) {
        val currentSong = queueManager.currentSong.value ?: return
        _playerInfo.update { it.copy(lyricsSyncOffsetMs = offsetMs) }
        coroutineScope.launch {
            settingsRepository.setLyricsSyncOffset(currentSong.id, offsetMs)
        }
    }

    fun saveLyricsBesideCurrentSong(preferSynced: Boolean = true): File? {
        val song = queueManager.currentSong.value ?: return null
        val lyrics = _playerInfo.value.lyrics ?: return null
        val content = LrcParser.toLrcString(lyrics, preferSynced)
        if (content.isBlank()) return null
        return runCatching {
            val songFile = File(song.path)
            val lrcFile = File(songFile.parentFile, "${songFile.nameWithoutExtension}.lrc")
            lrcFile.writeText(content, Charsets.UTF_8)
            lrcFile
        }.getOrNull()
    }

    fun exportLyricsToFile(targetFile: File, preferSynced: Boolean = true): Boolean {
        val lyrics = _playerInfo.value.lyrics ?: return false
        val content = LrcParser.toLrcString(lyrics, preferSynced)
        if (content.isBlank()) return false
        return runCatching {
            targetFile.writeText(content, Charsets.UTF_8)
            true
        }.getOrDefault(false)
    }

    suspend fun saveLyricsToEmbeddedMetadata(preferSynced: Boolean = true): Boolean {
        val song = queueManager.currentSong.value ?: return false
        val lyrics = _playerInfo.value.lyrics ?: return false
        val content = LrcParser.toLrcString(lyrics, preferSynced)
        if (content.isBlank()) return false
        val result = musicRepository.updateSongLyrics(song, content)
        if (result.success && result.updatedSong != null) {
            onSongMetadataUpdated(result.updatedSong)
            return true
        }
        return false
    }

    fun onSongMetadataUpdated(updatedSong: Song) {
        queueManager.updateSong(updatedSong)
        val current = queueManager.currentSong.value
        if (current != null && current.id == updatedSong.id) {
            _playerInfo.update {
                it.copy(
                    songTitle = updatedSong.title,
                    artistName = updatedSong.displayArtist,
                    albumArtUri = updatedSong.albumArtUriString,
                    totalDurationMs = updatedSong.duration
                )
            }
        }
    }

    fun onSongsMetadataUpdated(updatedSongs: List<Song>) {
        for (song in updatedSongs) {
            onSongMetadataUpdated(song)
        }
    }

    init {
        val initialSettings = settingsRepository.settingsFlow.value
        audioEngine.setVolume(initialSettings.volume)
        audioEngine.setMute(initialSettings.isMuted)
        _playerInfo.update {
            it.copy(
                volume = initialSettings.volume,
                isMuted = initialSettings.isMuted
            )
        }

        coroutineScope.launch {
            queueManager.currentSong
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { song ->
                val activeTimer = sleepTimerSession.timer.value
                if (activeTimer is SleepTimerSession.Timer.EndOfTrack && activeTimer.songId != song?.id) {
                    sleepTimerSession.setTimer(null)
                    sleepTimerSession.emit(SleepTimerSession.Event.EndOfTrackCancelledBySongChange(activeTimer.songId, song?.id))
                }
                val countedPlay = sleepTimerSession.countedPlay.value
                if (countedPlay != null && countedPlay.songId != song?.id) {
                    sleepTimerSession.setCountedPlay(null)
                }

                val savedSpeed = song?.let { settingsRepository.settingsFlow.value.songPlaybackSpeeds[it.id] }
                baseSessionSpeedBeforeSong = sessionPlaybackSpeed
                val targetSpeed = savedSpeed ?: sessionPlaybackSpeed
                audioEngine.setPlaybackSpeed(targetSpeed)

                listeningStatsTracker.onSongChanged(song, audioEngine.playbackState.value == PlaybackState.PLAYING)
                val isFav = if (song != null) musicRepository.isFavorite(song.id) else false
                val offset = if (song != null) {
                    settingsRepository.settingsFlow.value.lyricsSyncOffsets[song.id] ?: 0
                } else 0
                _playerInfo.update {
                    it.copy(
                        songTitle = song?.title ?: "",
                        artistName = song?.displayArtist ?: "",
                        albumArtUri = song?.albumArtUriString,
                        totalDurationMs = song?.duration ?: 0L,
                        isFavorite = isFav,
                        lyricsSyncOffsetMs = offset,
                        lyrics = null,
                        isLoadingLyrics = song != null
                    )
                }
                if (song != null) {
                    scheduleTransitionFor(song)
                    val lyrics = lyricsRepository.getLyrics(song.id, song.path, song.title, song.artist)
                    _playerInfo.update {
                        it.copy(
                            lyrics = lyrics,
                            isLoadingLyrics = false
                        )
                    }
                } else {
                    transitionSchedulerJob?.cancel()
                    _playerInfo.update { it.copy(isLoadingLyrics = false) }
                }
            }
        }

        coroutineScope.launch {
            audioEngine.playbackState.collect { state ->
                val isPlaying = state == PlaybackState.PLAYING
                val isBuffering = state == PlaybackState.BUFFERING
                listeningStatsTracker.onPlaybackStateChanged(isPlaying)
                _playerInfo.update { it.copy(isPlaying = isPlaying, isBuffering = isBuffering) }
            }
        }



        coroutineScope.launch {
            audioEngine.durationMs.collect { dur ->
                if (dur > 0) {
                    _playerInfo.update { it.copy(totalDurationMs = dur) }
                }
            }
        }

        coroutineScope.launch {
            queueManager.queue.collect { q ->
                val items = q.map { QueueItem(id = it.id.hashCode().toLong(), albumArtUri = it.albumArtUriString) }
                _playerInfo.update { it.copy(queue = items) }
            }
        }

        coroutineScope.launch {
            queueManager.isShuffleEnabled.collect { shuffle ->
                _playerInfo.update { it.copy(isShuffleEnabled = shuffle) }
            }
        }

        coroutineScope.launch {
            queueManager.repeatMode.collect { rep ->
                _playerInfo.update { it.copy(repeatMode = rep) }
                val countedPlay = sleepTimerSession.countedPlay.value
                if (countedPlay != null && rep != 2) {
                    sleepTimerSession.setCountedPlay(null)
                }
            }
        }

        coroutineScope.launch {
            audioEngine.volume.collect { vol ->
                _playerInfo.update { it.copy(volume = vol) }
            }
        }

        coroutineScope.launch {
            audioEngine.isMuted.collect { muted ->
                _playerInfo.update { it.copy(isMuted = muted) }
            }
        }

        coroutineScope.launch {
            settingsRepository.settingsFlow
                .map { Pair(it.replayGainEnabled, it.replayGainMode) }
                .distinctUntilChanged()
                .collect { (enabled, mode) ->
                    audioEngine.setReplayGain(enabled, mode)
                }
        }

        coroutineScope.launch {
            settingsRepository.settingsFlow
                .map { Triple(it.equalizerEnabled, it.equalizerBands, it.equalizerPreamp) }
                .distinctUntilChanged()
                .collect { (enabled, bands, preamp) ->
                    audioEngine.setEqualizer(enabled, bands, preamp)
                }
        }

        coroutineScope.launch {
            settingsRepository.settingsFlow
                .map { it.selectedAudioDevice }
                .distinctUntilChanged()
                .collect { device ->
                    audioEngine.setAudioDevice(device)
                }
        }
        coroutineScope.launch {
            audioEngine.audioDeviceFallbackEvents.collect { fallbackDevice ->
                settingsRepository.setSelectedAudioDevice(fallbackDevice)
                refreshAudioDevices()
            }
        }
        coroutineScope.launch {
            audioEngine.audioDeviceListChangedEvents.collect {
                refreshAudioDevices()
            }
        }
        refreshAudioDevices()

        coroutineScope.launch {
            settingsRepository.settingsFlow
                .map { Pair(it.crossfadeEnabled, it.crossfadeDurationSeconds) }
                .distinctUntilChanged()
                .collect { (enabled, _) ->
                    val curr = queueManager.currentSong.value
                    if (curr != null) {
                        if (enabled) {
                            scheduleTransitionFor(curr)
                        } else {
                            transitionSchedulerJob?.cancel()
                        }
                    }
                }
        }

        coroutineScope.launch {
            settingsRepository.settingsFlow
                .map { it.audioFadeOnPlayPause }
                .distinctUntilChanged()
                .collect { enabled ->
                    audioEngine.setAudioFadeOnPlayPause(enabled)
                }
        }

        coroutineScope.launch {
            sleepTimerSession.timer.collect { timer ->
                if (timer is SleepTimerSession.Timer.EndOfTrack) {
                    transitionSchedulerJob?.cancel()
                } else if (timer == null) {
                    val curr = queueManager.currentSong.value
                    if (curr != null && crossfadeEnabled.value) {
                        scheduleTransitionFor(curr)
                    }
                }
            }
        }

        coroutineScope.launch {
            sleepTimerSession.countedPlay.collect { countedPlay ->
                if (countedPlay != null && countedPlay.currentCount >= countedPlay.total) {
                    transitionSchedulerJob?.cancel()
                } else if (countedPlay == null) {
                    val curr = queueManager.currentSong.value
                    if (curr != null && crossfadeEnabled.value) {
                        scheduleTransitionFor(curr)
                    }
                }
            }
        }

        coroutineScope.launch {
            audioEngine.completionEvents.collect {
                transitionSchedulerJob?.cancel()
                val current = queueManager.currentSong.value
                val activeTimer = sleepTimerSession.timer.value
                val countedPlay = sleepTimerSession.countedPlay.value

                if (activeTimer is SleepTimerSession.Timer.EndOfTrack) {
                    sleepTimerSession.setTimer(null)
                    sleepTimerSession.emit(SleepTimerSession.Event.EndOfTrackReached(activeTimer.songId))
                    listeningStatsTracker.onPlaybackStopped()
                    audioEngine.stop()
                    val next = queueManager.peekNextSong()
                    if (next != null && next.id != current?.id) {
                        queueManager.getNextSong()
                    }
                    return@collect
                }

                if (countedPlay != null) {
                    if (countedPlay.currentCount >= countedPlay.total) {
                        sleepTimerSession.setCountedPlay(null)
                        sleepTimerSession.emit(SleepTimerSession.Event.CountedPlayFinished(countedPlay.songId))
                        listeningStatsTracker.onPlaybackStopped()
                        audioEngine.stop()
                        return@collect
                    } else {
                        val updated = countedPlay.copy(currentCount = countedPlay.currentCount + 1)
                        sleepTimerSession.setCountedPlay(updated)
                        if (current != null) {
                            audioEngine.play(resolvePath(current))
                        }
                        return@collect
                    }
                }

                val next = queueManager.getNextSong()
                if (next != null) {
                    if (current != null && next.id == current.id) {
                        listeningStatsTracker.onSongChanged(next, isPlaying = true)
                    }
                    audioEngine.play(resolvePath(next))
                } else {
                    listeningStatsTracker.onPlaybackStopped()
                    audioEngine.stop()
                }
            }
        }
    }

    private var transitionSchedulerJob: Job? = null

    private fun scheduleTransitionFor(song: Song) {
        transitionSchedulerJob?.cancel()
        if (!crossfadeEnabled.value) return

        val currentTimer = sleepTimerSession.timer.value
        val countedPlay = sleepTimerSession.countedPlay.value
        val isEndOfTrack = currentTimer is SleepTimerSession.Timer.EndOfTrack
        val isFinalCountedPlay = countedPlay != null && countedPlay.currentCount >= countedPlay.total
        if (isEndOfTrack || isFinalCountedPlay) return

        transitionSchedulerJob = coroutineScope.launch {
            while (isActive && audioEngine.isCrossfading.value) {
                delay(200)
            }
            if (!isActive || queueManager.currentSong.value?.id != song.id) return@launch

            delay(1500)
            if (!isActive || queueManager.currentSong.value?.id != song.id) return@launch

            var dur = audioEngine.durationMs.value
            if (dur <= 0) dur = song.duration
            var waitCount = 0
            while (isActive && dur <= 0 && waitCount < 20) {
                delay(250)
                dur = audioEngine.durationMs.value
                waitCount++
            }
            if (!isActive || dur <= 0 || queueManager.currentSong.value?.id != song.id) return@launch

            val fadeDurationMs = (crossfadeDurationSeconds.value * 1000L).coerceIn(500L, 12000L)
            val minTrackLength = fadeDurationMs + 1000L
            if (dur < minTrackLength) return@launch

            val transitionPoint = dur - fadeDurationMs

            val currentPos = audioEngine.currentPositionMs.value
            if (currentPos >= transitionPoint) {
                val remaining = (dur - currentPos).coerceAtLeast(0L)
                if (remaining > 500L) {
                    val nextSong = queueManager.peekNextSong()
                    if (nextSong != null) {
                        val nextSpeed = settingsRepository.settingsFlow.value.songPlaybackSpeeds[nextSong.id] ?: sessionPlaybackSpeed
                        audioEngine.crossfadeTo(resolvePath(nextSong), remaining.coerceAtMost(fadeDurationMs), nextSpeed)
                        queueManager.getNextSong()
                    }
                }
                return@launch
            }

            while (isActive &&
                queueManager.currentSong.value?.id == song.id &&
                audioEngine.playbackState.value == PlaybackState.PLAYING
            ) {
                val pos = audioEngine.currentPositionMs.value
                val remaining = transitionPoint - pos
                if (remaining <= 0) break

                val sleep = when {
                    remaining > 5000 -> 1000L
                    remaining > 1000 -> 250L
                    else -> 50L
                }.coerceAtMost(remaining).coerceAtLeast(20L)

                delay(sleep)
            }

            if (!isActive ||
                queueManager.currentSong.value?.id != song.id ||
                audioEngine.playbackState.value != PlaybackState.PLAYING
            ) return@launch

            val nextSong = queueManager.peekNextSong() ?: return@launch
            val nextSpeed = settingsRepository.settingsFlow.value.songPlaybackSpeeds[nextSong.id] ?: sessionPlaybackSpeed
            audioEngine.crossfadeTo(resolvePath(nextSong), fadeDurationMs, nextSpeed)
            queueManager.getNextSong()
        }
    }

    fun playSong(song: Song) {
        queueManager.playSong(song)
        audioEngine.play(resolvePath(song))
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val target = queueManager.setQueue(songs, startIndex)
        if (target != null) {
            audioEngine.play(resolvePath(target))
        }
    }

    fun shuffleSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playSongs(shuffled, 0)
    }

    fun togglePlayPause() {
        if (audioEngine.playbackState.value == PlaybackState.PLAYING) {
            audioEngine.pause()
        } else {
            val currSong = queueManager.currentSong.value ?: return
            val targetPath = resolvePath(currSong)
            if (audioEngine.playbackState.value == PlaybackState.PAUSED && audioEngine.currentTrackPath == targetPath) {
                audioEngine.play()
            } else {
                audioEngine.play(targetPath)
            }
        }
    }

    fun playNext() {
        val next = queueManager.getNextSong()
        if (next != null) {
            audioEngine.play(resolvePath(next))
        } else {
            listeningStatsTracker.onPlaybackStopped()
            audioEngine.stop()
        }
    }

    fun playPrevious() {
        val prev = queueManager.getPreviousSong(audioEngine.currentPositionMs.value)
        if (prev != null) {
            audioEngine.play(resolvePath(prev))
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        val song = queueManager.currentSong.value
        if (song != null && crossfadeEnabled.value) {
            scheduleTransitionFor(song)
        }
    }

    fun seekRelative(deltaMs: Long) {
        val duration = queueManager.currentSong.value?.duration?.takeIf { it > 0 }
            ?: audioEngine.durationMs.value.takeIf { it > 0 }
            ?: Long.MAX_VALUE
        val current = audioEngine.currentPositionMs.value
        val target = (current + deltaMs).coerceIn(0L, duration)
        seekTo(target)
    }

    fun setVolume(vol: Float) {
        audioEngine.setVolume(vol)
        coroutineScope.launch {
            settingsRepository.setVolume(vol)
        }
    }

    fun toggleMute() {
        val newMute = !audioEngine.isMuted.value
        audioEngine.setMute(newMute)
        coroutineScope.launch {
            settingsRepository.setMuted(newMute)
        }
    }

    fun setPlaybackSpeed(speed: Float, persist: Boolean = true) {
        val song = queueManager.currentSong.value
        if (isCurrentSongSpeedSaved.value && song != null) {
            if (persist) {
                coroutineScope.launch {
                    settingsRepository.setSongPlaybackSpeed(song.id, speed)
                }
            }
        } else {
            sessionPlaybackSpeed = speed
        }
        audioEngine.setPlaybackSpeed(speed)
    }

    fun setRememberSpeedForCurrentSong(remember: Boolean) {
        val song = queueManager.currentSong.value ?: return
        val currentSpeed = audioEngine.playbackSpeed.value
        coroutineScope.launch {
            if (remember) {
                sessionPlaybackSpeed = baseSessionSpeedBeforeSong
                settingsRepository.setSongPlaybackSpeed(song.id, currentSpeed)
            } else {
                settingsRepository.setSongPlaybackSpeed(song.id, null)
                sessionPlaybackSpeed = currentSpeed
            }
        }
    }

    fun resetPlaybackSpeed() {
        val song = queueManager.currentSong.value
        sessionPlaybackSpeed = 1.0f
        baseSessionSpeedBeforeSong = 1.0f
        audioEngine.setPlaybackSpeed(1.0f)
        if (song != null) {
            coroutineScope.launch {
                settingsRepository.setSongPlaybackSpeed(song.id, null)
            }
        }
    }

    fun clearAllSavedSongSpeeds() {
        coroutineScope.launch {
            settingsRepository.clearAllSongPlaybackSpeeds()
            audioEngine.setPlaybackSpeed(sessionPlaybackSpeed)
        }
    }

    fun toggleShuffle() {
        val newShuffle = queueManager.toggleShuffle()
        coroutineScope.launch {
            settingsRepository.setShuffleEnabled(newShuffle)
        }
    }

    fun cycleRepeatMode() {
        val newMode = queueManager.cycleRepeatMode()
        coroutineScope.launch {
            settingsRepository.setRepeatMode(newMode)
        }
    }

    fun toggleFavoriteCurrentSong() {
        val curr = queueManager.currentSong.value ?: return
        coroutineScope.launch {
            val newFav = musicRepository.toggleFavorite(curr.id)
            queueManager.updateSongFavorite(curr.id, newFav)
            _playerInfo.update { it.copy(isFavorite = newFav) }
        }
    }

    fun toggleFavorite(song: Song) {
        coroutineScope.launch {
            val newFav = musicRepository.toggleFavorite(song.id)
            queueManager.updateSongFavorite(song.id, newFav)
            if (queueManager.currentSong.value?.id == song.id) {
                _playerInfo.update { it.copy(isFavorite = newFav) }
            }
        }
    }

    private var lastDismissedQueue: List<Song> = emptyList()
    private var lastDismissedIndex: Int = 0
    private var lastDismissedPositionMs: Long = 0L

    fun stopPlayback() {
        listeningStatsTracker.onPlaybackStopped()
        lastDismissedQueue = queueManager.queue.value
        lastDismissedIndex = queueManager.currentIndex.value
        lastDismissedPositionMs = audioEngine.currentPositionMs.value
        audioEngine.stop()
        queueManager.clearQueue()
        _playerInfo.update {
            PlayerInfo(
                volume = audioEngine.volume.value,
                isMuted = audioEngine.isMuted.value,
                isShuffleEnabled = queueManager.isShuffleEnabled.value,
                repeatMode = queueManager.repeatMode.value
            )
        }
    }

    fun undoStopPlayback() {
        if (lastDismissedQueue.isNotEmpty()) {
            val targetIndex = lastDismissedIndex.coerceIn(0, lastDismissedQueue.size - 1)
            val pos = lastDismissedPositionMs
            playSongs(lastDismissedQueue, targetIndex)
            if (pos > 0) {
                audioEngine.seekTo(pos)
            }
            lastDismissedQueue = emptyList()
        }
    }

    fun removeFromQueue(index: Int) {
        val currIdx = queueManager.currentIndex.value
        val isRemovingCurrent = (index == currIdx)
        val nextSong = queueManager.removeFromQueue(index)

        if (isRemovingCurrent) {
            if (nextSong != null) {
                playSong(nextSong)
            } else {
                stopPlayback()
            }
        }
    }

    fun removeSongFromQueue(songId: String) {
        val idx = queueManager.queue.value.indexOfFirst { it.id == songId }
        if (idx >= 0) {
            removeFromQueue(idx)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        queueManager.moveQueueItem(fromIndex, toIndex)
    }

    fun clearQueue() {
        listeningStatsTracker.onPlaybackStopped()
        audioEngine.stop()
        queueManager.clearQueue()
        _playerInfo.update {
            PlayerInfo(
                volume = audioEngine.volume.value,
                isMuted = audioEngine.isMuted.value,
                isShuffleEnabled = queueManager.isShuffleEnabled.value,
                repeatMode = queueManager.repeatMode.value
            )
        }
    }

    fun addSongToQueue(song: Song) {
        queueManager.addToQueue(listOf(song))
    }

    fun addSongsToQueue(songs: List<Song>) {
        queueManager.addToQueue(songs)
    }

    fun playSongNext(song: Song) {
        queueManager.playNextInQueue(song)
    }

    fun playSongsNext(songs: List<Song>) {
        queueManager.playNextInQueue(songs)
    }

    fun playSongAtIndex(index: Int) {
        val target = queueManager.playAtIndex(index)
        if (target != null) {
            audioEngine.play(resolvePath(target))
        }
    }

    val currentSong: StateFlow<Song?> = queueManager.currentSong
    val currentQueueIndex: StateFlow<Int> = queueManager.currentIndex
    val queue: StateFlow<List<Song>> = queueManager.queue
    val playbackState: StateFlow<PlaybackState> = audioEngine.playbackState
    val errorEvents: SharedFlow<String> = audioEngine.errorEvents
}
