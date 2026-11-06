package com.kayoi.pixelplayerdesktop.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TtmlLyricsParserTest {

    private fun ttml(head: String = "", body: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
          <head><metadata>$head</metadata></head>
          <body><div>$body</div></body>
        </tt>
    """.trimIndent()

    @Test
    fun numberedAgents_becomeVoicePrefixes() {
        val lrc = TtmlLyricsParser.parseToEnhancedLrc(
            ttml(
                body = """
                    <p begin="1.000" ttm:agent="v1">First singer</p>
                    <p begin="3.000" ttm:agent="v2">Second singer</p>
                """.trimIndent()
            )
        )

        assertEquals("[00:01.00]v1:First singer\n[00:03.00]v2:Second singer", lrc)
    }

    @Test
    fun namedAgents_areNumberedByFirstAppearanceAndGroupsSingInTheMiddle() {
        val synced = requireNotNull(
            LrcParser.parse(
                ttml(
                    head = """
                        <ttm:agent type="person" xml:id="lead"/>
                        <ttm:agent type="person" xml:id="guest"/>
                        <ttm:agent type="group" xml:id="everyone"/>
                    """.trimIndent(),
                    body = """
                        <p begin="1.000" ttm:agent="guest">Guest</p>
                        <p begin="2.000" ttm:agent="lead">Lead</p>
                        <p begin="3.000" ttm:agent="guest">Guest again</p>
                        <p begin="4.000" ttm:agent="everyone">All</p>
                    """.trimIndent()
                )
            ).synced
        )

        assertEquals(listOf(1, 2, 1, 1_000), synced.map { it.voice })
    }

    @Test
    fun backgroundSpans_becomeBackgroundVocalsInsteadOfJoiningTheLine() {
        val synced = requireNotNull(
            LrcParser.parse(
                ttml(
                    body = """
                        <p begin="2.000" end="6.000" ttm:agent="v2"><span begin="2.000" end="2.400">Whis</span><span begin="2.400" end="3.000">per</span> <span begin="3.000" end="3.500">now</span><span ttm:role="x-bg"><span begin="4.000" end="4.500">(whis</span><span begin="4.500" end="5.000">per)</span></span></p>
                    """.trimIndent()
                )
            ).synced
        )

        val line = synced.single()
        assertEquals(2, line.voice)
        assertEquals("Whisper now", line.line)
        val lineWords = requireNotNull(line.words)
        assertEquals(listOf(true, false, true), lineWords.map { it.startsNewWord })
        val background = requireNotNull(line.background)
        assertEquals("(whisper)", background.text)
        val bgWords = requireNotNull(background.words)
        assertEquals(listOf("(whis", "per)"), bgWords.map { it.word })
        assertEquals(listOf(true, false), bgWords.map { it.startsNewWord })
    }

    @Test
    fun paragraphSungOnlyInTheBackground_isKeptAsAnEmptyLineWithBackgroundVocals() {
        val lyrics = LrcParser.parse(
            ttml(
                body = """
                    <p begin="1.000" ttm:agent="v1">Lead line</p>
                    <p begin="10.000" ttm:agent="v2"><span ttm:role="x-bg"><span begin="10.000" end="10.500">(Ooh</span> <span begin="10.500" end="11.200">ooh)</span></span></p>
                """.trimIndent()
            )
        )

        val synced = requireNotNull(lyrics.synced)
        assertEquals(listOf("Lead line", ""), synced.map { it.line })
        val backingLine = synced[1]
        assertEquals(10_000, backingLine.time)
        assertEquals(2, backingLine.voice)
        val background = requireNotNull(backingLine.background)
        assertEquals("(Ooh ooh)", background.text)
        val bgWords = requireNotNull(background.words)
        assertEquals(listOf("(Ooh", "ooh)"), bgWords.map { it.word })
        assertEquals(11_200, bgWords.last().endTime)
        val plain = requireNotNull(lyrics.plain)
        assertEquals("(Ooh ooh)", plain[1])
    }

    @Test
    fun rejectsDtdDeclarations() {
        val malformed = """
            <!DOCTYPE tt SYSTEM "http://malicious.example.com">
            <tt xmlns="http://www.w3.org/ns/ttml">
                <body><div><p begin="0.000">Text</p></div></body>
            </tt>
        """.trimIndent()
        assertNull(TtmlLyricsParser.parseToEnhancedLrc(malformed))
    }

    @Test
    fun parsesMetricTimeWithMillisecondUnits() {
        val content = ttml(
            body = """
                <p begin="1500ms">Text</p>
            """.trimIndent()
        )
        val lrc = TtmlLyricsParser.parseToEnhancedLrc(content)
        assertEquals("[00:01.50]Text", lrc)
    }
}
