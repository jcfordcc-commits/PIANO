package com.example

import com.example.model.MusicalScale
import com.example.music.MusicTheory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicTheoryTest {

    @Test
    fun `a4 midi 69 resolves to 440Hz and note name A4`() {
        assertEquals(440.0, MusicTheory.midiToFrequencyHz(69), 0.01)
        assertEquals("A4", MusicTheory.midiToNoteName(69))
        assertEquals("C4", MusicTheory.midiToNoteName(60))
    }

    @Test
    fun `chord analyzer identifies major seventh and minor seventh chords`() {
        val cmaj7 = MusicTheory.analyzeChord(setOf(60, 64, 67, 71))
        assertNotNull(cmaj7)
        assertEquals("Cmaj7", cmaj7!!.symbol)

        val dm7 = MusicTheory.analyzeChord(setOf(62, 65, 69, 72))
        assertNotNull(dm7)
        assertEquals("Dm7", dm7!!.symbol)
    }

    @Test
    fun `scale membership and snapping keep notes in chosen scale`() {
        assertTrue(MusicTheory.isInScale(60, 0, MusicalScale.MAJOR))
        assertFalse(MusicTheory.isInScale(61, 0, MusicalScale.MAJOR))

        val snapped = MusicTheory.snapToScale(61, 0, MusicalScale.MAJOR)
        assertTrue(MusicTheory.isInScale(snapped, 0, MusicalScale.MAJOR))
    }
}
