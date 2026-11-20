package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActiveVoiceVisual
import com.example.model.ChordAnalysis
import com.example.model.MusicalScale
import com.example.model.SoundPreset
import com.example.model.VisualizerRipple
import com.example.music.MusicTheory
import com.example.ui.theme.BiolumCyan
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CrispIvoryText
import com.example.ui.theme.EbonyDeck
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.VioletAurora
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HarmonicFieldCanvas(
    preset: SoundPreset,
    scale: MusicalScale,
    rootPitchClass: Int,
    soundingVoices: List<ActiveVoiceVisual>,
    ripples: List<VisualizerRipple>,
    chordAnalysis: ChordAnalysis?,
    onAuditionChord: () -> Unit,
    modifier: Modifier = Modifier
) {
    var frameTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                frameTimeMs = System.currentTimeMillis()
            }
        }
    }

    val accent = preset.accentColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianVoid,
                        EbonyDeck,
                        Color(0xFF0D111A)
                    )
                )
            )
            .border(1.dp, ConsoleBorder, RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerY = h * 0.56f
            val timeSec = (frameTimeMs % 100_000L) / 1000f

            // Subtle ambient radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (soundingVoices.isNotEmpty()) 0.14f else 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, centerY),
                    radius = w * 0.58f
                ),
                radius = w * 0.58f,
                center = Offset(w * 0.5f, centerY)
            )

            // Subtle harmonic horizon lines
            for (i in 1..3) {
                val y = h * (i * 0.25f)
                drawLine(
                    color = ConsoleBorder.copy(alpha = 0.28f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Draw Cymatic Standing Interference Waves driven by active voices
            val waveCount = 3
            for (layer in 0 until waveCount) {
                val path = Path()
                val layerColor = when (layer) {
                    0 -> accent.copy(alpha = if (soundingVoices.isEmpty()) 0.22f else 0.78f)
                    1 -> BiolumCyan.copy(alpha = if (soundingVoices.isEmpty()) 0.12f else 0.45f)
                    else -> VioletAurora.copy(alpha = if (soundingVoices.isEmpty()) 0.08f else 0.30f)
                }
                val strokeW = when (layer) {
                    0 -> 2.8f
                    1 -> 1.8f
                    else -> 1.2f
                }

                val steps = 72
                for (step in 0..steps) {
                    val normX = step.toFloat() / steps
                    val x = normX * w
                    // Window taper at edges so waves anchor cleanly
                    val edgeWindow = sin(normX * PI).toFloat()
                    var yOffset = 0f

                    if (soundingVoices.isEmpty()) {
                        // Gentle breathing idle acoustic string
                        val freq = 2.0f + layer * 1.1f
                        yOffset = sin(normX * PI * freq + timeSec * (1.2f + layer * 0.4f)).toFloat() *
                            (4.5f - layer * 1.1f) * edgeWindow
                    } else {
                        for (v in soundingVoices.take(8)) {
                            val harmonicCycles = ((v.midiNote - 28).coerceIn(4, 56)) * 0.22f + layer * 0.7f
                            val phaseSpeed = (2.5f + (v.midiNote % 12) * 0.45f) * (if (layer % 2 == 0) 1f else -0.8f)
                            val amp = v.amplitude * (h * 0.24f) / (1f + layer * 0.65f)
                            yOffset += sin(normX * PI * harmonicCycles + timeSec * phaseSpeed).toFloat() * amp
                        }
                        yOffset = (yOffset / (1f + soundingVoices.size * 0.22f)) * edgeWindow
                    }

                    val y = centerY + yOffset
                    if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = layerColor,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            // Draw Harmonic Constellation Nodes & Interval Arcs for currently active notes
            if (soundingVoices.isNotEmpty()) {
                val nodePositions = soundingVoices
                    .distinctBy { it.midiNote }
                    .sortedBy { it.midiNote }
                    .map { voice ->
                        val normX = ((voice.midiNote - 36).coerceIn(0, 48) / 48f).coerceIn(0.06f, 0.94f)
                        val x = normX * w
                        val y = centerY + sin(normX * PI * 4f + timeSec * 3f).toFloat() * (h * 0.12f)
                        Triple(voice, Offset(x, y), normX)
                    }

                // Draw glowing interval arcs between adjacent active notes
                for (i in 0 until nodePositions.size - 1) {
                    val (v1, p1, _) = nodePositions[i]
                    val (v2, p2, _) = nodePositions[i + 1]
                    val interval = (v2.midiNote - v1.midiNote) % 12
                    val arcColor = when (interval) {
                        0, 7, 5 -> BrushedBrass
                        4, 3, 9 -> BiolumCyan
                        6, 1, 11 -> CoralPulse
                        else -> VioletAurora
                    }
                    val arcPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        val midX = (p1.x + p2.x) * 0.5f
                        val arcLift = (p2.x - p1.x).coerceAtLeast(32f) * 0.35f
                        val ctrlY = minOf(p1.y, p2.y) - arcLift
                        quadraticTo(midX, ctrlY, p2.x, p2.y)
                    }
                    drawPath(
                        path = arcPath,
                        color = arcColor.copy(alpha = 0.55f),
                        style = Stroke(width = 2.0f, cap = StrokeCap.Round)
                    )
                }

                // Draw luminous resonance nodes
                for ((voice, pos, _) in nodePositions) {
                    val radius = 5f + voice.amplitude * 9f
                    drawCircle(
                        color = accent.copy(alpha = 0.25f * voice.amplitude),
                        radius = radius * 2.4f,
                        center = pos
                    )
                    drawCircle(
                        color = CrispIvoryText.copy(alpha = 0.9f * voice.amplitude),
                        radius = radius * 0.65f,
                        center = pos
                    )
                }
            }

            // Draw Expanding Ripple Blooms on key strikes
            for (ripple in ripples) {
                val ageMs = (frameTimeMs - ripple.createdAtMs).coerceAtLeast(0L)
                val progress = (ageMs / 1300f).coerceIn(0f, 1f)
                if (progress < 1f) {
                    val alpha = (1f - progress) * (1f - progress) * 0.65f
                    val center = Offset(ripple.normalizedX * w, centerY)
                    val radius = (14f + progress * (w * 0.26f)) * (0.65f + ripple.velocity * 0.45f)
                    drawCircle(
                        color = ripple.color.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5f * (1f - progress) + 0.8f)
                    )
                }
            }
        }

        // Top HUD Overlay inside the Harmonic Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = chordAnalysis,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ChordHudTransition"
                ) { analysis ->
                    if (analysis != null) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = analysis.symbol,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = CrispIvoryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accent.copy(alpha = 0.16f))
                                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${analysis.primaryFrequencyHz.toInt()} Hz",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent
                                    )
                                }
                            }
                            Text(
                                text = "${analysis.qualityName}  •  ${analysis.activeNotesText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${MusicTheory.PITCH_CLASS_NAMES[rootPitchClass]} ${scale.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CrispIvoryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = scale.moodTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent.copy(alpha = 0.9f)
                                )
                            }
                            Text(
                                text = stringResource(R.string.label_chord_idle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Audition Voicing Pill (1-tap lush demo chord in current scale & preset)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EbonyDeck.copy(alpha = 0.88f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onAuditionChord)
                    .testTag("audition_chord_button")
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Audition chord voicing",
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "VOICING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CrispIvoryText
                    )
                }
            }
        }

        // Bottom Consonance Meter Strip inside Field
        if (chordAnalysis != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HARMONIC CONSONANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlateText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(ConsoleBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(chordAnalysis.consonance)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(BiolumCyan, accent)
                                    )
                                )
                        )
                    }
                    Text(
                        text = "${(chordAnalysis.consonance * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }
    }
}
