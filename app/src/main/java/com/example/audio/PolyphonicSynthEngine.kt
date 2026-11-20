package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.ActiveVoiceVisual
import com.example.model.SoundPreset
import com.example.music.MusicTheory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Low-latency 16-voice stereo polyphonic PCM synthesizer with per-preset harmonic spectra,
 * velocity-sensitive brightness, sustain/damper pedal, stereo shimmer delay, and
 * sample-accurate metronome click.
 */
class PolyphonicSynthEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_FRAMES = 256
        private const val MAX_VOICES = 16
        private const val TABLE_SIZE = 4096
        private const val TABLE_MASK = TABLE_SIZE - 1
        private const val DELAY_FRAMES = (SAMPLE_RATE * 0.31).toInt() // 310ms stereo shimmer delay
    }

    private sealed interface SynthCommand {
        data class NoteOn(
            val midiNote: Int,
            val velocity: Float,
            val preset: SoundPreset,
            val sourceTag: String
        ) : SynthCommand

        data class NoteOff(
            val midiNote: Int,
            val sourceTag: String
        ) : SynthCommand

        data class SetSustain(val active: Boolean) : SynthCommand
        data class SetEchoMix(val mix: Float) : SynthCommand
        data class SetMetronome(val enabled: Boolean, val bpm: Int) : SynthCommand
        data object PanicAllOff : SynthCommand
    }

    private enum class EnvStage { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

    private class Voice {
        var active = false
        var midiNote = 60
        var sourceTag = "live"
        var preset = SoundPreset.CONCERT_GRAND
        var frequencyHz = 261.63
        var velocity = 0.8f
        var panLeft = 0.7f
        var panRight = 0.7f

        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var phase4 = 0.0
        var phaseMod = 0.0
        var transientSamplesLeft = 0

        var envStage = EnvStage.IDLE
        var envLevel = 0.0f
        var keyHeld = false
        var sustainedByPedal = false
        var ageCounter = 0L

        fun reset() {
            active = false
            envStage = EnvStage.IDLE
            envLevel = 0f
            keyHeld = false
            sustainedByPedal = false
        }
    }

    private val sineTable = FloatArray(TABLE_SIZE) { i ->
        sin(2.0 * PI * i / TABLE_SIZE).toFloat()
    }

    private val commandQueue = ConcurrentLinkedQueue<SynthCommand>()
    private val activeVisualsRef = AtomicReference<List<ActiveVoiceVisual>>(emptyList())
    private val metronomeBeatRef = AtomicInteger(0)
    private val running = AtomicBoolean(false)
    private var audioThread: Thread? = null

    val activeVisuals: List<ActiveVoiceVisual>
        get() = activeVisualsRef.get()

    private fun fastSin(phaseNormalized0To1: Double): Float {
        val wrapped = phaseNormalized0To1 - kotlin.math.floor(phaseNormalized0To1)
        val idxFloat = wrapped * TABLE_SIZE
        val idx0 = idxFloat.toInt() and TABLE_MASK
        val idx1 = (idx0 + 1) and TABLE_MASK
        val frac = (idxFloat - idx0).toFloat()
        val s0 = sineTable[idx0]
        val s1 = sineTable[idx1]
        return s0 + (s1 - s0) * frac
    }

    fun start() {
        if (running.getAndSet(true)) return
        audioThread = Thread(::audioLoop, "AetherKeysAudioThread").apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        try {
            audioThread?.join(300)
        } catch (_: InterruptedException) {
        }
        audioThread = null
    }

    fun noteOn(
        midiNote: Int,
        velocity: Float = 0.85f,
        preset: SoundPreset = SoundPreset.CONCERT_GRAND,
        sourceTag: String = "live"
    ) {
        commandQueue.offer(
            SynthCommand.NoteOn(
                midiNote = midiNote.coerceIn(21, 108),
                velocity = velocity.coerceIn(0.15f, 1.0f),
                preset = preset,
                sourceTag = sourceTag
            )
        )
    }

    fun noteOff(midiNote: Int, sourceTag: String = "live") {
        commandQueue.offer(SynthCommand.NoteOff(midiNote, sourceTag))
    }

    fun setSustain(active: Boolean) {
        commandQueue.offer(SynthCommand.SetSustain(active))
    }

    fun setEchoMix(mix: Float) {
        commandQueue.offer(SynthCommand.SetEchoMix(mix.coerceIn(0f, 0.65f)))
    }

    fun setMetronome(enabled: Boolean, bpm: Int) {
        commandQueue.offer(SynthCommand.SetMetronome(enabled, bpm.coerceIn(50, 180)))
    }

    fun panic() {
        commandQueue.offer(SynthCommand.PanicAllOff)
    }

    private fun audioLoop() {
        val minBufBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufBytes <= 0) {
            // Headless / test environment without audio hardware: keep state loop silent
            silentTestLoop()
            return
        }

        val bufferBytes = maxOf(minBufBytes, BUFFER_FRAMES * 4 * 2)
        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            silentTestLoop()
            return
        }

        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            silentTestLoop()
            return
        }

        try {
            audioTrack.play()
        } catch (_: Throwable) {
            audioTrack.release()
            silentTestLoop()
            return
        }

        val pcmBuffer = ShortArray(BUFFER_FRAMES * 2)
        val delayLeft = FloatArray(DELAY_FRAMES)
        val delayRight = FloatArray(DELAY_FRAMES)
        var delayIdx = 0

        val voices = Array(MAX_VOICES) { Voice() }
        var globalAge = 0L
        var sustainPedal = false
        var echoMix = 0.28f
        var metronomeEnabled = false
        var bpm = 96
        var samplesUntilNextBeat = 0
        var beatIndex = 0
        var metroClickSamplesLeft = 0
        var metroClickFreq = 1200.0
        var metroClickPhase = 0.0
        var metroClickAmp = 0f

        var noiseSeed = 0x13579BDF

        while (running.get()) {
            // Drain incoming commands
            while (true) {
                val cmd = commandQueue.poll() ?: break
                when (cmd) {
                    is SynthCommand.NoteOn -> {
                        val existing = voices.firstOrNull {
                            it.active && it.midiNote == cmd.midiNote && it.sourceTag == cmd.sourceTag
                        }
                        val voice = existing ?: voices.firstOrNull { !it.active } ?: voices.minBy {
                            // Steal releasing or quietest/oldest voice
                            val releasePenalty = if (it.envStage == EnvStage.RELEASE) -10f else 0f
                            releasePenalty + it.envLevel + (it.ageCounter.toFloat() * 1e-9f)
                        }

                        voice.apply {
                            active = true
                            midiNote = cmd.midiNote
                            sourceTag = cmd.sourceTag
                            preset = cmd.preset
                            frequencyHz = MusicTheory.midiToFrequencyHz(cmd.midiNote)
                            velocity = cmd.velocity
                            // Stereo piano soundboard imaging: low keys left, high keys right
                            val normPan = ((cmd.midiNote - 36).coerceIn(0, 60) / 60f) * 0.6f - 0.3f
                            val angle = (normPan + 1f) * (PI / 4.0)
                            panLeft = cos(angle).toFloat()
                            panRight = sin(angle).toFloat()
                            phase1 = 0.0
                            phase2 = 0.0
                            phase3 = 0.0
                            phase4 = 0.0
                            phaseMod = 0.0
                            transientSamplesLeft = if (cmd.preset == SoundPreset.CONCERT_GRAND) {
                                (SAMPLE_RATE * 0.009).toInt()
                            } else {
                                0
                            }
                            envStage = EnvStage.ATTACK
                            envLevel = 0.001f
                            keyHeld = true
                            sustainedByPedal = false
                            ageCounter = ++globalAge
                        }
                    }

                    is SynthCommand.NoteOff -> {
                        for (v in voices) {
                            if (v.active && v.midiNote == cmd.midiNote && v.sourceTag == cmd.sourceTag) {
                                v.keyHeld = false
                                if (sustainPedal && cmd.sourceTag == "live") {
                                    v.sustainedByPedal = true
                                } else {
                                    v.sustainedByPedal = false
                                    v.envStage = EnvStage.RELEASE
                                }
                            }
                        }
                    }

                    is SynthCommand.SetSustain -> {
                        sustainPedal = cmd.active
                        if (!sustainPedal) {
                            for (v in voices) {
                                if (v.active && !v.keyHeld && v.sustainedByPedal) {
                                    v.sustainedByPedal = false
                                    v.envStage = EnvStage.RELEASE
                                }
                            }
                        }
                    }

                    is SynthCommand.SetEchoMix -> echoMix = cmd.mix
                    is SynthCommand.SetMetronome -> {
                        val wasEnabled = metronomeEnabled
                        metronomeEnabled = cmd.enabled
                        bpm = cmd.bpm
                        if (!wasEnabled && metronomeEnabled) {
                            samplesUntilNextBeat = 0
                            beatIndex = 0
                        }
                    }

                    is SynthCommand.PanicAllOff -> {
                        for (v in voices) v.reset()
                    }
                }
            }

            val invSr = 1.0 / SAMPLE_RATE
            val samplesPerBeat = (SAMPLE_RATE * 60.0 / bpm).toInt().coerceAtLeast(1000)

            for (i in 0 until BUFFER_FRAMES) {
                var mixL = 0f
                var mixR = 0f

                // Metronome trigger
                if (metronomeEnabled) {
                    if (samplesUntilNextBeat <= 0) {
                        val isDownbeat = (beatIndex % 4) == 0
                        metroClickFreq = if (isDownbeat) 1320.0 else 920.0
                        metroClickAmp = if (isDownbeat) 0.34f else 0.20f
                        metroClickSamplesLeft = (SAMPLE_RATE * 0.028).toInt()
                        metroClickPhase = 0.0
                        metronomeBeatRef.set(beatIndex % 16)
                        beatIndex = (beatIndex + 1) % 16
                        samplesUntilNextBeat = samplesPerBeat
                    }
                    samplesUntilNextBeat--
                }

                if (metroClickSamplesLeft > 0) {
                    val clickEnv = metroClickSamplesLeft.toFloat() / (SAMPLE_RATE * 0.028f)
                    val clickSample = fastSin(metroClickPhase) * clickEnv * clickEnv * metroClickAmp
                    metroClickPhase += metroClickFreq * invSr
                    mixL += clickSample
                    mixR += clickSample
                    metroClickSamplesLeft--
                }

                // Synthesize all active voices
                for (v in voices) {
                    if (!v.active) continue

                    // Pitch-dependent decay (real piano strings decay faster on high notes)
                    val pitchFactor = ((v.midiNote - 36).coerceIn(0, 60)) / 60f
                    val env = v.envLevel

                    when (v.envStage) {
                        EnvStage.ATTACK -> {
                            val attackStep = when (v.preset) {
                                SoundPreset.CONCERT_GRAND -> 0.018f
                                SoundPreset.RHODES_VELVET -> 0.012f
                                SoundPreset.CELESTA_GLASS -> 0.005f
                                SoundPreset.SOLAR_SYNTH -> 0.0035f
                            }
                            v.envLevel += attackStep
                            if (v.envLevel >= 1.0f) {
                                v.envLevel = 1.0f
                                v.envStage = EnvStage.DECAY
                            }
                        }

                        EnvStage.DECAY -> {
                            val decayCoeff = when (v.preset) {
                                SoundPreset.CONCERT_GRAND -> 0.99991f - (pitchFactor * 0.00011f)
                                SoundPreset.RHODES_VELVET -> 0.99993f - (pitchFactor * 0.00006f)
                                SoundPreset.CELESTA_GLASS -> 0.99989f - (pitchFactor * 0.00009f)
                                SoundPreset.SOLAR_SYNTH -> 0.99996f
                            }
                            val sustainFloor = when (v.preset) {
                                SoundPreset.CONCERT_GRAND -> 0.14f
                                SoundPreset.RHODES_VELVET -> 0.25f
                                SoundPreset.CELESTA_GLASS -> 0.10f
                                SoundPreset.SOLAR_SYNTH -> 0.58f
                            }
                            v.envLevel *= decayCoeff
                            if (v.envLevel <= sustainFloor && v.keyHeld) {
                                v.envStage = EnvStage.SUSTAIN
                            } else if (v.envLevel < 0.0015f) {
                                v.reset()
                                continue
                            }
                        }

                        EnvStage.SUSTAIN -> {
                            val sustainDecay = when (v.preset) {
                                SoundPreset.SOLAR_SYNTH -> 0.999985f
                                else -> 0.99995f
                            }
                            v.envLevel *= sustainDecay
                            if (v.envLevel < 0.0015f) {
                                v.reset()
                                continue
                            }
                        }

                        EnvStage.RELEASE -> {
                            val releaseCoeff = when (v.preset) {
                                SoundPreset.CONCERT_GRAND -> 0.99945f
                                SoundPreset.RHODES_VELVET -> 0.99955f
                                SoundPreset.CELESTA_GLASS -> 0.99965f
                                SoundPreset.SOLAR_SYNTH -> 0.99968f
                            }
                            v.envLevel *= releaseCoeff
                            if (v.envLevel < 0.0012f) {
                                v.reset()
                                continue
                            }
                        }

                        EnvStage.IDLE -> {
                            v.reset()
                            continue
                        }
                    }

                    val freq = v.frequencyHz
                    val bright = v.velocity * (0.4f + 0.6f * env)

                    val sample = when (v.preset) {
                        SoundPreset.CONCERT_GRAND -> {
                            val f1 = fastSin(v.phase1)
                            val f2 = fastSin(v.phase2) * (0.42f * bright)
                            val f3 = fastSin(v.phase3) * (0.20f * bright * env)
                            val f4 = fastSin(v.phase4) * (0.09f * bright * env)
                            v.phase1 += freq * invSr
                            v.phase2 += (freq * 2.0012) * invSr // Subtle real-string inharmonicity
                            v.phase3 += (freq * 3.0038) * invSr
                            v.phase4 += (freq * 4.008) * invSr

                            var transient = 0f
                            if (v.transientSamplesLeft > 0) {
                                noiseSeed = noiseSeed * 1664525 + 1013904223
                                val white = ((noiseSeed ushr 16) and 0xFFFF) / 32768f - 1f
                                val tEnv = v.transientSamplesLeft / (SAMPLE_RATE * 0.009f)
                                transient = white * tEnv * 0.14f * v.velocity
                                v.transientSamplesLeft--
                            }
                            (f1 * 0.68f + f2 + f3 + f4 + transient) * env * v.velocity * 0.42f
                        }

                        SoundPreset.RHODES_VELVET -> {
                            val f1 = fastSin(v.phase1)
                            val tine = fastSin(v.phase2) * (0.28f * exp((-1.0 + env) * 4.0).toFloat().coerceIn(0.05f, 1f))
                            val warm2 = fastSin(v.phase3) * 0.18f
                            val tremolo = 0.85f + 0.15f * fastSin(v.phaseMod)
                            v.phase1 += freq * invSr
                            v.phase2 += (freq * 7.0) * invSr
                            v.phase3 += (freq * 2.0) * invSr
                            v.phaseMod += 4.2 * invSr
                            (f1 * 0.78f + warm2 + tine) * env * tremolo * v.velocity * 0.40f
                        }

                        SoundPreset.CELESTA_GLASS -> {
                            val f1 = fastSin(v.phase1)
                            val f2 = fastSin(v.phase2) * 0.34f
                            val bell = fastSin(v.phase3) * (0.24f * env)
                            val shimmer = fastSin(v.phase4) * (0.14f * env)
                            v.phase1 += freq * invSr
                            v.phase2 += (freq * 2.0) * invSr
                            v.phase3 += (freq * 3.99) * invSr
                            v.phase4 += (freq * 5.41) * invSr
                            (f1 * 0.66f + f2 + bell + shimmer) * env * v.velocity * 0.36f
                        }

                        SoundPreset.SOLAR_SYNTH -> {
                            val detune = 1.0038
                            val o1 = fastSin(v.phase1) + fastSin(v.phase1 * 2.0) * 0.25f + fastSin(v.phase1 * 3.0) * 0.12f
                            val o2 = fastSin(v.phase2) + fastSin(v.phase2 * 2.0) * 0.25f + fastSin(v.phase2 * 3.0) * 0.12f
                            val sub = fastSin(v.phase3) * 0.22f
                            v.phase1 += freq * invSr
                            v.phase2 += (freq * detune) * invSr
                            v.phase3 += (freq * 0.5) * invSr
                            ((o1 + o2) * 0.38f + sub) * env * v.velocity * 0.36f
                        }
                    }

                    mixL += sample * v.panLeft
                    mixR += sample * v.panRight
                }

                // Stereo Shimmer Cross-Delay Bus
                if (echoMix > 0.01f) {
                    val wetL = delayLeft[delayIdx]
                    val wetR = delayRight[delayIdx]
                    delayLeft[delayIdx] = mixL * 0.6f + wetR * 0.42f
                    delayRight[delayIdx] = mixR * 0.6f + wetL * 0.42f
                    delayIdx = (delayIdx + 1) % DELAY_FRAMES

                    mixL += wetL * echoMix
                    mixR += wetR * echoMix
                } else {
                    delayLeft[delayIdx] = 0f
                    delayRight[delayIdx] = 0f
                    delayIdx = (delayIdx + 1) % DELAY_FRAMES
                }

                // Soft warm limiter to prevent digital clipping on huge chords
                val outL = tanh(mixL.toDouble()).toFloat()
                val outR = tanh(mixR.toDouble()).toFloat()

                pcmBuffer[i * 2] = (outL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                pcmBuffer[i * 2 + 1] = (outR * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }

            // Publish lightweight visual snapshot for 60fps UI cymatics
            val visuals = ArrayList<ActiveVoiceVisual>(6)
            for (v in voices) {
                if (v.active && v.envLevel > 0.015f) {
                    visuals.add(
                        ActiveVoiceVisual(
                            midiNote = v.midiNote,
                            amplitude = (v.envLevel * v.velocity).coerceIn(0f, 1f),
                            frequencyHz = v.frequencyHz,
                            isSustainedOnly = !v.keyHeld
                        )
                    )
                }
            }
            activeVisualsRef.set(visuals)

            audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
        }

        try {
            audioTrack.pause()
            audioTrack.flush()
            audioTrack.release()
        } catch (_: Throwable) {
        }
    }

    private fun silentTestLoop() {
        while (running.get()) {
            while (commandQueue.poll() != null) { /* drain */ }
            try {
                Thread.sleep(20)
            } catch (_: InterruptedException) {
                break
            }
        }
    }
}
