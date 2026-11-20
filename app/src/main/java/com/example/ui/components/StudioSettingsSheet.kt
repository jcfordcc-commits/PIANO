package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MusicalScale
import com.example.model.PianoUiState
import com.example.model.ShimmerMode
import com.example.model.SoundPreset
import com.example.music.MusicTheory
import com.example.ui.theme.BiolumCyan
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.CrispIvoryText
import com.example.ui.theme.EbonyDeck
import com.example.ui.theme.ElevatedConsole
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianVoid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSettingsSheet(
    state: PianoUiState,
    onDismiss: () -> Unit,
    onSelectPreset: (SoundPreset) -> Unit,
    onSelectScale: (MusicalScale) -> Unit,
    onSelectRoot: (Int) -> Unit,
    onSelectShimmer: (ShimmerMode) -> Unit,
    onEchoMixChange: (Float) -> Unit,
    onBpmChange: (Int) -> Unit,
    onToggleLabels: () -> Unit,
    onToggleVelocity: () -> Unit,
    onToggleScaleSnap: () -> Unit,
    onAuditionChord: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EbonyDeck,
        contentColor = CrispIvoryText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Studio Crest Card using bespoke generated soundboard artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, ConsoleBorder, RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_soundboard_crest),
                    contentDescription = stringResource(R.string.cd_soundboard_art),
                    modifier = Modifier.fillMaxWidth().height(126.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ObsidianVoid.copy(alpha = 0.88f),
                                    ObsidianVoid.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_sound_studio),
                            style = MaterialTheme.typography.headlineSmall,
                            color = CrispIvoryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.selectedPreset.displayName}  •  ${state.shimmerMode.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = state.selectedPreset.accentColor
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ObsidianVoid.copy(alpha = 0.6f))
                            .testTag("studio_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.label_close),
                            tint = CrispIvoryText
                        )
                    }
                }
            }

            // Section 1: Sound Engine Selection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Piano,
                            contentDescription = null,
                            tint = BrushedBrass,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACOUSTIC & SYNTH ENGINES",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrispIvoryText
                        )
                    }
                    Text(
                        text = "AUDITION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = state.selectedPreset.accentColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(state.selectedPreset.accentColor.copy(alpha = 0.14f))
                            .clickable(onClick = onAuditionChord)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundPreset.entries.forEach { preset ->
                        val selected = preset == state.selectedPreset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) preset.accentColor.copy(alpha = 0.14f) else ElevatedConsole
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) preset.accentColor else ConsoleBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .testTag("preset_${preset.name.lowercase()}")
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) preset.accentColor else CrispIvoryText
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlateText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) preset.accentColor else ConsoleBorder)
                            )
                        }
                    }
                }
            }

            // Section 2: Scale Compass & Root Key
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = BiolumCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_scale_compass).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = CrispIvoryText
                    )
                }

                // Root Note Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MusicTheory.PITCH_CLASS_NAMES.forEachIndexed { idx, name ->
                        val selected = idx == state.rootNotePitchClass
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) BrushedBrass else ElevatedConsole)
                                .border(
                                    1.dp,
                                    if (selected) BrushedBrass else ConsoleBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectRoot(idx) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) ObsidianVoid else CrispIvoryText
                            )
                        }
                    }
                }

                // Scale Mode Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicalScale.entries.forEach { scale ->
                        val selected = scale == state.selectedScale
                        Column(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) BiolumCyan.copy(alpha = 0.16f) else ElevatedConsole)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) BiolumCyan else ConsoleBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectScale(scale) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = scale.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) BiolumCyan else CrispIvoryText
                            )
                            Text(
                                text = scale.moodTag,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlateText
                            )
                        }
                    }
                }
            }

            // Section 3: Aether Shimmer & Hall Echo Mix
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = BrushedBrass,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_aether_shimmer).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = CrispIvoryText
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerMode.entries.forEach { mode ->
                        val selected = mode == state.shimmerMode
                        Column(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) BrushedBrass.copy(alpha = 0.18f) else ElevatedConsole)
                                .border(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) BrushedBrass else ConsoleBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectShimmer(mode) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) BrushedBrass else CrispIvoryText
                            )
                            Text(
                                text = mode.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlateText
                            )
                        }
                    }
                }

                // Hall Shimmer Mix Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.label_reverb_echo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispIvoryText
                        )
                        Text(
                            text = "${((state.echoMix / 0.65f) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = BiolumCyan
                        )
                    }
                    Slider(
                        value = state.echoMix,
                        onValueChange = onEchoMixChange,
                        valueRange = 0f..0.65f,
                        colors = SliderDefaults.colors(
                            thumbColor = BiolumCyan,
                            activeTrackColor = BiolumCyan,
                            inactiveTrackColor = ConsoleBorder
                        )
                    )
                }

                // Tempo BPM Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.label_bpm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispIvoryText
                        )
                        Text(
                            text = "${state.bpm} BPM",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrushedBrass
                        )
                    }
                    Slider(
                        value = state.bpm.toFloat(),
                        onValueChange = { onBpmChange(it.toInt()) },
                        valueRange = 60f..160f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrushedBrass,
                            activeTrackColor = BrushedBrass,
                            inactiveTrackColor = ConsoleBorder
                        )
                    )
                }
            }

            // Section 4: Playability Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElevatedConsole)
                    .border(1.dp, ConsoleBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                StudioSwitchRow(
                    title = stringResource(R.string.label_key_labels),
                    subtitle = "Display note & octave badges on ivory keys",
                    checked = state.showKeyLabels,
                    onToggle = onToggleLabels,
                    tag = "switch_key_labels"
                )
                StudioSwitchRow(
                    title = stringResource(R.string.label_velocity_touch),
                    subtitle = "Lower key strikes sound louder & brighter",
                    checked = state.velocitySensitive,
                    onToggle = onToggleVelocity,
                    tag = "switch_velocity_touch"
                )
                StudioSwitchRow(
                    title = stringResource(R.string.label_scale_snap),
                    subtitle = "Gently snap out-of-scale taps to nearest harmonic tone",
                    checked = state.scaleLockSnap,
                    onToggle = onToggleScaleSnap,
                    tag = "switch_scale_snap"
                )
            }
        }
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = CrispIvoryText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlateText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = BrushedBrass,
                uncheckedThumbColor = MutedSlateText,
                uncheckedTrackColor = EbonyDeck
            )
        )
    }
}
