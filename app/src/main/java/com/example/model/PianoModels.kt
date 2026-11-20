package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BiolumCyan
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EmeraldHarmonic
import com.example.ui.theme.VioletAurora

enum class SoundPreset(
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val accentColor: Color
) {
    CONCERT_GRAND(
        displayName = "Concert Grand",
        shortName = "GRAND",
        subtitle = "Warm acoustic overtones & hammer transient",
        accentColor = BrushedBrass
    ),
    RHODES_VELVET(
        displayName = "Rhodes Velvet",
        shortName = "RHODES",
        subtitle = "Tine bell chime with gentle stereo tremolo",
        accentColor = BiolumCyan
    ),
    CELESTA_GLASS(
        displayName = "Celesta Glass",
        shortName = "GLASS",
        subtitle = "Crystalline harmonics & ethereal bloom",
        accentColor = VioletAurora
    ),
    SOLAR_SYNTH(
        displayName = "Solar Warmth",
        shortName = "SOLAR",
        subtitle = "Detuned analog pad-piano hybrid",
        accentColor = EmeraldHarmonic
    )
}

enum class MusicalScale(
    val displayName: String,
    val moodTag: String,
    val intervals: Set<Int>
) {
    CHROMATIC("Chromatic", "Free Expression", (0..11).toSet()),
    MAJOR("Major Ionian", "Bright & Open", setOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR("Natural Minor", "Deep & Cinematic", setOf(0, 2, 3, 5, 7, 8, 10)),
    DORIAN("Dorian", "Soulful & Nocturnal", setOf(0, 2, 3, 5, 7, 9, 10)),
    LYDIAN("Lydian", "Luminous Dream", setOf(0, 2, 4, 6, 7, 9, 11)),
    HIRAJOSHI("Hirajoshi", "Japanese Koto", setOf(0, 2, 3, 7, 8)),
    BLUES("Blues Hex", "Velvet Groove", setOf(0, 3, 5, 6, 7, 10)),
    WHOLE_TONE("Whole Tone", "Suspended Mirage", setOf(0, 2, 4, 6, 8, 10))
}

enum class ShimmerMode(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    PURE("Pure Direct", "PURE", "Direct uncolored polyphonic response"),
    OCTAVE_BLOOM("Octave Bloom", "OCT+", "Echoes each note +1 octave in soft shimmer"),
    FIFTH_CASCADE("Fifth Cascade", "5TH+", "Harp cascade echoing +5th and +octave"),
    HARP_SHIMMER("Aether Harp", "HARP", "Triplet shimmer across octave & harmonic fifth")
}

data class ChordAnalysis(
    val symbol: String,
    val qualityName: String,
    val activeNotesText: String,
    val primaryFrequencyHz: Double,
    val consonance: Float
)

data class ActiveVoiceVisual(
    val midiNote: Int,
    val amplitude: Float,
    val frequencyHz: Double,
    val isSustainedOnly: Boolean
)

data class VisualizerRipple(
    val id: Long,
    val midiNote: Int,
    val normalizedX: Float,
    val velocity: Float,
    val color: Color,
    val createdAtMs: Long
)

data class LoopNoteEvent(
    val beatPosition: Float, // 0f .. totalBeats
    val durationBeats: Float,
    val midiNote: Int,
    val velocity: Float,
    val preset: SoundPreset
)

data class LoopTrackState(
    val index: Int,
    val name: String,
    val accentColor: Color,
    val events: List<LoopNoteEvent> = emptyList(),
    val isRecording: Boolean = false,
    val isMuted: Boolean = false
)

data class PianoUiState(
    val selectedPreset: SoundPreset = SoundPreset.CONCERT_GRAND,
    val selectedScale: MusicalScale = MusicalScale.DORIAN,
    val rootNotePitchClass: Int = 0, // 0 = C .. 11 = B
    val shimmerMode: ShimmerMode = ShimmerMode.OCTAVE_BLOOM,
    val echoMix: Float = 0.28f,
    val sustainLocked: Boolean = false,
    val momentarySustain: Boolean = false,
    val isDualManual: Boolean = false,
    val lowerOctave: Int = 3,
    val upperOctave: Int = 4,
    val showKeyLabels: Boolean = true,
    val velocitySensitive: Boolean = true,
    val scaleLockSnap: Boolean = false,
    val metronomeEnabled: Boolean = false,
    val bpm: Int = 96,
    val currentBeat: Int = 0, // 0..15 across 4 bars
    val loopProgress: Float = 0f, // 0f..1f
    val isLooperVisible: Boolean = true,
    val loopTracks: List<LoopTrackState> = defaultLoopTracks(),
    val pressedMidiNotes: Set<Int> = emptySet(),
    val soundingVoices: List<ActiveVoiceVisual> = emptyList(),
    val chordAnalysis: ChordAnalysis? = null,
    val ripples: List<VisualizerRipple> = emptyList(),
    val isStudioSheetOpen: Boolean = false
) {
    val isSustainActive: Boolean get() = sustainLocked || momentarySustain
}

fun defaultLoopTracks(): List<LoopTrackState> = listOf(
    LoopTrackState(index = 0, name = "BASS", accentColor = BiolumCyan),
    LoopTrackState(index = 1, name = "CHORD", accentColor = BrushedBrass),
    LoopTrackState(index = 2, name = "LEAD", accentColor = VioletAurora)
)
