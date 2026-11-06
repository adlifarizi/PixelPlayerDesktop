package com.kayoi.pixelplayerdesktop.utils

import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

import com.kayoi.pixelplayerdesktop.data.model.BackgroundVocals
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import com.kayoi.pixelplayerdesktop.data.model.SyncedWord

object LrcParser {
    private val LRC_LINE_REGEX = Pattern.compile("^\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})](.*)$")
    private val LRC_WORD_REGEX = Pattern.compile("<(\\d{2}):(\\d{2})[.:](\\d{2,3})>([^<]*)")
    private val LRC_WORD_TAG_REGEX = Regex("<\\d{2}:\\d{2}[.:]\\d{2,3}>")
    private val LRC_WORD_SPLIT_REGEX = Regex("(?=<\\d{2}:\\d{2}[.:]\\d{2,3}>)")
    private val LRC_TIMESTAMP_TAG_REGEX = Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]")
    private val TRANSLATION_CREDIT_REGEX = Regex("^\\s*by\\s*[:：].+", RegexOption.IGNORE_CASE)
    private val LRC_METADATA_PATTERN = Pattern.compile("^\\[[a-zA-Z]+:.*]$")

    private val LRC_BACKGROUND_LINE_REGEX = Regex("^\\[bg:(.*)]$", RegexOption.IGNORE_CASE)
    private val LRC_TRANSLATION_LINE_REGEX = Regex("^\\[tr:(.*)]$", RegexOption.IGNORE_CASE)
    private val LRC_VOICE_PREFIX_REGEX = Regex(
        "^(<\\d{2}:\\d{2}[.:]\\d{2,3}>)?\\s*v(\\d{1,4})\\s*:\\s*",
        RegexOption.IGNORE_CASE
    )

    private val KUGOU_LINE_REGEX = Pattern.compile("^\\[(\\d+),(\\d+)](.*)$")
    private val KUGOU_WORD_PATTERN = Pattern.compile("<(\\d+),(\\d+),(\\d+)>([^<]*)")
    private val KUGOU_JOINING_PUNCTUATION = setOf('\'', '\u2019', '-', '\u2010', '\u2011')
    private val TTML_TIMED_NODE_REGEX = Regex("<(?:p|span)\\b[^>]*\\bbegin\\s*=", RegexOption.IGNORE_CASE)

    fun hasSyncedTimestamps(content: String): Boolean {
        if (content.isBlank()) return false
        val trimmed = stripLeadingLyricsDocumentNoise(content)
        if (looksLikeTtmlDocument(trimmed)) {
            return TTML_TIMED_NODE_REGEX.containsMatchIn(trimmed)
        }
        return trimmed.lineSequence().take(200).any { line ->
            val clean = line.trim()
            if (clean.isEmpty() || LRC_METADATA_PATTERN.matcher(clean).matches()) false
            else LRC_TIMESTAMP_TAG_REGEX.containsMatchIn(clean) || KUGOU_LINE_REGEX.matcher(clean).matches()
        }
    }

    fun parse(content: String): Lyrics {
        if (content.isBlank()) {
            return Lyrics(plain = emptyList(), synced = emptyList())
        }

        val entireLyricsHasKana = content.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }
        val normalizedInput = stripLeadingLyricsDocumentNoise(content).toNfc()

        if (looksLikeTtmlDocument(normalizedInput)) {
            val converted = TtmlLyricsParser.parseToEnhancedLrc(normalizedInput)
                ?: return Lyrics(plain = emptyList(), synced = emptyList())
            return parse(converted)
        }

        if (looksLikeKugouFormat(normalizedInput)) {
            return parseKugouLyrics(normalizedInput, entireLyricsHasKana)
        }

        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()
        var isSynced = false
        var holdsOpeningBackground = false

        normalizedInput.lines().forEach { rawLine ->
            val line = sanitizeLrcLine(rawLine)
            if (line.isEmpty()) return@forEach

            val backgroundMatch = LRC_BACKGROUND_LINE_REGEX.matchEntire(line)
            if (backgroundMatch != null) {
                val previous = syncedLines.lastOrNull()
                val background = parseBackgroundVocals(
                    rawBody = backgroundMatch.groupValues[1],
                    anchorTimeMs = previous?.time?.toLong() ?: 0L
                ) ?: return@forEach
                if (previous != null) {
                    syncedLines[syncedLines.lastIndex] =
                        previous.copy(background = previous.background.mergedWith(background))
                } else if (!background.words.isNullOrEmpty()) {
                    isSynced = true
                    holdsOpeningBackground = true
                    syncedLines.add(
                        SyncedLine(
                            time = background.words.first().time,
                            line = "",
                            background = background
                        )
                    )
                }
                return@forEach
            }

            val translationMatch = LRC_TRANSLATION_LINE_REGEX.matchEntire(line)
            if (translationMatch != null) {
                val previous = syncedLines.lastOrNull()
                val translation = stripFormatCharacters(translationMatch.groupValues[1]).trim()
                if (previous != null && translation.isNotEmpty()) {
                    syncedLines[syncedLines.lastIndex] = previous.copy(
                        translation = previous.translation?.let { "$it\n$translation" } ?: translation
                    )
                }
                return@forEach
            }

            if (LRC_METADATA_PATTERN.matcher(line).matches()) return@forEach

            val lineMatcher = LRC_LINE_REGEX.matcher(line)
            if (lineMatcher.matches()) {
                isSynced = true
                val minutes = lineMatcher.group(1)?.toLong() ?: 0
                val seconds = lineMatcher.group(2)?.toLong() ?: 0
                val fraction = lineMatcher.group(3)?.toLong() ?: 0
                val textWithTags = stripFormatCharacters(lineMatcher.group(4)?.trim() ?: "")
                val (voice, text) = splitVoicePrefix(stripLrcTimestamps(textWithTags))

                val millis = if (lineMatcher.group(3)?.length == 2) fraction * 10 else fraction
                val lineTimestamp = minutes * 60 * 1000 + seconds * 1000 + millis

                val syncedLine = if (text.contains(LRC_WORD_TAG_REGEX)) {
                    val words = parseTimedWords(text, lineTimestamp)
                    val displayText = LRC_WORD_TAG_REGEX.replace(text, "").trim()
                    SyncedLine(
                        time = lineTimestamp.toInt(),
                        line = displayText,
                        words = words.takeIf { it.isNotEmpty() },
                        voice = voice
                    )
                } else {
                    SyncedLine(time = lineTimestamp.toInt(), line = text, voice = voice)
                }

                val opening = syncedLines.singleOrNull()?.takeIf {
                    holdsOpeningBackground &&
                        it.line.isBlank() &&
                        it.translation == null &&
                        it.time == syncedLine.time
                }
                holdsOpeningBackground = false
                if (opening != null) {
                    syncedLines[0] = syncedLine.copy(background = opening.background)
                } else {
                    syncedLines.add(syncedLine)
                }
            } else {
                val stripped = stripLrcTimestamps(stripFormatCharacters(line))
                if (isSynced && syncedLines.isNotEmpty()) {
                    val last = syncedLines.removeAt(syncedLines.lastIndex)
                    val mergedLineText = if (last.line.isEmpty()) {
                        stripped
                    } else {
                        last.line + "\n" + stripped
                    }
                    syncedLines.add(last.copy(line = mergedLineText))
                } else {
                    plainLines.add(stripped)
                }
            }
        }

        return if (isSynced && syncedLines.isNotEmpty()) {
            finalizeSyncedLyrics(syncedLines, entireLyricsHasKana)
        } else {
            val targetPlain = plainLines.ifEmpty { normalizedInput.lines().filter { it.isNotBlank() } }
            val processedPlain = targetPlain.map { line ->
                val romanized = MultiLangRomanizer.romanizeText(line, entireLyricsHasKana)
                if (!romanized.isNullOrEmpty()) "$line\n$romanized" else line
            }
            Lyrics(plain = processedPlain, areFromRemote = false)
        }
    }

    private fun finalizeSyncedLyrics(
        syncedLines: List<SyncedLine>,
        entireLyricsHasKana: Boolean
    ): Lyrics {
        val sortedSyncedLines = syncedLines.sortedBy { it.time }
        val pairedLines = pairTranslationLines(sortedSyncedLines).map { line ->
            val romanized = MultiLangRomanizer.romanizeText(line.line, entireLyricsHasKana)
            line.copy(romanization = romanized)
        }
        val plainVersion = pairedLines.map { line ->
            buildString {
                val mainText = line.line.ifBlank { line.background?.text.orEmpty() }
                append(mainText)
                if (line.line.isNotBlank() && !line.background?.text.isNullOrBlank()) {
                    append(" ").append(line.background.text)
                }
                if (!line.romanization.isNullOrEmpty()) append("\n").append(line.romanization)
                if (!line.translation.isNullOrEmpty()) append("\n").append(line.translation)
            }
        }
        return Lyrics(synced = pairedLines, plain = plainVersion, areFromRemote = false)
    }

    private fun looksLikeTtmlDocument(value: String): Boolean {
        fun String.startsWithTtmlRoot(): Boolean =
            startsWith("<tt", ignoreCase = true) ||
                startsWith("<smil", ignoreCase = true)

        if (value.startsWithTtmlRoot()) return true
        if (!value.startsWith("<?xml", ignoreCase = true)) return false

        return value
            .substringAfter("?>", missingDelimiterValue = "")
            .trimStart()
            .startsWithTtmlRoot()
    }

    private fun looksLikeKugouFormat(text: String): Boolean {
        return text.lines().any { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@any false
            if (LRC_METADATA_PATTERN.matcher(line).matches()) return@any false
            val matcher = KUGOU_LINE_REGEX.matcher(line)
            matcher.matches() && KUGOU_WORD_PATTERN.matcher(matcher.group(3).orEmpty()).find()
        }
    }

    private fun parseKugouLyrics(text: String, entireLyricsHasKana: Boolean): Lyrics {
        val globalOffsetMs = text.lines()
            .firstOrNull { it.trim().startsWith("[offset:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.substringBefore(']')
            ?.trim()
            ?.toLongOrNull() ?: 0L

        val syncedLines = mutableListOf<SyncedLine>()

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || LRC_METADATA_PATTERN.matcher(line).matches()) continue

            val headerMatcher = KUGOU_LINE_REGEX.matcher(line)
            if (!headerMatcher.matches()) continue
            val lineStartMs = (headerMatcher.group(1)?.toLongOrNull() ?: continue) + globalOffsetMs
            val body = headerMatcher.group(3) ?: ""

            val words = mutableListOf<SyncedWord>()
            val wordMatcher = KUGOU_WORD_PATTERN.matcher(body)
            var previousEndsWithSpace = true

            while (wordMatcher.find()) {
                val wordOffsetMs = wordMatcher.group(1)?.toLongOrNull() ?: 0L
                val wordDurationMs = wordMatcher.group(2)?.toLongOrNull() ?: 0L
                val rawText = wordMatcher.group(4) ?: ""

                val wordText = rawText.trim()
                val startsNew = words.isEmpty() ||
                    previousEndsWithSpace ||
                    rawText.firstOrNull()?.isWhitespace() == true ||
                    (wordOffsetMs > 0L && shouldSeparateKugouTokens(words.lastOrNull()?.word, wordText))
                previousEndsWithSpace = rawText.lastOrNull()?.isWhitespace() == true

                if (wordText.isEmpty()) continue

                val wordStartMs = lineStartMs + wordOffsetMs
                words.addSyllable(
                    SyncedWord(
                        time = wordStartMs.toInt(),
                        word = wordText,
                        startsNewWord = startsNew,
                        endTime = (wordStartMs + wordDurationMs).toInt().takeIf { wordDurationMs > 0L }
                    )
                )
            }

            val plainText = KUGOU_WORD_PATTERN.toRegex().replace(body) { it.groupValues[4] }.trim()
            if (plainText.isEmpty() && words.isEmpty()) continue

            syncedLines.add(
                SyncedLine(
                    time = lineStartMs.toInt(),
                    line = plainText,
                    words = words.takeIf { it.isNotEmpty() }
                )
            )
        }

        if (syncedLines.isEmpty()) return Lyrics(plain = emptyList(), synced = emptyList(), areFromRemote = false)

        return finalizeSyncedLyrics(syncedLines, entireLyricsHasKana)
    }

    private fun shouldSeparateKugouTokens(previous: String?, current: String): Boolean {
        val previousChar = previous?.lastOrNull { !it.isWhitespace() } ?: return false
        val currentChar = current.firstOrNull { !it.isWhitespace() } ?: return false

        if (currentChar.isLetterOrDigit()) {
            if (previousChar.isLetterOrDigit()) {
                return usesVisibleInterWordSpacing(previousChar) &&
                    usesVisibleInterWordSpacing(currentChar)
            }
            return previousChar !in KUGOU_JOINING_PUNCTUATION
        }
        return false
    }

    private fun usesVisibleInterWordSpacing(char: Char): Boolean = when (
        Character.UnicodeScript.of(char.code)
    ) {
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.BOPOMOFO,
        Character.UnicodeScript.THAI,
        Character.UnicodeScript.LAO,
        Character.UnicodeScript.KHMER,
        Character.UnicodeScript.MYANMAR -> false
        else -> true
    }

    private fun splitVoicePrefix(text: String): Pair<Int?, String> {
        val match = LRC_VOICE_PREFIX_REGEX.find(text) ?: return null to text
        val voice = match.groupValues[2].toIntOrNull() ?: return null to text
        return voice to match.groupValues[1] + text.substring(match.range.last + 1)
    }

    private fun parseTimedWords(text: String, lineTimestampMs: Long): List<SyncedWord> {
        val words = mutableListOf<SyncedWord>()
        var pendingWordBoundary = false

        for (part in text.split(LRC_WORD_SPLIT_REGEX)) {
            if (part.isEmpty()) continue
            val wordMatcher = LRC_WORD_REGEX.matcher(part)
            if (wordMatcher.find()) {
                val wordMinutes = wordMatcher.group(1)?.toLong() ?: 0
                val wordSeconds = wordMatcher.group(2)?.toLong() ?: 0
                val wordFraction = wordMatcher.group(3)?.toLong() ?: 0
                val wordText = stripFormatCharacters(wordMatcher.group(4) ?: "")
                val timedWordTextRaw = wordText
                    .substringBefore('\n')
                    .substringBefore('\r')
                    .substringBefore("\\n")
                    .substringBefore("\\r")
                val startsNewWord = words.isEmpty() ||
                    pendingWordBoundary ||
                    timedWordTextRaw.firstOrNull()?.isWhitespace() == true
                val timedWordText = timedWordTextRaw.trim()
                val endsWithWhitespace = timedWordTextRaw.lastOrNull()?.isWhitespace() == true

                pendingWordBoundary = if (timedWordText.isEmpty()) {
                    pendingWordBoundary || endsWithWhitespace
                } else {
                    endsWithWhitespace
                }
                val wordMillis = if (wordMatcher.group(3)?.length == 2) wordFraction * 10 else wordFraction
                val wordTimestamp = (wordMinutes * 60 * 1000 + wordSeconds * 1000 + wordMillis).toInt()
                if (timedWordText.isEmpty()) {
                    words.endLastSyllableAt(wordTimestamp)
                } else {
                    words.addSyllable(
                        SyncedWord(
                            time = wordTimestamp,
                            word = timedWordText,
                            startsNewWord = startsNewWord
                        )
                    )
                }
            } else {
                if (words.isEmpty()) {
                    val leading = stripFormatCharacters(part)
                    val startsNewWord = pendingWordBoundary || leading.firstOrNull()?.isWhitespace() == true
                    val visibleLeading = leading.trim()
                    pendingWordBoundary = leading.lastOrNull()?.isWhitespace() == true
                    if (visibleLeading.isNotEmpty()) {
                        words.add(
                            SyncedWord(
                                time = lineTimestampMs.toInt(),
                                word = visibleLeading,
                                startsNewWord = words.isEmpty() || startsNewWord
                            )
                        )
                    }
                } else if (part.any { it.isWhitespace() }) {
                    pendingWordBoundary = true
                }
            }
        }
        return words
    }

    private fun MutableList<SyncedWord>.endLastSyllableAt(timeMs: Int) {
        val last = lastOrNull() ?: return
        if (last.endTime == null && timeMs > last.time) {
            this[lastIndex] = last.copy(endTime = timeMs)
        }
    }

    private fun MutableList<SyncedWord>.addSyllable(syllable: SyncedWord) {
        val previous = lastOrNull()
        if (previous == null || !syllable.word.startsWithCombiningMark()) {
            add(syllable)
            return
        }
        this[lastIndex] = previous.copy(
            word = (previous.word + syllable.word).toNfc(),
            endTime = listOfNotNull(previous.endTime, syllable.endTime).maxOrNull()
        )
    }

    private fun String.startsWithCombiningMark(): Boolean {
        if (isEmpty()) return false
        return when (Character.getType(codePointAt(0)).toByte()) {
            Character.NON_SPACING_MARK,
            Character.ENCLOSING_MARK,
            Character.COMBINING_SPACING_MARK -> true
            else -> false
        }
    }

    private fun parseBackgroundVocals(rawBody: String, anchorTimeMs: Long): BackgroundVocals? {
        val body = stripFormatCharacters(rawBody).trim()
        val text = LRC_WORD_TAG_REGEX.replace(body, "").trim()
        if (text.isEmpty()) return null
        val words = if (body.contains(LRC_WORD_TAG_REGEX)) parseTimedWords(body, anchorTimeMs) else emptyList()
        return BackgroundVocals(text = text, words = words.takeIf { it.isNotEmpty() })
    }

    private fun BackgroundVocals?.mergedWith(next: BackgroundVocals): BackgroundVocals {
        if (this == null) return next
        val mergedWords = if (words != null && next.words != null) {
            words + next.words.mapIndexed { index, word ->
                if (index == 0) word.copy(startsNewWord = true) else word
            }
        } else {
            null
        }
        return BackgroundVocals(text = "$text ${next.text}", words = mergedWords)
    }

    internal fun pairTranslationLines(lines: List<SyncedLine>): List<SyncedLine> {
        if (lines.size < 2) return lines
        val result = mutableListOf<SyncedLine>()
        var i = 0
        while (i < lines.size) {
            val current = lines[i]
            val next = lines.getOrNull(i + 1)
            if (
                next != null &&
                next.time == current.time &&
                current.translation == null &&
                current.line.isNotBlank() &&
                next.line.isNotBlank() &&
                next.words == null &&
                (next.voice == null || next.voice == current.voice)
            ) {
                val translationParts = mutableListOf(next.line)
                var consumed = 2
                while (true) {
                    val trailing = lines.getOrNull(i + consumed) ?: break
                    if (trailing.time != current.time || !isTranslationCreditLine(trailing.line)) break
                    translationParts.add(trailing.line)
                    consumed++
                }
                result.add(
                    current.copy(
                        translation = translationParts.joinToString("\n"),
                        background = current.background ?: next.background
                    )
                )
                i += consumed
            } else {
                result.add(current)
                i++
            }
        }
        return result
    }

    internal fun isTranslationCreditLine(line: String): Boolean {
        val normalized = stripLrcTimestamps(line).trim()
        return normalized.isNotEmpty() && TRANSLATION_CREDIT_REGEX.matches(normalized)
    }

    internal fun stripLrcTimestamps(value: String): String {
        if (value.isEmpty()) return value
        return LRC_TIMESTAMP_TAG_REGEX.replace(value, "").trimStart()
    }

    internal fun formatLrcTimestamp(timeMs: Int): String {
        val safeTimeMs = timeMs.coerceAtLeast(0)
        val totalSeconds = safeTimeMs / 1_000
        val hundredths = (safeTimeMs % 1_000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", totalSeconds / 60, totalSeconds % 60, hundredths)
    }

    internal fun translationLrcLine(timestamp: String, mainLine: String, translationLine: String): String =
        if (mainLine.isBlank()) "[tr:$translationLine]" else timestamp + translationLine

    fun toLrcString(lyrics: Lyrics, preferSynced: Boolean = true): String {
        if (preferSynced && !lyrics.synced.isNullOrEmpty()) {
            return toEnhancedLrc(lyrics.synced, includeWordTimings = true)
        } else if (!lyrics.plain.isNullOrEmpty()) {
            return lyrics.plain.joinToString("\n") { it.substringBefore("\n") }
        } else if (!lyrics.synced.isNullOrEmpty()) {
            return toEnhancedLrc(lyrics.synced, includeWordTimings = false)
        }
        return ""
    }

    fun toEnhancedLrc(lines: List<SyncedLine>, includeWordTimings: Boolean = true): String {
        return lines.sortedBy { it.time }.flatMap { line ->
            val timestamp = "[${formatLrcTimestamp(line.time)}]"
            buildList {
                val body = line.words
                    ?.takeIf { includeWordTimings && it.isNotEmpty() }
                    ?.let(::formatTimedWords)
                    ?: line.line
                add(timestamp + line.voice?.let { "v$it:" }.orEmpty() + body)

                line.background?.let { background ->
                    val backgroundBody = background.words
                        ?.takeIf { includeWordTimings && it.isNotEmpty() }
                        ?.let(::formatTimedWords)
                        ?: background.text
                    if (backgroundBody.isNotBlank()) add("[bg:$backgroundBody]")
                }

                line.translation
                    ?.lines()
                    ?.filter { it.isNotBlank() }
                    ?.forEach { translationLine -> add(translationLrcLine(timestamp, line.line, translationLine)) }
            }
        }.joinToString("\n")
    }

    private fun formatTimedWords(words: List<SyncedWord>): String = buildString {
        words.forEachIndexed { index, word ->
            if (index > 0 && word.startsNewWord) append(' ')
            append('<').append(formatLrcTimestamp(word.time)).append('>').append(word.word)
            word.endTime
                ?.takeIf { it > word.time }
                ?.let { append('<').append(formatLrcTimestamp(it)).append('>') }
        }
    }

    private fun stripLeadingLyricsDocumentNoise(value: String): String {
        return value.trimStart { char ->
            char.isWhitespace() ||
                char == '\uFEFF' ||
                Character.getType(char).toByte() == Character.FORMAT
        }
    }

    private fun String.toNfc(): String =
        if (Normalizer.isNormalized(this, Normalizer.Form.NFC)) this else Normalizer.normalize(this, Normalizer.Form.NFC)

    private fun sanitizeLrcLine(rawLine: String): String {
        if (rawLine.isEmpty()) return rawLine

        val withoutTerminators = rawLine
            .trimEnd('\r', '\n')
            .filterNot { char ->
                Character.getType(char).toByte() == Character.FORMAT ||
                    (Character.isISOControl(char) && char != '\t')
            }
            .trimEnd('\uFEFF')

        val trimmedPrefix = withoutTerminators.trimStart { it.isWhitespace() }
        val firstBracket = trimmedPrefix.indexOf('[')
        val hasLrcTagAtBracket = firstBracket > 0 && (
            LRC_TIMESTAMP_TAG_REGEX.find(trimmedPrefix)?.range?.first == firstBracket ||
                trimmedPrefix.startsWith("[bg:", startIndex = firstBracket, ignoreCase = true) ||
                trimmedPrefix.startsWith("[tr:", startIndex = firstBracket, ignoreCase = true)
        )
        return if (hasLrcTagAtBracket) {
            trimmedPrefix.substring(firstBracket)
        } else {
            trimmedPrefix
        }
    }

    private fun stripFormatCharacters(value: String): String {
        val cleaned = value.filterNot { char ->
            char.category == CharCategory.FORMAT ||
                (char.isISOControl() && char != '\t')
        }

        return when (cleaned) {
            "\"", "'" -> ""
            else -> cleaned
        }
    }
}
