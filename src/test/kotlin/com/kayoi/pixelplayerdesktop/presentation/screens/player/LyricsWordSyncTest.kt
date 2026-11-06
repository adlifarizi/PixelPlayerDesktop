package com.kayoi.pixelplayerdesktop.presentation.screens.player

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kayoi.pixelplayerdesktop.data.model.SyncedWord

class LyricsWordSyncTest {

    @Test
    fun resolveWordVisualStates_completesWordWhenPastExplicitEndTime() {
        val words = listOf(
            SyncedWord(time = 1000, word = "First", endTime = 1500),
            SyncedWord(time = 3000, word = "Second", endTime = 4000)
        )

        val statesActive = resolveWordVisualStates(
            words = words,
            positionMs = 1200L,
            lineStartTimeMs = 1000L,
            lineEndTimeMs = 5000L
        )
        assertEquals(LyricWordPhase.Active, statesActive[0].phase)
        assertEquals(LyricWordPhase.Future, statesActive[1].phase)

        val statesPause = resolveWordVisualStates(
            words = words,
            positionMs = 2000L,
            lineStartTimeMs = 1000L,
            lineEndTimeMs = 5000L
        )
        assertEquals(LyricWordPhase.Completed, statesPause[0].phase)
        assertEquals(1f, statesPause[0].progress)
        assertEquals(LyricWordPhase.Future, statesPause[1].phase)
    }
}

