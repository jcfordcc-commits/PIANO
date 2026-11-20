package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.PianoUiState
import com.example.model.SoundPreset
import com.example.ui.components.HarmonicFieldCanvas
import com.example.ui.components.LooperDeck
import com.example.ui.components.PianoKeyboard
import com.example.ui.components.StudioSettingsSheet
import com.example.ui.theme.BiolumCyan
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.CrispIvoryText
import com.example.ui.theme.EbonyDeck
import com.example.ui.theme.ElevatedConsole
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianVoid

@Composable
fun AetherPianoScreen(
    viewModel: PianoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianVoid,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianVoid, EbonyDeck, ObsidianVoid)
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val isLandscapeWide = maxWidth >= 620.dp && maxWidth > maxHeight

            if (isLandscapeWide) {
                LandscapeStudioLayout(
                    state = state,
                    viewModel = viewModel
                )
            } else {
                PortraitStudioLayout(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }

    if (state.isStudioSheetOpen) {
        StudioSettingsSheet(
            state = state,
            onDismiss = { viewModel.setStudioSheetOpen(false) },
            onSelectPreset = viewModel::selectPreset,
            onSelectScale = viewModel::selectScale,
            onSelectRoot = viewModel::selectRootPitchClass,
            onSelectShimmer = viewModel::selectShimmerMode,
            onEchoMixChange = viewModel::setEchoMix,
            onBpmChange = viewModel::setBpm,
            onToggleLabels = viewModel::toggleShowKeyLabels,
            onToggleVelocity = viewModel::toggleVelocitySensitive,
            onToggleScaleSnap = viewModel::toggleScaleLockSnap,
            onAuditionChord = viewModel::auditionWarmChord
        )
    }
}

@Composable
private fun PortraitStudioLayout(
    state: PianoUiState,
    viewModel: PianoViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudioTopBar(
            state = state,
            onSelectPreset = viewModel::selectPreset,
            onToggleMetronome = viewModel::toggleMetronome,
            onToggleDualManual = viewModel::toggleDualManual,
            onOpenSettings = { viewModel.setStudioSheetOpen(true) }
        )

        HarmonicFieldCanvas(
            preset = state.selectedPreset,
            scale = state.selectedScale,
            rootPitchClass = state.rootNotePitchClass,
            soundingVoices = state.soundingVoices,
            ripples = state.ripples,
            chordAnalysis = state.chordAnalysis,
            onAuditionChord = viewModel::auditionWarmChord,
            modifier = Modifier.weight(if (state.isDualManual) 0.30f else 0.42f)
        )

        AnimatedVisibility(visible = state.isLooperVisible) {
            LooperDeck(
                tracks = state.loopTracks,
                currentBeat = state.currentBeat,
                loopProgress = state.loopProgress,
                onToggleRecord = viewModel::toggleTrackRecord,
                onToggleMute = viewModel::toggleTrackMute,
                onClearTrack = viewModel::clearTrack,
                onClearAll = viewModel::clearAllLoops
            )
        }

        PerformanceControlStrip(
            state = state,
            onOctaveDown = { viewModel.shiftOctave(-1, isUpperManual = false) },
            onOctaveUp = { viewModel.shiftOctave(1, isUpperManual = false) },
            onToggleSustain = viewModel::toggleSustainLock,
            onCycleShimmer = viewModel::cycleShimmerMode,
            onToggleLooper = viewModel::toggleLooperVisible
        )

        // Piano Keybed Area (Single or Dual Manual)
        if (state.isDualManual) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.70f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DualManualKeybedHeader(
                    label = "UPPER · OCT ${state.upperOctave}",
                    onDown = { viewModel.shiftOctave(-1, isUpperManual = true) },
                    onUp = { viewModel.shiftOctave(1, isUpperManual = true) }
                )
                PianoKeyboard(
                    startOctave = state.upperOctave,
                    pressedMidiNotes = state.pressedMidiNotes,
                    accentColor = state.selectedPreset.accentColor,
                    selectedScale = state.selectedScale,
                    rootPitchClass = state.rootNotePitchClass,
                    showLabels = state.showKeyLabels,
                    tagPrefix = "upper",
                    onKeyPress = viewModel::onKeyPress,
                    onKeyRelease = viewModel::onKeyRelease,
                    modifier = Modifier.weight(1f)
                )
                PianoKeyboard(
                    startOctave = state.lowerOctave,
                    pressedMidiNotes = state.pressedMidiNotes,
                    accentColor = state.selectedPreset.accentColor,
                    selectedScale = state.selectedScale,
                    rootPitchClass = state.rootNotePitchClass,
                    showLabels = state.showKeyLabels,
                    tagPrefix = "lower",
                    onKeyPress = viewModel::onKeyPress,
                    onKeyRelease = viewModel::onKeyRelease,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            PianoKeyboard(
                startOctave = state.lowerOctave,
                pressedMidiNotes = state.pressedMidiNotes,
                accentColor = state.selectedPreset.accentColor,
                selectedScale = state.selectedScale,
                rootPitchClass = state.rootNotePitchClass,
                showLabels = state.showKeyLabels,
                tagPrefix = "main",
                onKeyPress = viewModel::onKeyPress,
                onKeyRelease = viewModel::onKeyRelease,
                modifier = Modifier.weight(0.58f)
            )
        }
    }
}

@Composable
private fun LandscapeStudioLayout(
    state: PianoUiState,
    viewModel: PianoViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudioTopBar(
            state = state,
            onSelectPreset = viewModel::selectPreset,
            onToggleMetronome = viewModel::toggleMetronome,
            onToggleDualManual = viewModel::toggleDualManual,
            onOpenSettings = { viewModel.setStudioSheetOpen(true) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HarmonicFieldCanvas(
                preset = state.selectedPreset,
                scale = state.selectedScale,
                rootPitchClass = state.rootNotePitchClass,
                soundingVoices = state.soundingVoices,
                ripples = state.ripples,
                chordAnalysis = state.chordAnalysis,
                onAuditionChord = viewModel::auditionWarmChord,
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.isLooperVisible) {
                    LooperDeck(
                        tracks = state.loopTracks,
                        currentBeat = state.currentBeat,
                        loopProgress = state.loopProgress,
                        onToggleRecord = viewModel::toggleTrackRecord,
                        onToggleMute = viewModel::toggleTrackMute,
                        onClearTrack = viewModel::clearTrack,
                        onClearAll = viewModel::clearAllLoops
                    )
                }
                PerformanceControlStrip(
                    state = state,
                    onOctaveDown = { viewModel.shiftOctave(-1, isUpperManual = false) },
                    onOctaveUp = { viewModel.shiftOctave(1, isUpperManual = false) },
                    onToggleSustain = viewModel::toggleSustainLock,
                    onCycleShimmer = viewModel::cycleShimmerMode,
                    onToggleLooper = viewModel::toggleLooperVisible
                )
            }
        }

        if (state.isDualManual) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.60f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PianoKeyboard(
                    startOctave = state.lowerOctave,
                    pressedMidiNotes = state.pressedMidiNotes,
                    accentColor = state.selectedPreset.accentColor,
                    selectedScale = state.selectedScale,
                    rootPitchClass = state.rootNotePitchClass,
                    showLabels = state.showKeyLabels,
                    tagPrefix = "lower",
                    onKeyPress = viewModel::onKeyPress,
                    onKeyRelease = viewModel::onKeyRelease,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                PianoKeyboard(
                    startOctave = state.upperOctave,
                    pressedMidiNotes = state.pressedMidiNotes,
                    accentColor = state.selectedPreset.accentColor,
                    selectedScale = state.selectedScale,
                    rootPitchClass = state.rootNotePitchClass,
                    showLabels = state.showKeyLabels,
                    tagPrefix = "upper",
                    onKeyPress = viewModel::onKeyPress,
                    onKeyRelease = viewModel::onKeyRelease,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            PianoKeyboard(
                startOctave = state.lowerOctave,
                pressedMidiNotes = state.pressedMidiNotes,
                accentColor = state.selectedPreset.accentColor,
                selectedScale = state.selectedScale,
                rootPitchClass = state.rootNotePitchClass,
                showLabels = state.showKeyLabels,
                tagPrefix = "main",
                onKeyPress = viewModel::onKeyPress,
                onKeyRelease = viewModel::onKeyRelease,
                modifier = Modifier.weight(0.60f)
            )
        }
    }
}

@Composable
private fun StudioTopBar(
    state: PianoUiState,
    onSelectPreset: (SoundPreset) -> Unit,
    onToggleMetronome: () -> Unit,
    onToggleDualManual: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand Crest
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(state.selectedPreset.accentColor.copy(alpha = 0.16f))
                    .border(1.dp, state.selectedPreset.accentColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = state.selectedPreset.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispIvoryText
                )
            }
        }

        // Quick Preset Chips (scrollable for narrow screens)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoundPreset.entries.forEach { preset ->
                val selected = preset == state.selectedPreset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) preset.accentColor.copy(alpha = 0.20f) else ElevatedConsole
                        )
                        .border(
                            1.dp,
                            if (selected) preset.accentColor else ConsoleBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .testTag("top_preset_${preset.name.lowercase()}")
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.shortName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) preset.accentColor else MutedSlateText
                    )
                }
            }
        }

        // Right Action Icons: Metronome, Dual Manual, Studio Settings
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metronome Chip
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (state.metronomeEnabled) BrushedBrass.copy(alpha = 0.18f) else ElevatedConsole)
                    .border(
                        1.dp,
                        if (state.metronomeEnabled) BrushedBrass else ConsoleBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onToggleMetronome)
                    .testTag("metronome_button")
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = stringResource(R.string.cd_metronome),
                        tint = if (state.metronomeEnabled) BrushedBrass else MutedSlateText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${state.bpm}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.metronomeEnabled) BrushedBrass else MutedSlateText
                    )
                }
            }

            // Dual Manual Keybed Toggle
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (state.isDualManual) BiolumCyan.copy(alpha = 0.18f) else ElevatedConsole)
                    .border(
                        1.dp,
                        if (state.isDualManual) BiolumCyan else ConsoleBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onToggleDualManual)
                    .testTag("dual_manual_button")
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ViewAgenda,
                    contentDescription = stringResource(R.string.cd_dual_manual),
                    tint = if (state.isDualManual) BiolumCyan else MutedSlateText,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Studio Settings Button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedConsole)
                    .border(1.dp, ConsoleBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenSettings)
                    .testTag("studio_settings_button")
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.cd_studio_settings),
                    tint = CrispIvoryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PerformanceControlStrip(
    state: PianoUiState,
    onOctaveDown: () -> Unit,
    onOctaveUp: () -> Unit,
    onToggleSustain: () -> Unit,
    onCycleShimmer: () -> Unit,
    onToggleLooper: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Octave Shifter
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedConsole)
                .border(1.dp, ConsoleBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOctaveDown)
                    .testTag("octave_down_button")
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_octave_down),
                    tint = CrispIvoryText,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "OCT ${state.lowerOctave}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CrispIvoryText,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOctaveUp)
                    .testTag("octave_up_button")
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_octave_up),
                    tint = CrispIvoryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Sustain Pedal Paddle
        val sustainOn = state.isSustainActive
        Box(
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (sustainOn) BrushedBrass.copy(alpha = 0.22f) else ElevatedConsole
                )
                .border(
                    width = if (sustainOn) 1.5.dp else 1.dp,
                    color = if (sustainOn) BrushedBrass else ConsoleBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onToggleSustain)
                .testTag("sustain_pedal_button")
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (sustainOn) BrushedBrass else ConsoleBorder)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.label_sustain),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (sustainOn) BrushedBrass else MutedSlateText
                )
            }
        }

        // Aether Shimmer Cycle Pill
        val shimmerActive = state.shimmerMode.ordinal != 0
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(if (shimmerActive) BiolumCyan.copy(alpha = 0.16f) else ElevatedConsole)
                .border(
                    1.dp,
                    if (shimmerActive) BiolumCyan.copy(alpha = 0.7f) else ConsoleBorder,
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onCycleShimmer)
                .testTag("shimmer_cycle_button")
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = stringResource(R.string.cd_shimmer_mode),
                    tint = if (shimmerActive) BiolumCyan else MutedSlateText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = state.shimmerMode.shortLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (shimmerActive) BiolumCyan else MutedSlateText
                )
            }
        }

        // Looper Visibility Toggle
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(if (state.isLooperVisible) ElevatedConsole else ObsidianVoid)
                .border(
                    1.dp,
                    if (state.isLooperVisible) BrushedBrass.copy(alpha = 0.5f) else ConsoleBorder,
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onToggleLooper)
                .testTag("looper_toggle_button")
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Loop,
                contentDescription = stringResource(R.string.cd_looper_panel),
                tint = if (state.isLooperVisible) BrushedBrass else MutedSlateText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DualManualKeybedHeader(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = null,
                tint = BiolumCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MutedSlateText
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "- OCT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = BiolumCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDown)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Text(
                text = "+ OCT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = BiolumCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onUp)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
