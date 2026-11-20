package com.example.music

import com.example.model.ChordAnalysis
import com.example.model.MusicalScale
import kotlin.math.abs
import kotlin.math.pow

object MusicTheory {

    val PITCH_CLASS_NAMES = listOf(
        "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
    )

    private val BLACK_PITCH_CLASSES = setOf(1, 3, 6, 8, 10)

    fun isBlackKey(midiNote: Int): Boolean = (midiNote % 12) in BLACK_PITCH_CLASSES

    fun midiToPitchClass(midiNote: Int): Int = ((midiNote % 12) + 12) % 12

    fun midiToOctave(midiNote: Int): Int = (midiNote / 12) - 1

    fun midiToNoteName(midiNote: Int): String {
        val pc = midiToPitchClass(midiNote)
        val octave = midiToOctave(midiNote)
        return "${PITCH_CLASS_NAMES[pc]}$octave"
    }

    fun midiToPitchNameOnly(midiNote: Int): String =
        PITCH_CLASS_NAMES[midiToPitchClass(midiNote)]

    fun midiToFrequencyHz(midiNote: Int): Double =
        440.0 * 2.0.pow((midiNote - 69) / 12.0)

    fun isInScale(midiNote: Int, rootPitchClass: Int, scale: MusicalScale): Boolean {
        if (scale == MusicalScale.CHROMATIC) return true
        val rel = ((midiNote - rootPitchClass) % 12 + 12) % 12
        return rel in scale.intervals
    }

    fun snapToScale(midiNote: Int, rootPitchClass: Int, scale: MusicalScale): Int {
        if (scale == MusicalScale.CHROMATIC || isInScale(midiNote, rootPitchClass, scale)) {
            return midiNote
        }
        for (offset in 1..6) {
            val down = midiNote - offset
            if (down >= 21 && isInScale(down, rootPitchClass, scale)) return down
            val up = midiNote + offset
            if (up <= 108 && isInScale(up, rootPitchClass, scale)) return up
        }
        return midiNote
    }

    private data class ChordTemplate(
        val intervals: Set<Int>,
        val suffix: String,
        val qualityLabel: String,
        val consonance: Float
    )

    private val CHORD_TEMPLATES = listOf(
        // 4+ note chords first
        ChordTemplate(setOf(0, 4, 7, 11, 2), "maj9", "Luminous Major Ninth", 0.92f),
        ChordTemplate(setOf(0, 3, 7, 10, 2), "m9", "Velvet Minor Ninth", 0.88f),
        ChordTemplate(setOf(0, 4, 7, 11), "maj7", "Luminous Major Seventh", 0.90f),
        ChordTemplate(setOf(0, 3, 7, 10), "m7", "Warm Minor Seventh", 0.86f),
        ChordTemplate(setOf(0, 4, 7, 10), "7", "Dominant Seventh Tension", 0.74f),
        ChordTemplate(setOf(0, 3, 6, 10), "m7b5", "Half-Diminished Noir", 0.62f),
        ChordTemplate(setOf(0, 3, 6, 9), "dim7", "Symmetric Diminished", 0.54f),
        ChordTemplate(setOf(0, 4, 7, 9), "6", "Golden Major Sixth", 0.88f),
        ChordTemplate(setOf(0, 3, 7, 9), "m6", "Melancholic Minor Sixth", 0.80f),
        ChordTemplate(setOf(0, 4, 7, 2), "add9", "Open Add-Nine", 0.94f),
        ChordTemplate(setOf(0, 3, 7, 2), "m(add9)", "Cinematic Minor Add-Nine", 0.87f),
        ChordTemplate(setOf(0, 5, 7, 10), "7sus4", "Suspended Dominant", 0.78f),
        // Triads
        ChordTemplate(setOf(0, 4, 7), "", "Radiant Major Triad", 0.96f),
        ChordTemplate(setOf(0, 3, 7), "m", "Deep Minor Triad", 0.89f),
        ChordTemplate(setOf(0, 2, 7), "sus2", "Airy Suspended Second", 0.92f),
        ChordTemplate(setOf(0, 5, 7), "sus4", "Suspended Horizon", 0.85f),
        ChordTemplate(setOf(0, 3, 6), "dim", "Taut Diminished Triad", 0.56f),
        ChordTemplate(setOf(0, 4, 8), "aug", "Dreamlike Augmented", 0.66f),
        // Two-note dyads / intervals
        ChordTemplate(setOf(0, 7), "5", "Open Perfect Fifth", 0.98f),
        ChordTemplate(setOf(0, 5), " (P4)", "Resonant Perfect Fourth", 0.90f),
        ChordTemplate(setOf(0, 4), " (M3)", "Warm Major Third", 0.91f),
        ChordTemplate(setOf(0, 3), " (m3)", "Intimate Minor Third", 0.84f),
        ChordTemplate(setOf(0, 9), " (M6)", "Lyrical Major Sixth", 0.88f),
        ChordTemplate(setOf(0, 8), " (m6)", "Bittersweet Minor Sixth", 0.80f),
        ChordTemplate(setOf(0, 11), " (M7)", "Crystalline Major Seventh", 0.65f),
        ChordTemplate(setOf(0, 10), " (m7)", "Floating Minor Seventh", 0.72f),
        ChordTemplate(setOf(0, 2), " (M2)", "Close Whole Step", 0.64f),
        ChordTemplate(setOf(0, 6), " (TT)", "Tritone Gravity", 0.45f),
        ChordTemplate(setOf(0, 1), " (m2)", "Sharp Semitone Friction", 0.35f)
    )

    fun analyzeChord(activeMidiNotes: Set<Int>): ChordAnalysis? {
        if (activeMidiNotes.isEmpty()) return null
        val sorted = activeMidiNotes.sorted()
        val noteListText = sorted.joinToString(" · ") { midiToNoteName(it) }
        val lowestMidi = sorted.first()
        val primaryFreq = midiToFrequencyHz(lowestMidi)

        val pitchClasses = sorted.map { midiToPitchClass(it) }.distinct()
        if (pitchClasses.size == 1) {
            val rootName = PITCH_CLASS_NAMES[pitchClasses.first()]
            val quality = if (sorted.size > 1) "Octave Unison Resonance" else "Pure Fundamental Tone"
            return ChordAnalysis(
                symbol = rootName,
                qualityName = quality,
                activeNotesText = noteListText,
                primaryFrequencyHz = primaryFreq,
                consonance = 1.0f
            )
        }

        val bassPc = midiToPitchClass(lowestMidi)
        // Prefer matching with bass note as root first, then try other inversions
        val candidateRoots = listOf(bassPc) + pitchClasses.filter { it != bassPc }

        for (template in CHORD_TEMPLATES) {
            if (template.intervals.size != pitchClasses.size) continue
            for (root in candidateRoots) {
                val normalized = pitchClasses.map { ((it - root) + 12) % 12 }.toSet()
                if (normalized == template.intervals) {
                    val rootName = PITCH_CLASS_NAMES[root]
                    val slash = if (root != bassPc && pitchClasses.size >= 3) {
                        "/${PITCH_CLASS_NAMES[bassPc]}"
                    } else {
                        ""
                    }
                    return ChordAnalysis(
                        symbol = "$rootName${template.suffix}$slash",
                        qualityName = template.qualityLabel,
                        activeNotesText = noteListText,
                        primaryFrequencyHz = primaryFreq,
                        consonance = template.consonance
                    )
                }
            }
        }

        // Fallback polychord/cluster analysis
        val rootName = PITCH_CLASS_NAMES[bassPc]
        val avgIntervalConsonance = computeClusterConsonance(sorted)
        return ChordAnalysis(
            symbol = "$rootName Cluster",
            qualityName = "${pitchClasses.size}-Tone Harmonic Voicing",
            activeNotesText = noteListText,
            primaryFrequencyHz = primaryFreq,
            consonance = avgIntervalConsonance
        )
    }

    private fun computeClusterConsonance(sortedNotes: List<Int>): Float {
        if (sortedNotes.size < 2) return 1.0f
        var score = 0f
        var pairs = 0
        for (i in sortedNotes.indices) {
            for (j in i + 1 until sortedNotes.size) {
                val interval = abs(sortedNotes[j] - sortedNotes[i]) % 12
                score += when (interval) {
                    0, 7 -> 0.98f
                    4, 5, 9 -> 0.88f
                    3, 8 -> 0.80f
                    2, 10 -> 0.65f
                    11 -> 0.55f
                    6 -> 0.45f
                    else -> 0.35f
                }
                pairs++
            }
        }
        return if (pairs > 0) (score / pairs).coerceIn(0.25f, 1.0f) else 0.8f
    }
}
