package com.kayoi.pixelplayerdesktop.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Tag

data class AdtsAudioInfo(
    val durationMs: Long,
    val sampleRate: Int,
    val bitrate: Int?,
    val channelCount: Int
)

data class AdtsParseResult(
    val audioInfo: AdtsAudioInfo,
    val tag: Tag?
)

object AdtsParser {

    private val SAMPLING_FREQUENCIES = intArrayOf(
        96000, 88200, 64000, 48000, 44100, 32000,
        24000, 22050, 16000, 12000, 11025, 8000, 7350
    )

    fun parse(file: File): AdtsParseResult? {
        if (!file.exists() || file.length() < 7) return null

        return try {
            val (tag, audioOffset) = extractId3TagAndOffset(file)
            val audioInfo = parseFrames(file, audioOffset) ?: return null
            AdtsParseResult(audioInfo, tag)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractId3TagAndOffset(file: File): Pair<Tag?, Long> {
        val fileLength = file.length()
        if (fileLength < 10) return Pair(null, 0L)

        FileInputStream(file).use { fis ->
            val header = ByteArray(10)
            if (fis.read(header) < 10) return Pair(null, 0L)

            if (header[0] == 'I'.code.toByte() &&
                header[1] == 'D'.code.toByte() &&
                header[2] == '3'.code.toByte()
            ) {
                val majorVersion = header[3].toInt() and 0xFF
                val flags = header[5].toInt() and 0xFF
                val synchsafeSize = ((header[6].toInt() and 0x7F) shl 21) or
                        ((header[7].toInt() and 0x7F) shl 14) or
                        ((header[8].toInt() and 0x7F) shl 7) or
                        (header[9].toInt() and 0x7F)
                val hasFooter = (flags and 0x10) != 0
                val tagTotalSize = 10L + synchsafeSize + (if (hasFooter) 10L else 0L)
                val safeTagSize = tagTotalSize.coerceAtMost(fileLength)

                val tag = try {
                    if (safeTagSize <= Int.MAX_VALUE) {
                        val tagBytes = ByteArray(safeTagSize.toInt())
                        System.arraycopy(header, 0, tagBytes, 0, 10)
                        val remaining = safeTagSize.toInt() - 10
                        if (remaining > 0) {
                            var readTotal = 0
                            while (readTotal < remaining) {
                                val r = fis.read(tagBytes, 10 + readTotal, remaining - readTotal)
                                if (r == -1) break
                                readTotal += r
                            }
                        }
                        val buf = ByteBuffer.wrap(tagBytes)
                        when (majorVersion) {
                            2 -> ID3v22Tag().apply { read(buf) }
                            3 -> ID3v23Tag().apply { read(buf) }
                            4 -> ID3v24Tag().apply { read(buf) }
                            else -> ID3v23Tag().apply { read(buf) }
                        }
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
                return Pair(tag, safeTagSize)
            }
        }
        return Pair(null, 0L)
    }

    private fun parseFrames(file: File, audioOffset: Long): AdtsAudioInfo? {
        val fileLength = file.length()
        if (audioOffset >= fileLength) return null

        var sampleRate = 44100
        var channelCount = 2
        var frameCount = 0
        var totalSamples = 0L
        var firstFrameBitrate: Int? = null

        BufferedInputStream(FileInputStream(file), 65536).use { bis ->
            if (audioOffset > 0) {
                var skippedOffset = 0L
                while (skippedOffset < audioOffset) {
                    val s = bis.skip(audioOffset - skippedOffset)
                    if (s <= 0) break
                    skippedOffset += s
                }
            }

            val header = ByteArray(7)
            while (true) {
                var headerRead = 0
                while (headerRead < 7) {
                    val r = bis.read(header, headerRead, 7 - headerRead)
                    if (r == -1) break
                    headerRead += r
                }
                if (headerRead < 7) break

                val b0 = header[0].toInt() and 0xFF
                val b1 = header[1].toInt() and 0xFF
                if (b0 != 0xFF || (b1 and 0xF6) != 0xF0) break

                val b2 = header[2].toInt() and 0xFF
                val b3 = header[3].toInt() and 0xFF
                val b4 = header[4].toInt() and 0xFF
                val b5 = header[5].toInt() and 0xFF
                val b6 = header[6].toInt() and 0xFF

                val srIndex = (b2 and 0x3C) ushr 2
                if (frameCount == 0) {
                    if (srIndex in SAMPLING_FREQUENCIES.indices) {
                        sampleRate = SAMPLING_FREQUENCIES[srIndex]
                    }
                    channelCount = ((b2 and 0x01) shl 2) or ((b3 and 0xC0) ushr 6)
                }

                val frameLength = ((b3 and 0x03) shl 11) or (b4 shl 3) or ((b5 and 0xE0) ushr 5)
                if (frameLength < 7) break

                val rawBlocks = b6 and 0x03
                val samples = (rawBlocks + 1) * 1024
                totalSamples += samples
                frameCount++

                if (frameCount == 1 && samples > 0) {
                    firstFrameBitrate = ((frameLength * 8L * sampleRate) / samples).toInt()
                }

                val toSkip = (frameLength - 7).toLong()
                var skipped = 0L
                while (skipped < toSkip) {
                    val s = bis.skip(toSkip - skipped)
                    if (s <= 0) break
                    skipped += s
                }
                if (skipped < toSkip) break
            }
        }

        if (frameCount == 0 || sampleRate <= 0) return null

        val durationMs = (totalSamples * 1000L) / sampleRate
        val totalAudioBytes = fileLength - audioOffset
        val avgBitrate = if (durationMs > 0) {
            ((totalAudioBytes * 8L * 1000L) / durationMs).toInt()
        } else {
            firstFrameBitrate
        }

        return AdtsAudioInfo(
            durationMs = durationMs,
            sampleRate = sampleRate,
            bitrate = avgBitrate,
            channelCount = channelCount
        )
    }
}

