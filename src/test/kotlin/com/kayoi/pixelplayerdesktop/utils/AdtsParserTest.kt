package com.kayoi.pixelplayerdesktop.utils

import java.io.File
import java.nio.ByteBuffer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress

class AdtsParserTest {

    private fun createSyntheticAdtsBytes(frameCount: Int, frameLength: Int = 100, srIndex: Int = 4, channelConfig: Int = 2): ByteArray {
        val buffer = ByteBuffer.allocate(frameLength * frameCount)
        repeat(frameCount) {
            val b0 = 0xFF.toByte()
            val b1 = 0xF1.toByte()
            val b2 = (((1 shl 6) or (srIndex shl 2) or (channelConfig ushr 2)) and 0xFF).toByte()
            val b3 = (((channelConfig and 0x03) shl 6) or (frameLength ushr 11)).toByte()
            val b4 = ((frameLength ushr 3) and 0xFF).toByte()
            val b5 = (((frameLength and 0x07) shl 5) or 0x1F).toByte()
            val b6 = 0xFC.toByte()

            buffer.put(byteArrayOf(b0, b1, b2, b3, b4, b5, b6))
            buffer.put(ByteArray(frameLength - 7))
        }
        return buffer.array()
    }

    @Test
    fun testParseSyntheticAdtsFrames() {
        val frameLength = 100
        val sampleRate = 44100
        val channelConfig = 2
        val frameCount = 3

        val tempFile = File.createTempFile("test_synthetic", ".aac")
        tempFile.deleteOnExit()
        tempFile.writeBytes(createSyntheticAdtsBytes(frameCount, frameLength, 4, channelConfig))

        val result = AdtsParser.parse(tempFile)
        assertNotNull(result)
        assertEquals(sampleRate, result.audioInfo.sampleRate)
        assertEquals(channelConfig, result.audioInfo.channelCount)
        val expectedDurationMs = (frameCount * 1024 * 1000L) / sampleRate
        assertEquals(expectedDurationMs, result.audioInfo.durationMs)
    }

    @Test
    fun testScannerScansAdtsAacFile() = runTest {
        val tempMusicDir = File(System.getProperty("java.io.tmpdir"), "test_adts_music_${System.currentTimeMillis()}")
        tempMusicDir.mkdirs()
        tempMusicDir.deleteOnExit()

        val fileName = "You Belong With Me (Taylor’s Version) - Taylor Swift (128k).aac"
        val aacFile = File(tempMusicDir, fileName)
        aacFile.deleteOnExit()
        // 500 frames * 1024 samples @ 44.1kHz = ~11.6 seconds (> 10s default minimum duration filter)
        aacFile.writeBytes(createSyntheticAdtsBytes(frameCount = 500))

        val tempDb = File.createTempFile("test_aac_scanner_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val dao = MusicDao(db)

        val scanner = DesktopMusicScanner(
            musicDao = dao,
            artworkCacheDir = File(System.getProperty("java.io.tmpdir"))
        )

        val scanResult = scanner.scanDirectories(listOf(tempMusicDir.absolutePath))
        assertTrue(scanResult is ScanProgress.Completed)

        val songs = dao.getAllSongs(minDurationMs = 10000L)
        val taylorSong = songs.find { it.path == aacFile.absolutePath }
        assertNotNull(taylorSong)
        assertTrue(taylorSong.duration > 10000L)
        assertEquals("Taylor Swift", taylorSong.artist)
        assertEquals("You Belong With Me (Taylor’s Version)", taylorSong.title)
    }
}
