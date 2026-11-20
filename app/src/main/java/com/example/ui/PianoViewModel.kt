package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PolyphonicSynthEngine
import com.example.model.LoopNoteEvent
import com.example.model.MusicalScale
import com.example.model.PianoUiState
import com.example.model.ShimmerMode
import com.example.model.SoundPreset
import com.example.model.VisualizerRipple
import com.example.music.MusicTheory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PianoViewModel : ViewModel() {

    companion object {
        const val TOTAL_LOOP_BEATS = 16f // 4 bars of 4/4
    }

    private val synthEngine = PolyphonicSynthEngine()

    private val _uiState = MutableStateFlow(PianoUiState())
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    // Maps raw physical key MIDI -> actual played MIDI (supports Scale Lock Assist safely)
    private val rawToPlayedMidi = mutableMapOf<Int, Int>()
    private val activeKeyStartBeat = mutableMapOf<Int, Triple<Float, Float, SoundPreset>>()
    private val shimmerJobs = mutableListOf<Job>()

    private var rippleIdCounter = 0L
    private var currentBeatPos = 0f
    private var previousBeatPos = 0f
    private var lastTickNs = System.nanoTime()
    private val recordingAccumulatedBeats = mutableMapOf<Int, Float>()

    init {
        synthEngine.start()
        startEngineLoop()
    }

    override fun onCleared() {
        super.onCleared()
        shimmerJobs.forEach { it.cancel() }
        synthEngine.stop()
    }

    private fun startEngineLoop() {
        viewModelScope.launch {
            while (isActive) {
                val nowNs = System.nanoTime()
                val dtSec = ((nowNs - lastTickNs) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
                lastTickNs = nowNs

                val state = _uiState.value
                val anyTrackHasEvents = state.loopTracks.any { it.events.isNotEmpty() }
                val anyTrackRecording = state.loopTracks.any { it.isRecording }
                val looperRunning = anyTrackHasEvents || anyTrackRecording || state.metronomeEnabled

                if (looperRunning) {
                    val beatsPerSec = state.bpm / 60f
                    val deltaBeats = dtSec * beatsPerSec
                    previousBeatPos = currentBeatPos
                    currentBeatPos = (currentBeatPos + deltaBeats) % TOTAL_LOOP_BEATS

                    // Trigger loop playback events that fall between previousBeatPos and currentBeatPos
                    triggerLoopEventsInWindow(previousBeatPos, currentBeatPos, state)

                    // Advance recording budget so a track auto-locks after 1 full 4-bar cycle
                    val autoStopTracks = mutableSetOf<Int>()
                    for (track in state.loopTracks) {
                        if (track.isRecording) {
                            val acc = (recordingAccumulatedBeats[track.index] ?: 0f) + deltaBeats
                            recordingAccumulatedBeats[track.index] = acc
                            if (acc >= TOTAL_LOOP_BEATS) {
                                autoStopTracks.add(track.index)
                            }
                        }
                    }

                    if (autoStopTracks.isNotEmpty()) {
                        _uiState.update { cur ->
                            cur.copy(
                                loopTracks = cur.loopTracks.map { t ->
                                    if (t.index in autoStopTracks) {
                                        recordingAccumulatedBeats.remove(t.index)
                                        t.copy(isRecording = false)
                                    } else t
                                }
                            )
                        }
                    }
                } else {
                    currentBeatPos = 0f
                    previousBeatPos = 0f
                }

                // Visual refresh: active voices, chord intelligence, ripple pruning
                val visuals = synthEngine.activeVisuals
                val nowMs = System.currentTimeMillis()
                val soundingMidis = (state.pressedMidiNotes + visuals.filter { it.amplitude > 0.08f }.map { it.midiNote }).toSet()
                val chord = MusicTheory.analyzeChord(soundingMidis)

                _uiState.update { cur ->
                    val prunedRipples = cur.ripples.filter { nowMs - it.createdAtMs < 1300L }
                    cur.copy(
                        currentBeat = currentBeatPos.toInt().coerceIn(0, 15),
                        loopProgress = (currentBeatPos / TOTAL_LOOP_BEATS).coerceIn(0f, 1f),
                        soundingVoices = visuals,
                        chordAnalysis = chord,
                        ripples = prunedRipples
                    )
                }

                delay(16L)
            }
        }
    }

    private fun triggerLoopEventsInWindow(prev: Float, curr: Float, state: PianoUiState) {
        val wrapped = curr < prev
        for (track in state.loopTracks) {
            if (track.isMuted || track.events.isEmpty()) continue
            val tag = "loop_${track.index}"
            for (ev in track.events) {
                val shouldFire = if (!wrapped) {
                    ev.beatPosition >= prev && ev.beatPosition < curr
                } else {
                    ev.beatPosition >= prev || ev.beatPosition < curr
                }
                if (shouldFire) {
                    synthEngine.noteOn(
                        midiNote = ev.midiNote,
                        velocity = ev.velocity * 0.85f,
                        preset = ev.preset,
                        sourceTag = tag
                    )
                    val durMs = ((ev.durationBeats / (state.bpm / 60f)) * 1000f).toLong().coerceIn(90L, 2400L)
                    viewModelScope.launch {
                        delay(durMs)
                        synthEngine.noteOff(ev.midiNote, sourceTag = tag)
                    }
                }
            }
        }
    }

    fun onKeyPress(rawMidi: Int, velocity: Float, normalizedX: Float) {
        val state = _uiState.value
        val playedMidi = if (state.scaleLockSnap) {
            MusicTheory.snapToScale(rawMidi, state.rootNotePitchClass, state.selectedScale)
        } else {
            rawMidi
        }
        val effectiveVelocity = if (state.velocitySensitive) velocity.coerceIn(0.32f, 1.0f) else 0.85f

        rawToPlayedMidi[rawMidi] = playedMidi
        synthEngine.noteOn(
            midiNote = playedMidi,
            velocity = effectiveVelocity,
            preset = state.selectedPreset,
            sourceTag = "live"
        )

        // Track start beat if any loop track is recording
        if (state.loopTracks.any { it.isRecording }) {
            activeKeyStartBeat[playedMidi] = Triple(currentBeatPos, effectiveVelocity, state.selectedPreset)
        }

        // Spawn harmonic ripple for cymatics canvas
        val ripple = VisualizerRipple(
            id = ++rippleIdCounter,
            midiNote = playedMidi,
            normalizedX = normalizedX.coerceIn(0.04f, 0.96f),
            velocity = effectiveVelocity,
            color = state.selectedPreset.accentColor,
            createdAtMs = System.currentTimeMillis()
        )

        _uiState.update { cur ->
            val updatedNotes = cur.pressedMidiNotes + playedMidi
            cur.copy(
                pressedMidiNotes = updatedNotes,
                ripples = (cur.ripples + ripple).takeLast(20),
                chordAnalysis = MusicTheory.analyzeChord(updatedNotes)
            )
        }

        // Schedule Aether Shimmer harmonic echo notes if enabled
        triggerShimmerEcho(playedMidi, effectiveVelocity, state)
    }

    fun onKeyRelease(rawMidi: Int) {
        val playedMidi = rawToPlayedMidi.remove(rawMidi) ?: rawMidi
        // Only release if no other raw key is currently holding the same snapped midi note
        if (playedMidi !in rawToPlayedMidi.values) {
            synthEngine.noteOff(playedMidi, sourceTag = "live")

            // Commit loop event if recording
            val startInfo = activeKeyStartBeat.remove(playedMidi)
            if (startInfo != null) {
                val (startBeat, vel, preset) = startInfo
                val rawDur = currentBeatPos - startBeat
                val dur = (if (rawDur < 0f) rawDur + TOTAL_LOOP_BEATS else rawDur).coerceIn(0.18f, 8.0f)
                val newEvent = LoopNoteEvent(
                    beatPosition = startBeat,
                    durationBeats = dur,
                    midiNote = playedMidi,
                    velocity = vel,
                    preset = preset
                )
                _uiState.update { cur ->
                    cur.copy(
                        loopTracks = cur.loopTracks.map { t ->
                            if (t.isRecording) t.copy(events = t.events + newEvent) else t
                        }
                    )
                }
            }

            _uiState.update { cur ->
                val updated = cur.pressedMidiNotes - playedMidi
                cur.copy(pressedMidiNotes = updated)
            }
        }
    }

    private fun triggerShimmerEcho(baseMidi: Int, velocity: Float, state: PianoUiState) {
        val offsetsAndDelays: List<Pair<Int, Long>> = when (state.shimmerMode) {
            ShimmerMode.PURE -> emptyList()
            ShimmerMode.OCTAVE_BLOOM -> listOf(12 to 155L)
            ShimmerMode.FIFTH_CASCADE -> listOf(7 to 120L, 12 to 240L)
            ShimmerMode.HARP_SHIMMER -> listOf(12 to 95L, 19 to 190L, 24 to 285L)
        }
        if (offsetsAndDelays.isEmpty()) return

        offsetsAndDelays.forEachIndexed { idx, (semitones, delayMs) ->
            val targetMidi = baseMidi + semitones
            if (targetMidi <= 108) {
                val job = viewModelScope.launch {
                    delay(delayMs)
                    val echoVel = (velocity * (0.48f - idx * 0.09f)).coerceAtLeast(0.18f)
                    synthEngine.noteOn(
                        midiNote = targetMidi,
                        velocity = echoVel,
                        preset = SoundPreset.CELESTA_GLASS,
                        sourceTag = "shimmer"
                    )
                    delay(260L)
                    synthEngine.noteOff(targetMidi, sourceTag = "shimmer")
                }
                shimmerJobs.add(job)
                if (shimmerJobs.size > 48) {
                    shimmerJobs.removeAll { it.isCompleted }
                }
            }
        }
    }

    fun toggleSustainLock() {
        _uiState.update { cur ->
            val next = !cur.sustainLocked
            synthEngine.setSustain(next || cur.momentarySustain)
            cur.copy(sustainLocked = next)
        }
    }

    fun setMomentarySustain(held: Boolean) {
        _uiState.update { cur ->
            synthEngine.setSustain(cur.sustainLocked || held)
            cur.copy(momentarySustain = held)
        }
    }

    fun selectPreset(preset: SoundPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun selectScale(scale: MusicalScale) {
        _uiState.update { it.copy(selectedScale = scale) }
    }

    fun selectRootPitchClass(pitchClass: Int) {
        _uiState.update { it.copy(rootNotePitchClass = ((pitchClass % 12) + 12) % 12) }
    }

    fun cycleShimmerMode() {
        _uiState.update { cur ->
            val values = ShimmerMode.entries
            val next = values[(cur.shimmerMode.ordinal + 1) % values.size]
            cur.copy(shimmerMode = next)
        }
    }

    fun selectShimmerMode(mode: ShimmerMode) {
        _uiState.update { it.copy(shimmerMode = mode) }
    }

    fun setEchoMix(mix: Float) {
        val clamped = mix.coerceIn(0f, 0.65f)
        synthEngine.setEchoMix(clamped)
        _uiState.update { it.copy(echoMix = clamped) }
    }

    fun shiftOctave(delta: Int, isUpperManual: Boolean = false) {
        _uiState.update { cur ->
            if (isUpperManual) {
                cur.copy(upperOctave = (cur.upperOctave + delta).coerceIn(2, 6))
            } else {
                cur.copy(lowerOctave = (cur.lowerOctave + delta).coerceIn(1, 6))
            }
        }
    }

    fun toggleDualManual() {
        _uiState.update { cur ->
            val nextDual = !cur.isDualManual
            cur.copy(
                isDualManual = nextDual,
                lowerOctave = if (nextDual) 3 else cur.lowerOctave,
                upperOctave = if (nextDual) 4 else cur.upperOctave
            )
        }
    }

    fun toggleMetronome() {
        _uiState.update { cur ->
            val next = !cur.metronomeEnabled
            synthEngine.setMetronome(next, cur.bpm)
            cur.copy(metronomeEnabled = next)
        }
    }

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(60, 160)
        _uiState.update { cur ->
            synthEngine.setMetronome(cur.metronomeEnabled, clamped)
            cur.copy(bpm = clamped)
        }
    }

    fun toggleLooperVisible() {
        _uiState.update { it.copy(isLooperVisible = !it.isLooperVisible) }
    }

    fun toggleTrackRecord(trackIndex: Int) {
        _uiState.update { cur ->
            val updated = cur.loopTracks.map { t ->
                if (t.index == trackIndex) {
                    val willRecord = !t.isRecording
                    if (willRecord) {
                        recordingAccumulatedBeats[trackIndex] = 0f
                        t.copy(isRecording = true, isMuted = false)
                    } else {
                        recordingAccumulatedBeats.remove(trackIndex)
                        t.copy(isRecording = false)
                    }
                } else {
                    recordingAccumulatedBeats.remove(t.index)
                    t.copy(isRecording = false)
                }
            }
            cur.copy(loopTracks = updated)
        }
    }

    fun toggleTrackMute(trackIndex: Int) {
        _uiState.update { cur ->
            cur.copy(
                loopTracks = cur.loopTracks.map { t ->
                    if (t.index == trackIndex) t.copy(isMuted = !t.isMuted) else t
                }
            )
        }
    }

    fun clearTrack(trackIndex: Int) {
        recordingAccumulatedBeats.remove(trackIndex)
        _uiState.update { cur ->
            cur.copy(
                loopTracks = cur.loopTracks.map { t ->
                    if (t.index == trackIndex) {
                        t.copy(events = emptyList(), isRecording = false, isMuted = false)
                    } else t
                }
            )
        }
    }

    fun clearAllLoops() {
        recordingAccumulatedBeats.clear()
        synthEngine.panic()
        _uiState.update { cur ->
            cur.copy(
                loopTracks = cur.loopTracks.map {
                    it.copy(events = emptyList(), isRecording = false, isMuted = false)
                }
            )
        }
    }

    fun toggleShowKeyLabels() {
        _uiState.update { it.copy(showKeyLabels = !it.showKeyLabels) }
    }

    fun toggleVelocitySensitive() {
        _uiState.update { it.copy(velocitySensitive = !it.velocitySensitive) }
    }

    fun toggleScaleLockSnap() {
        _uiState.update { it.copy(scaleLockSnap = !it.scaleLockSnap) }
    }

    fun setStudioSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isStudioSheetOpen = open) }
    }

    /**
     * Plays a short signature 4-note resonant voicing so users can audition any sound engine
     * right from the Studio Sheet or empty-state welcome chip.
     */
    fun auditionWarmChord() {
        val state = _uiState.value
        val root = 48 + state.rootNotePitchClass // C3..B3
        val chordNotes = when (state.selectedScale) {
            MusicalScale.MINOR, MusicalScale.DORIAN, MusicalScale.BLUES -> listOf(root, root + 7, root + 15, root + 19, root + 22)
            MusicalScale.HIRAJOSHI -> listOf(root, root + 7, root + 14, root + 15, root + 19)
            MusicalScale.WHOLE_TONE -> listOf(root, root + 4, root + 8, root + 14)
            else -> listOf(root, root + 7, root + 14, root + 16, root + 23)
        }
        viewModelScope.launch {
            chordNotes.forEachIndexed { i, midi ->
                onKeyPress(midi, 0.72f + i * 0.04f, 0.2f + i * 0.16f)
                delay(75L)
            }
            delay(650L)
            chordNotes.forEach { midi ->
                onKeyRelease(midi)
            }
        }
    }
}
