package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LoopTrackState
import com.example.ui.PianoViewModel
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CrispIvoryText
import com.example.ui.theme.EbonyDeck
import com.example.ui.theme.ElevatedConsole
import com.example.ui.theme.MutedSlateText

@Composable
fun LooperDeck(
    tracks: List<LoopTrackState>,
    currentBeat: Int,
    loopProgress: Float,
    onToggleRecord: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onClearTrack: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anyActive = tracks.any { it.events.isNotEmpty() || it.isRecording }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EbonyDeck)
            .border(1.dp, ConsoleBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Header: 4-bar Playhead + Beat LEDs + Clear All
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "4-BAR LOOPER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MutedSlateText
                )
                // 4 Beat LEDs within current bar
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val beatInBar = currentBeat % 4
                    for (b in 0..3) {
                        val lit = anyActive && b == beatInBar
                        Box(
                            modifier = Modifier
                                .size(if (b == 0) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        lit && b == 0 -> BrushedBrass
                                        lit -> CrispIvoryText
                                        else -> ConsoleBorder
                                    }
                                )
                        )
                    }
                }
            }

            // Progress bar across 16 beats
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ElevatedConsole)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (anyActive) loopProgress else 0f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(BrushedBrass)
                )
            }

            if (anyActive) {
                Text(
                    text = "CLEAR",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CoralPulse,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClearAll)
                        .testTag("looper_clear_all")
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                Text(
                    text = "TAP REC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlateText.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3 Track Pods
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (track in tracks) {
                LoopTrackPod(
                    track = track,
                    loopProgress = loopProgress,
                    onToggleRecord = { onToggleRecord(track.index) },
                    onToggleMute = { onToggleMute(track.index) },
                    onClear = { onClearTrack(track.index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoopTrackPod(
    track: LoopTrackState,
    loopProgress: Float,
    onToggleRecord: () -> Unit,
    onToggleMute: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            track.isRecording -> CoralPulse
            track.events.isNotEmpty() && !track.isMuted -> track.accentColor.copy(alpha = 0.7f)
            else -> ConsoleBorder
        },
        label = "TrackBorderColor"
    )

    val bgColor = when {
        track.isRecording -> CoralPulse.copy(alpha = 0.14f)
        track.events.isNotEmpty() && !track.isMuted -> track.accentColor.copy(alpha = 0.10f)
        else -> ElevatedConsole
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tap track badge to toggle record
            Row(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleRecord)
                    .testTag("loop_rec_${track.index}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = "Record track ${track.name}",
                    tint = if (track.isRecording) CoralPulse else track.accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CrispIvoryText
                )
            }

            if (track.events.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (track.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = "Mute track ${track.name}",
                        tint = if (track.isMuted) MutedSlateText else track.accentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleMute)
                            .testTag("loop_mute_${track.index}")
                            .padding(2.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear track ${track.name}",
                        tint = MutedSlateText,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClear)
                            .testTag("loop_clear_${track.index}")
                            .padding(2.dp)
                    )
                }
            }
        }

        // Mini event waveform strip
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val w = size.width
            val h = size.height
            drawLine(
                color = ConsoleBorder,
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            for (ev in track.events) {
                val x = (ev.beatPosition / PianoViewModel.TOTAL_LOOP_BEATS).coerceIn(0f, 1f) * w
                drawLine(
                    color = if (track.isMuted) MutedSlateText else track.accentColor,
                    start = Offset(x, 1f),
                    end = Offset(x, h - 1f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            if (track.isRecording || track.events.isNotEmpty()) {
                val playX = loopProgress * w
                drawLine(
                    color = CrispIvoryText,
                    start = Offset(playX, 0f),
                    end = Offset(playX, h),
                    strokeWidth = 2f
                )
            }
        }
    }
}
