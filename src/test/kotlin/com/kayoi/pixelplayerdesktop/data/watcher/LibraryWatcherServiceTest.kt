package com.kayoi.pixelplayerdesktop.data.watcher

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DirectoryFilterUtils

class LibraryWatcherServiceTest {

    private lateinit var tempDbFile: File
    private lateinit var database: PixelPlayerDatabase
    private lateinit var musicDao: MusicDao
    private lateinit var lyricsDao: LyricsDao
    private lateinit var tempMusicDir: File
    private lateinit var tempArtDir: File
    private lateinit var scanner: DesktopMusicScanner

    @BeforeTest
    fun setup() {
        tempDbFile = File.createTempFile("test_watcher_db", ".db")
        database = PixelPlayerDatabase(tempDbFile)
        musicDao = MusicDao(database)
        lyricsDao = LyricsDao(database)

        tempMusicDir = File(System.getProperty("java.io.tmpdir"), "test_watcher_${System.currentTimeMillis()}")
        tempMusicDir.mkdirs()

        tempArtDir = File(System.getProperty("java.io.tmpdir"), "test_art_${System.currentTimeMillis()}")
        tempArtDir.mkdirs()

        scanner = DesktopMusicScanner(
            musicDao = musicDao,
            lyricsDao = lyricsDao,
            artworkCacheDir = tempArtDir
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempDbFile.delete()
        tempMusicDir.deleteRecursively()
        tempArtDir.deleteRecursively()
    }

    @Test
    fun testDeleteSongsByPathsRemovesDatabaseRowsWithoutDeletingDiskFiles() = runTest {
        val file1 = File(tempMusicDir, "track1.mp3")
        file1.writeBytes(byteArrayOf(1, 2, 3))
        val file2 = File(tempMusicDir, "track2.mp3")
        file2.writeBytes(byteArrayOf(4, 5, 6))

        scanner.scanFiles(listOf(file1, file2))
        val initialSongs = musicDao.getAllSongs()
        assertEquals(2, initialSongs.size)

        musicDao.deleteSongsByPaths(listOf(file1.absolutePath))
        val remainingSongs = musicDao.getAllSongs()
        assertEquals(1, remainingSongs.size)
        assertEquals(file2.absolutePath, remainingSongs.first().path)

        assertTrue(file1.exists())
        assertTrue(file2.exists())
    }

    @Test
    fun testScanFilesIncrementalAddition() = runTest {
        val file1 = File(tempMusicDir, "songA.mp3")
        file1.writeBytes(byteArrayOf(1, 2, 3))

        val scanned = scanner.scanFiles(listOf(file1))
        assertEquals(1, scanned.size)
        assertEquals("songA", scanned.first().title)

        val inDb = musicDao.getSongByPath(file1.absolutePath)
        assertNotNull(inDb)
        assertEquals("songA", inDb.title)
    }

    @Test
    fun testScanSidecarLrcFile() = runTest {
        val audioFile = File(tempMusicDir, "songWithLyrics.mp3")
        audioFile.writeBytes(byteArrayOf(1, 2, 3))
        val lrcFile = File(tempMusicDir, "songWithLyrics.lrc")
        lrcFile.writeText("[00:01.00]First line\n[00:05.00]Second line\n")

        val scanned = scanner.scanFiles(listOf(audioFile))
        assertEquals(1, scanned.size)
        val songId = scanned.first().id

        scanner.scanLrcFile(lrcFile)
        val lyrics = lyricsDao.getLyrics(songId)
        assertNotNull(lyrics)
        val synced = lyrics.synced
        assertNotNull(synced)
        assertEquals(2, synced.size)
        assertEquals("First line", synced[0].line)
    }

    @Test
    fun testDirectoryFilterExclusionMatching() {
        val excludedDir = File(tempMusicDir, "ExcludedFolder")
        val childFile = File(excludedDir, "secret.mp3")
        val normalFile = File(tempMusicDir, "public.mp3")

        val excludedList = listOf(excludedDir.absolutePath)
        assertTrue(DirectoryFilterUtils.isDirectoryOrParentExcluded(childFile.absolutePath, excludedList))
        assertTrue(!DirectoryFilterUtils.isDirectoryOrParentExcluded(normalFile.absolutePath, excludedList))
    }

    @Test
    fun testModifyFilePreservesExistingId() = runTest {
        val file = File(tempMusicDir, "modSong.mp3")
        file.writeBytes(byteArrayOf(1, 2, 3))

        val initialScan = scanner.scanFiles(listOf(file))
        assertEquals(1, initialScan.size)
        val originalId = initialScan.first().id

        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        file.setLastModified(System.currentTimeMillis() + 5000L)

        val updatedScan = scanner.scanFiles(listOf(file))
        assertEquals(1, updatedScan.size)
        assertEquals(originalId, updatedScan.first().id)
        val inDb = musicDao.getSongByPath(file.absolutePath)
        assertNotNull(inDb)
        assertEquals(originalId, inDb.id)
    }

    @Test
    fun testDeleteDirectoryRemovesNestedSongsOnly() = runTest {
        val folderA = File(tempMusicDir, "AlbumA").apply { mkdirs() }
        val folderB = File(tempMusicDir, "AlbumB").apply { mkdirs() }

        val trackA1 = File(folderA, "track1.mp3").apply { writeBytes(byteArrayOf(1)) }
        val trackA2 = File(folderA, "track2.mp3").apply { writeBytes(byteArrayOf(2)) }
        val trackB = File(folderB, "track3.mp3").apply { writeBytes(byteArrayOf(3)) }

        scanner.scanFiles(listOf(trackA1, trackA2, trackB))
        assertEquals(3, musicDao.getAllSongs().size)

        musicDao.deleteSongsInDirectory(folderA.absolutePath)
        val remaining = musicDao.getAllSongs()
        assertEquals(1, remaining.size)
        assertEquals(trackB.absolutePath, remaining.first().path)
    }

    @Test
    fun testNoiseFileFiltering() {
        val noiseNames = listOf(".DS_Store", "._track.mp3", "Thumbs.db", "desktop.ini", "temp.tmp", "audio.crdownload", "song.part")
        val ignoredExtensions = setOf("tmp", "crdownload", "part", "download", "bak", "swp")
        val ignoredFileNames = setOf("thumbs.db", "desktop.ini")

        for (noise in noiseNames) {
            val lower = noise.lowercase()
            val isIgnored = lower.startsWith(".") ||
                ignoredFileNames.contains(lower) ||
                ignoredExtensions.contains(lower.substringAfterLast('.', ""))

            assertTrue(isIgnored, "Expected $noise to be filtered as noise")
        }
    }
}
