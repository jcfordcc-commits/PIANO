package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MusicalScale
import com.example.music.MusicTheory
import com.example.ui.theme.BiolumCyan
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.CrispIvoryText
import com.example.ui.theme.EbonyDeck
import com.example.ui.theme.EbonyKeyHighlight
import com.example.ui.theme.EbonyKeyPressed
import com.example.ui.theme.EbonyKeySurface
import com.example.ui.theme.IvoryKeyLip
import com.example.ui.theme.IvoryKeyPressed
import com.example.ui.theme.IvoryKeySurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianVoid
import kotlin.math.roundToInt

private data class WhiteKeySpec(
    val midiNote: Int,
    val index: Int
)

private data class BlackKeySpec(
    val midiNote: Int,
    val leftWhiteIndex: Int // Positioned between leftWhiteIndex and leftWhiteIndex + 1
)

/**
 * Multi-touch, glissando-capable, Y-axis velocity-sensitive expressive piano keybed.
 * Adapts white key count dynamically to available container width via BoxWithConstraints.
 */
@Composable
fun PianoKeyboard(
    startOctave: Int,
    pressedMidiNotes: Set<Int>,
    accentColor: Color,
    selectedScale: MusicalScale,
    rootPitchClass: Int,
    showLabels: Boolean,
    tagPrefix: String,
    onKeyPress: (midiNote: Int, velocity: Float, normalizedX: Float) -> Unit,
    onKeyRelease: (midiNote: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnPress by rememberUpdatedState(onKeyPress)
    val currentOnRelease by rememberUpdatedState(onKeyRelease)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianVoid)
            .border(1.dp, ConsoleBorder, RoundedCornerShape(16.dp))
            .padding(top = 5.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
            .testTag("${tagPrefix}_keyboard")
    ) {
        val density = LocalDensity.current
        val availableWidthDp = maxWidth
        // Aim for ergonomic ~42-56dp white keys across phones and tablets
        val whiteKeyCount = remember(availableWidthDp) {
            val ideal = (availableWidthDp / 46.dp).toInt()
            ideal.coerceIn(8, 18)
        }

        val startMidi = ((startOctave + 1) * 12).coerceIn(24, 96)

        val (whiteKeys, blackKeys) = remember(startMidi, whiteKeyCount) {
            buildKeySpecs(startMidi, whiteKeyCount)
        }

        val totalWidthPx = with(density) { availableWidthDp.toPx() }.coerceAtLeast(1f)
        val totalHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val whiteKeyWidthDp: Dp = availableWidthDp / whiteKeys.size.toFloat()
        val blackKeyWidthDp: Dp = whiteKeyWidthDp * 0.62f
        val blackKeyWidthPx = with(density) { blackKeyWidthDp.toPx() }
        val whiteKeyWidthPx = totalWidthPx / whiteKeys.size.toFloat()

        // Track active pointers for smooth multi-touch & glissando
        var activePointers by remember { mutableStateOf<Map<PointerId, Int>>(emptyMap()) }

        fun hitTestMidi(offset: Offset): Int? {
            val x = offset.x.coerceIn(0f, totalWidthPx - 0.1f)
            val y = offset.y.coerceIn(0f, totalHeightPx - 0.1f)

            // Black keys occupy the top 60% of the keybed
            if (y <= totalHeightPx * 0.61f) {
                for (bk in blackKeys) {
                    val center = (bk.leftWhiteIndex + 1) * whiteKeyWidthPx
                    val left = center - blackKeyWidthPx * 0.52f
                    val right = center + blackKeyWidthPx * 0.52f
                    if (x in left..right) {
                        return bk.midiNote
                    }
                }
            }

            val whiteIdx = (x / whiteKeyWidthPx).toInt().coerceIn(0, whiteKeys.lastIndex)
            return whiteKeys[whiteIdx].midiNote
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(whiteKeys, blackKeys, totalWidthPx, totalHeightPx) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val nextMap = activePointers.toMutableMap()

                            for (change in event.changes) {
                                val pointerId = change.id
                                if (!change.pressed) {
                                    val prevMidi = nextMap.remove(pointerId)
                                    if (prevMidi != null && prevMidi !in nextMap.values) {
                                        currentOnRelease(prevMidi)
                                    }
                                    change.consume()
                                } else {
                                    val hitMidi = hitTestMidi(change.position)
                                    val prevMidi = nextMap[pointerId]
                                    if (hitMidi != null && hitMidi != prevMidi) {
                                        if (prevMidi != null) {
                                            nextMap.remove(pointerId)
                                            if (prevMidi !in nextMap.values) {
                                                currentOnRelease(prevMidi)
                                            }
                                        }
                                        nextMap[pointerId] = hitMidi
                                        // Y-axis velocity: higher tap = softer (0.38), lower tap = louder (1.0)
                                        val normY = (change.position.y / totalHeightPx).coerceIn(0f, 1f)
                                        val velocity = 0.38f + normY * 0.62f
                                        val normX = (change.position.x / totalWidthPx).coerceIn(0f, 1f)
                                        currentOnPress(hitMidi, velocity, normX)
                                    }
                                    change.consume()
                                }
                            }
                            activePointers = nextMap
                        }
                    }
                }
        ) {
            // Render White Keys Layer
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (wk in whiteKeys) {
                    val isPressed = wk.midiNote in pressedMidiNotes
                    val inScale = MusicTheory.isInScale(wk.midiNote, rootPitchClass, selectedScale)
                    val isRoot = MusicTheory.midiToPitchClass(wk.midiNote) == rootPitchClass &&
                        selectedScale != MusicalScale.CHROMATIC
                    val noteName = MusicTheory.midiToNoteName(wk.midiNote)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(top = if (isPressed) 3.dp else 0.dp)
                            .clip(RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp, topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                if (isPressed) {
                                    Brush.verticalGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.45f),
                                            IvoryKeyPressed,
                                            IvoryKeyLip
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            IvoryKeySurface,
                                            Color(0xFFECE5D6),
                                            IvoryKeyLip
                                        )
                                    )
                                }
                            )
                            .border(
                                width = if (isPressed) 1.5.dp else 0.5.dp,
                                color = if (isPressed) accentColor else Color(0xFF8C8474),
                                shape = RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp, topStart = 3.dp, topEnd = 3.dp)
                            )
                            .semantics {
                                contentDescription = "Piano key $noteName"
                            }
                    ) {
                        // Top Keybed Felt / Bioluminescent LED bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(if (isPressed) 5.dp else 3.dp)
                                .background(
                                    if (isPressed) accentColor else Color(0xFF5A2525).copy(alpha = 0.65f)
                                )
                        )

                        // Bottom Label & Scale Compass Jewel
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (selectedScale != MusicalScale.CHROMATIC && inScale) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isRoot) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isPressed -> CrispIvoryText
                                                isRoot -> BrushedBrass
                                                else -> BiolumCyan.copy(alpha = 0.85f)
                                            }
                                        )
                                        .then(
                                            if (isRoot && !isPressed) {
                                                Modifier.border(1.dp, ObsidianVoid, CircleShape)
                                            } else Modifier
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (showLabels) {
                                Text(
                                    text = if (MusicTheory.midiToPitchClass(wk.midiNote) == 0) {
                                        noteName
                                    } else {
                                        MusicTheory.midiToPitchNameOnly(wk.midiNote)
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (MusicTheory.midiToPitchClass(wk.midiNote) == 0) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    fontSize = if (whiteKeyCount > 12) 9.sp else 11.sp,
                                    color = if (isPressed) ObsidianVoid else Color(0xFF4C4940)
                                )
                            }
                        }
                    }
                }
            }

            // Render Black Keys Layer
            for (bk in blackKeys) {
                val isPressed = bk.midiNote in pressedMidiNotes
                val inScale = MusicTheory.isInScale(bk.midiNote, rootPitchClass, selectedScale)
                val isRoot = MusicTheory.midiToPitchClass(bk.midiNote) == rootPitchClass &&
                    selectedScale != MusicalScale.CHROMATIC
                val centerX = (bk.leftWhiteIndex + 1) * whiteKeyWidthPx
                val offsetX = (centerX - blackKeyWidthPx * 0.5f).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, 0) }
                        .width(blackKeyWidthDp)
                        .fillMaxHeight(0.60f)
                        .padding(top = if (isPressed) 2.dp else 0.dp)
                        .shadow(
                            elevation = if (isPressed) 2.dp else 8.dp,
                            shape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp),
                            ambientColor = ObsidianVoid,
                            spotColor = ObsidianVoid
                        )
                        .clip(RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp))
                        .background(
                            if (isPressed) {
                                Brush.verticalGradient(
                                    listOf(
                                        EbonyKeyPressed,
                                        accentColor.copy(alpha = 0.55f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        EbonyKeySurface,
                                        EbonyKeyHighlight,
                                        EbonyKeySurface
                                    )
                                )
                            }
                        )
                        .border(
                            width = if (isPressed) 1.5.dp else 1.dp,
                            color = if (isPressed) accentColor else Color(0xFF39415A),
                            shape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp)
                        )
                ) {
                    // Bottom bevel reflection on ebony key
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                if (isPressed) accentColor.copy(alpha = 0.7f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                    )

                    if (selectedScale != MusicalScale.CHROMATIC && inScale) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 11.dp)
                                .size(if (isRoot) 6.dp else 4.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRoot) BrushedBrass else BiolumCyan.copy(alpha = 0.9f)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun buildKeySpecs(startMidi: Int, whiteKeyCount: Int): Pair<List<WhiteKeySpec>, List<BlackKeySpec>> {
    val whites = mutableListOf<WhiteKeySpec>()
    val blacks = mutableListOf<BlackKeySpec>()
    var midi = startMidi
    // Ensure startMidi is a white key
    while (MusicTheory.isBlackKey(midi) && midi > 21) {
        midi--
    }
    while (whites.size < whiteKeyCount && midi <= 108) {
        if (!MusicTheory.isBlackKey(midi)) {
            val whiteIdx = whites.size
            whites.add(WhiteKeySpec(midi, whiteIdx))
            // Check if next semitone is a black key and still within range
            val nextMidi = midi + 1
            if (nextMidi <= 108 && MusicTheory.isBlackKey(nextMidi) && whites.size < whiteKeyCount) {
                blacks.add(BlackKeySpec(nextMidi, whiteIdx))
            }
        }
        midi++
    }
    return whites to blacks
}
