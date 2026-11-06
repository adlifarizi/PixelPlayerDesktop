package com.kayoi.pixelplayerdesktop.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import com.kayoi.pixelplayerdesktop.data.model.SyncedWord

class LrcParserTest {

    @Test
    fun parseSyncedLyricsSuccessfully() {
        val lrcContent = """
            [00:12.50]First line
            [01:05.120]Second line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        assertEquals(2, result.synced?.size)
        assertEquals(12500, result.synced?.get(0)?.time)
        assertEquals("First line", result.synced?.get(0)?.line)
        assertEquals(65120, result.synced?.get(1)?.time)
        assertEquals("Second line", result.synced?.get(1)?.line)
    }

    @Test
    fun toLrcStringFormatsSyncedLyrics() {
        val lyrics = Lyrics(
            synced = listOf(
                SyncedLine(time = 12500, line = "First line"),
                SyncedLine(time = 65000, line = "Second line")
            )
        )

        val lrcString = LrcParser.toLrcString(lyrics)
        assertTrue(lrcString.contains("[00:12.50]First line"))
        assertTrue(lrcString.contains("[01:05.00]Second line"))
    }

    @Test
    fun toLrcStringFormatsPlainLyrics() {
        val lyrics = Lyrics(
            plain = listOf("Line one", "Line two")
        )

        val lrcString = LrcParser.toLrcString(lyrics)
        assertEquals("Line one\nLine two", lrcString)
    }

    @Test
    fun parseWordSyncedLyricsSuccessfully() {
        val lrcContent = """
            [00:01.00]<00:01.00>Hel<00:01.50>lo <00:02.00>world
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        assertEquals(1, result.synced?.size)
        val line = result.synced?.firstOrNull()
        assertEquals("Hello world", line?.line)
        assertEquals(3, line?.words?.size)

        val words = line?.words ?: emptyList()
        assertEquals("Hel", words[0].word)
        assertEquals(1000, words[0].time)
        assertTrue(words[0].startsNewWord)

        assertEquals("lo", words[1].word)
        assertEquals(1500, words[1].time)
        assertEquals(false, words[1].startsNewWord)

        assertEquals("world", words[2].word)
        assertEquals(2000, words[2].time)
        assertTrue(words[2].startsNewWord)
    }

    @Test
    fun toLrcStringFormatsWordSyncedLyrics() {
        val lyrics = Lyrics(
            synced = listOf(
                SyncedLine(
                    time = 1000,
                    line = "Hello world",
                    words = listOf(
                        SyncedWord(1000, "Hello", startsNewWord = true),
                        SyncedWord(2000, "world", startsNewWord = true)
                    )
                )
            )
        )

        val lrcString = LrcParser.toLrcString(lyrics)
        assertEquals("[00:01.00]<00:01.00>Hello <00:02.00>world", lrcString)
    }

    @Test
    fun parseKugouLyricsSuccessfully() {
        val kugouContent = """
            [offset:100]
            [500,1000]<0,400,0>Intro <400,600,0>sound
            [1000,2000]<0,400,0>First <400,500,0>word <900,1000,0>line
        """.trimIndent()

        val result = LrcParser.parse(kugouContent)
        val synced = requireNotNull(result.synced)
        assertEquals(2, synced.size)

        val introLine = synced[0]
        assertEquals(600, introLine.time)
        assertEquals("Intro sound", introLine.line)

        val line = synced[1]
        assertEquals(1100, line.time)
        assertEquals("First word line", line.line)
        assertEquals(3, line.words?.size)
        assertEquals(1100, line.words?.get(0)?.time)
        assertEquals(1500, line.words?.get(0)?.endTime)
        assertEquals("First", line.words?.get(0)?.word)
    }

    @Test
    fun parseEnhancedLrcWithVoiceAndBackingVocals() {
        val lrcContent = """
            [00:01.00]v1:Lead vocal
            [bg: (backing)]
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        val line = requireNotNull(result.synced?.firstOrNull())
        assertEquals(1, line.voice)
        assertEquals("Lead vocal", line.line)
        val background = requireNotNull(line.background)
        assertEquals("(backing)", background.text)
    }

    @Test
    fun pairTranslationLinesMergesIdenticalTimestamps() {
        val lrcContent = """
            [00:01.00]Original lyrics
            [00:01.00]Translated lyrics
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        val synced = requireNotNull(result.synced)
        assertEquals(1, synced.size)
        assertEquals("Original lyrics", synced.first().line)
        assertEquals("Translated lyrics", synced.first().translation)
    }

    @Test
    fun parseTranslationTagUnderNormalLine() {
        val lrcContent = """
            [00:01.00]Original lyrics
            [tr:Translated lyrics]
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        val synced = requireNotNull(result.synced)
        assertEquals(1, synced.size)
        assertEquals("Original lyrics", synced.first().line)
        assertEquals("Translated lyrics", synced.first().translation)
    }

    @Test
    fun pairTranslationLinesPreservesSimultaneousDuetsWithWordTimings() {
        val lrcContent = """
            [00:01.00]<00:01.00>First <00:01.50>singer
            [00:01.00]<00:01.00>Second <00:01.50>singer
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        val synced = requireNotNull(result.synced)
        assertEquals(2, synced.size)
        assertEquals("First singer", synced[0].line)
        assertNotNull(synced[0].words)
        assertEquals("Second singer", synced[1].line)
        assertNotNull(synced[1].words)
    }

    @Test
    fun parseKugouLyricsWithUppercaseOffsetAndRomanization() {
        val kugouContent = """
            [OFFSET: 250]
            [1000,2000]<0,500,0>你好
        """.trimIndent()

        val result = LrcParser.parse(kugouContent)
        val synced = requireNotNull(result.synced)
        assertEquals(1, synced.size)
        assertEquals(1250, synced.first().time)
        assertEquals("你好", synced.first().line)
        assertEquals("Ni hao", synced.first().romanization)
    }

    @Test
    fun hasSyncedTimestampsDetectsFormatsAccurately() {
        assertTrue(LrcParser.hasSyncedTimestamps("[00:01.00]Hello world"))
        assertTrue(LrcParser.hasSyncedTimestamps("[1000,2000]<0,400,0>Intro"))
        assertTrue(LrcParser.hasSyncedTimestamps("<tt><body><div><p begin=\"1.000\">Hi</p></div></body></tt>"))
        assertTrue(!LrcParser.hasSyncedTimestamps("Just some regular plain text lyrics without timestamps"))
    }

    @Test
    fun sanitizePreservesMidLineBracketsInPlainText() {
        val plain = """
            Verse 1
            He said [crying] why did you leave?
            Chorus [repeat 2x]
        """.trimIndent()

        val result = LrcParser.parse(plain)
        val plainLines = requireNotNull(result.plain)
        assertEquals(3, plainLines.size)
        assertEquals("He said [crying] why did you leave?", plainLines[1])
        assertEquals("Chorus [repeat 2x]", plainLines[2])
    }

    @Test
    fun sanitizeStripsLeadingNoiseBeforeTimestamp() {
        val lrc = "01. [00:01.50]Hello world"
        val result = LrcParser.parse(lrc)
        val synced = requireNotNull(result.synced)
        assertEquals(1, synced.size)
        assertEquals(1500, synced[0].time)
        assertEquals("Hello world", synced[0].line)
    }
}
