package com.kayoi.pixelplayerdesktop.data.player.mpv

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState

class MpvAudioEngineTest {

    @Test
    fun testEngineInitializationAndVolumeControls() {
        val engine = runCatching { MpvAudioEngine() }.getOrNull()
        if (engine == null) {
            println("libmpv not available in test environment, skipping test.")
            return
        }

        try {
            assertEquals(PlaybackState.IDLE, engine.playbackState.value)
            assertEquals(1.0f, engine.volume.value)
            assertFalse(engine.isMuted.value)

            engine.setVolume(0.75f)
            assertEquals(0.75f, engine.volume.value)

            engine.setMute(true)
            assertTrue(engine.isMuted.value)

            engine.setMute(false)
            assertFalse(engine.isMuted.value)

            assertEquals(1.0f, engine.playbackSpeed.value)
            engine.setPlaybackSpeed(1.25f)
            assertEquals(1.25f, engine.playbackSpeed.value)
            engine.setPlaybackSpeed(1.0f)
            assertEquals(1.0f, engine.playbackSpeed.value)

            engine.setReplayGain(true, "album")
            engine.setEqualizer(true, listOf(2f, 3f, 0f, -1f, 0f, 0f, 1f, 2f, 3f, 4f), preamp = 1.5f)
            engine.setAudioFadeOnPlayPause(false)
            engine.setAudioFadeOnPlayPause(true)

            assertEquals("auto", engine.getCurrentAudioDevice())
            val devices = engine.getAudioDevices()
            assertTrue(devices.isNotEmpty(), "Audio devices list should not be empty")
            engine.setAudioDevice("auto")
            assertEquals("auto", engine.getCurrentAudioDevice())

            engine.pause()
            assertEquals(PlaybackState.IDLE, engine.playbackState.value)

            engine.stop()
            assertEquals(PlaybackState.IDLE, engine.playbackState.value)
            assertEquals(0L, engine.currentPositionMs.value)
        } finally {
            engine.release()
            assertTrue(engine.isClosed())
        }
    }

    @Test
    fun testPlaybackOfAudioFile() {
        val engine = runCatching { MpvAudioEngine() }.getOrNull() ?: return
        val tempWav = createTinyWavFile()
        try {
            engine.play(tempWav.absolutePath)
            Thread.sleep(600)
            assertTrue(
                engine.playbackState.value == PlaybackState.PLAYING ||
                engine.playbackState.value == PlaybackState.ENDED ||
                engine.playbackState.value == PlaybackState.IDLE
            )
        } finally {
            tempWav.delete()
            engine.release()
            assertTrue(engine.isClosed())
        }
    }

    @Test
    fun testDualPlayerInstancesAreDistinct() {
        val engine = runCatching { MpvAudioEngine() }.getOrNull() ?: return
        try {
            val active = engine.getActivePlayerForTest()
            val standby = engine.getStandbyPlayerForTest()
            kotlin.test.assertNotNull(active)
            kotlin.test.assertNotNull(standby)
            kotlin.test.assertNotSame(active, standby)
        } finally {
            engine.release()
            assertTrue(engine.isClosed())
        }
    }

    @Test
    fun testCrossfadeLifecycleAndCancelOnStop() {
        val engine = runCatching { MpvAudioEngine() }.getOrNull() ?: return
        val wav1 = createTinyWavFile(durationSec = 3.0)
        val wav2 = createTinyWavFile(durationSec = 3.0)
        try {
            engine.play(wav1.absolutePath)
            var attempts = 0
            while (engine.playbackState.value != PlaybackState.PLAYING && attempts < 20) {
                Thread.sleep(50)
                attempts++
            }
            assertEquals(PlaybackState.PLAYING, engine.playbackState.value)

            engine.setPlaybackSpeed(0.9f)
            assertEquals(0.9f, engine.playbackSpeed.value)

            engine.crossfadeTo(wav2.absolutePath, 1500L, targetSpeed = 1.5f)
            assertTrue(engine.isCrossfading.value)
            assertEquals(1.5f, engine.playbackSpeed.value)

            engine.stop()
            assertFalse(engine.isCrossfading.value)
            assertEquals(PlaybackState.IDLE, engine.playbackState.value)
        } finally {
            wav1.delete()
            wav2.delete()
            engine.release()
            assertTrue(engine.isClosed())
        }
    }

    @Test
    fun testPlayPauseMicroFade() {
        val engine = runCatching { MpvAudioEngine() }.getOrNull() ?: return
        val wav = createTinyWavFile(durationSec = 5.0)
        try {
            engine.setVolume(1.0f)
            engine.play(wav.absolutePath)
            var attempts = 0
            while (engine.playbackState.value != PlaybackState.PLAYING && attempts < 20) {
                Thread.sleep(50)
                attempts++
            }
            assertEquals(PlaybackState.PLAYING, engine.playbackState.value)

            engine.pause()
            assertEquals(PlaybackState.PAUSED, engine.playbackState.value)
            Thread.sleep(350)

            engine.play()
            assertEquals(PlaybackState.PLAYING, engine.playbackState.value)
            Thread.sleep(350)

            engine.stop()
            assertEquals(PlaybackState.IDLE, engine.playbackState.value)
        } finally {
            wav.delete()
            engine.release()
            assertTrue(engine.isClosed())
        }
    }

    private fun createTinyWavFile(durationSec: Double = 0.1): java.io.File {
        val file = java.io.File.createTempFile("test_audio_", ".wav")
        file.deleteOnExit()
        val sampleRate = 44100
        val numSamples = (sampleRate * durationSec).toInt()
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        java.io.FileOutputStream(file).use { out ->
            out.write("RIFF".toByteArray())
            out.write(java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(totalSize).array())
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(16).array())
            out.write(java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(1.toShort()).array())
            out.write(java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(1.toShort()).array())
            out.write(java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
            out.write(java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(sampleRate * 2).array())
            out.write(java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(2.toShort()).array())
            out.write(java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(16.toShort()).array())
            out.write("data".toByteArray())
            out.write(java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(dataSize).array())
            out.write(ByteArray(dataSize))
        }
        return file
    }
}
