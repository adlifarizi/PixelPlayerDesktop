package com.kayoi.pixelplayerdesktop.data.watcher

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DirectoryFilterUtils

class LibraryWatcherService(
    private val scanner: DesktopMusicScanner,
    private val musicDao: MusicDao,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : AutoCloseable {

    private var activeWatcher: DirectoryWatcher? = null
    private var settingsJob: Job? = null
    private var debounceJob: Job? = null
    private val eventChannel = Channel<DirectoryChangeEvent>(Channel.UNLIMITED)

    private val ignoredExtensions = setOf("tmp", "crdownload", "part", "download", "bak", "swp")
    private val ignoredFileNames = setOf("thumbs.db", "desktop.ini")
    private val lyricsExtensions = setOf("lrc", "ttml")

    @Volatile
    private var currentSettings: UserSettings = UserSettings()

    fun start() {
        if (settingsJob != null) return

        debounceJob = coroutineScope.launch(ioDispatcher) {
            processEventStream()
        }

        settingsJob = coroutineScope.launch(ioDispatcher) {
            settingsRepository.settingsFlow.collectLatest { settings ->
                val prevDirectories = currentSettings.musicDirectories
                currentSettings = settings
                if (prevDirectories != settings.musicDirectories || activeWatcher == null) {
                    reconfigureWatcher(settings.musicDirectories)
                }
            }
        }
    }

    private fun reconfigureWatcher(directories: List<String>) {
        stopWatcher()

        val validPaths = directories.mapNotNull { dirPath ->
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) dir.toPath() else null
        }

        if (validPaths.isEmpty()) return

        try {
            val watcher = DirectoryWatcher.builder()
                .paths(validPaths)
                .fileHashing(false)
                .listener { event ->
                    handleIncomingEvent(event)
                }
                .build()

            watcher.watchAsync()
            activeWatcher = watcher
        } catch (e: Throwable) {
            System.err.println("[LibraryWatcherService] Failed to initialize directory watcher: ${e.message}")
        }
    }

    private fun handleIncomingEvent(event: DirectoryChangeEvent) {
        val path = event.path() ?: return
        val pathStr = path.toString()

        if (DirectoryFilterUtils.isDirectoryOrParentExcluded(pathStr, currentSettings.excludedDirectories)) {
            return
        }

        val fileName = path.fileName?.toString()?.lowercase() ?: return
        if (fileName.startsWith(".") || ignoredFileNames.contains(fileName)) {
            return
        }

        val ext = fileName.substringAfterLast('.', "")
        if (ignoredExtensions.contains(ext)) {
            return
        }

        eventChannel.trySend(event)
    }

    private suspend fun processEventStream() {
        val pendingEvents = mutableListOf<DirectoryChangeEvent>()
        while (coroutineScope.isActive) {
            val first = eventChannel.receiveCatching().getOrNull() ?: break
            pendingEvents.add(first)
            var lastReceived = System.currentTimeMillis()

            while (coroutineScope.isActive) {
                val elapsed = System.currentTimeMillis() - lastReceived
                val remaining = (DEBOUNCE_DELAY_MS - elapsed).coerceAtLeast(0L)
                val next = withTimeoutOrNull(remaining) {
                    eventChannel.receiveCatching().getOrNull()
                }

                if (next != null) {
                    pendingEvents.add(next)
                    lastReceived = System.currentTimeMillis()
                    if (pendingEvents.size >= MAX_BATCH_SIZE) {
                        break
                    }
                } else {
                    break
                }
            }

            if (pendingEvents.isNotEmpty()) {
                val batch = pendingEvents.toList()
                pendingEvents.clear()
                runCatching {
                    processBatch(batch)
                }.onFailure { e ->
                    System.err.println("[LibraryWatcherService] Error processing event batch: ${e.message}")
                }
            }
        }
    }

    private suspend fun processBatch(events: List<DirectoryChangeEvent>) = withContext(ioDispatcher) {
        val audioFilesToScan = mutableSetOf<File>()
        val audioPathsToDelete = mutableSetOf<String>()
        val lrcFilesToScan = mutableSetOf<File>()
        val directoriesToDelete = mutableSetOf<String>()
        val directoriesToScan = mutableSetOf<File>()

        for (event in events) {
            val path = event.path() ?: continue
            val pathStr = path.toString()

            if (DirectoryFilterUtils.isDirectoryOrParentExcluded(pathStr, currentSettings.excludedDirectories)) {
                continue
            }

            when (event.eventType()) {
                EventType.CREATE, EventType.MODIFY -> {
                    val file = path.toFile()
                    if (event.isDirectory || file.isDirectory) {
                        if (file.exists() && file.isDirectory) {
                            directoriesToScan.add(file)
                        }
                    } else {
                        val ext = file.extension.lowercase()
                        if (scanner.supportedAudioExtensions.contains(ext)) {
                            audioFilesToScan.add(file)
                            audioPathsToDelete.remove(pathStr)
                        } else if (lyricsExtensions.contains(ext)) {
                            lrcFilesToScan.add(file)
                        }
                    }
                }
                EventType.DELETE -> {
                    if (event.isDirectory) {
                        directoriesToDelete.add(pathStr)
                    } else {
                        val ext = pathStr.substringAfterLast('.', "").lowercase()
                        if (scanner.supportedAudioExtensions.contains(ext)) {
                            audioPathsToDelete.add(pathStr)
                            val file = path.toFile()
                            audioFilesToScan.remove(file)
                        }
                    }
                }
                EventType.OVERFLOW -> {
                    val root = event.rootPath()?.toFile()
                    if (root != null && root.exists() && root.isDirectory) {
                        directoriesToScan.add(root)
                    }
                }
            }
        }

        for (dir in directoriesToDelete) {
            musicDao.deleteSongsInDirectory(dir)
        }

        for (dir in directoriesToScan) {
            collectAudioFilesFromSubtree(dir, audioFilesToScan, lrcFilesToScan)
        }

        if (audioPathsToDelete.isNotEmpty()) {
            musicDao.deleteSongsByPaths(audioPathsToDelete)
        }

        val readyAudioFiles = mutableListOf<File>()
        for (file in audioFilesToScan) {
            if (waitForFileReadiness(file)) {
                readyAudioFiles.add(file)
            }
        }

        if (readyAudioFiles.isNotEmpty()) {
            scanner.scanFiles(
                files = readyAudioFiles,
                artistDelimiters = currentSettings.artistDelimiters,
                wordDelimiters = currentSettings.wordDelimiters,
                extractArtistFromTitle = currentSettings.extractArtistFromTitle,
                autoScanLrcFiles = currentSettings.autoScanLrcFiles
            )
        }

        if (currentSettings.autoScanLrcFiles) {
            for (lrc in lrcFilesToScan) {
                if (waitForFileReadiness(lrc)) {
                    scanner.scanLrcFile(lrc)
                }
            }
        }
    }

    private fun collectAudioFilesFromSubtree(
        rootDir: File,
        audioFilesOut: MutableSet<File>,
        lrcFilesOut: MutableSet<File>
    ) {
        val rootPath = rootDir.toPath()
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return

        Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val dirAbs = dir.toFile().absolutePath
                val isExcluded = DirectoryFilterUtils.isDirectoryOrParentExcluded(
                    dirAbs,
                    currentSettings.excludedDirectories
                )
                return if (isExcluded) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val f = file.toFile()
                val name = f.name.lowercase()
                if (!name.startsWith(".") && !ignoredFileNames.contains(name)) {
                    val ext = f.extension.lowercase()
                    if (scanner.supportedAudioExtensions.contains(ext)) {
                        audioFilesOut.add(f)
                    } else if (lyricsExtensions.contains(ext)) {
                        lrcFilesOut.add(f)
                    }
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        })
    }

    private suspend fun waitForFileReadiness(file: File, maxAttempts: Int = 3): Boolean {
        for (attempt in 0 until maxAttempts) {
            if (!file.exists() || !file.isFile || !file.canRead()) return false
            val isReadable = runCatching {
                RandomAccessFile(file, "r").use { raf ->
                    raf.length() > 0L
                }
            }.getOrDefault(false)

            if (isReadable) return true
            delay(500L)
        }
        return false
    }

    private fun stopWatcher() {
        runCatching {
            activeWatcher?.close()
        }
        activeWatcher = null
    }

    override fun close() {
        settingsJob?.cancel()
        settingsJob = null
        debounceJob?.cancel()
        debounceJob = null
        eventChannel.close()
        stopWatcher()
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 2500L
        private const val MAX_BATCH_SIZE = 1000
    }
}
