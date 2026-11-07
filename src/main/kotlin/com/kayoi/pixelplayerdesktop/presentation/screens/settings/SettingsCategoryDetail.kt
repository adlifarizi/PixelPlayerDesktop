package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import com.kayoi.pixelplayerdesktop.generated.resources.ic_align_justify_space_even
import com.kayoi.pixelplayerdesktop.generated.resources.ic_lyrics
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_down
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_volume_up
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_clear_all
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_content_copy
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_delete
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_delete_forever
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_folder
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_person
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_sync
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_attach_file
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_auto_awesome
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_chevron_right
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_dark_mode
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_layers
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_light_mode
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_palette
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speaker
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speed
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_tab
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_view_week
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress
import com.kayoi.pixelplayerdesktop.presentation.components.AudioDeviceSelectorDialog
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveSwitchRow
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsCategoryDetail(
    section: SettingsSection,
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    isDynamicColor: Boolean,
    onToggleDarkTheme: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenFoldersDetail: () -> Unit,
    onOpenArtistsDetail: () -> Unit,
    onOpenDuplicatesDetail: () -> Unit,
    onOpenPaletteStyleDetail: () -> Unit,
    onOpenLicensesDetail: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = section.title,
                horizontalPadding = horizontalPadding
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (section) {
            SettingsSection.LIBRARY -> {
                item {
                    val musicDirectories by viewModel.musicDirectories.collectAsState()
                    val excludedDirectories by viewModel.excludedDirectories.collectAsState()
                    val artistDelimiters by viewModel.artistDelimiters.collectAsState()
                    val wordDelimiters by viewModel.wordDelimiters.collectAsState()

                    val folderSubtitle = when {
                        musicDirectories.isEmpty() -> "No folders configured"
                        excludedDirectories.isEmpty() -> "${musicDirectories.size} configured ${if (musicDirectories.size == 1) "directory" else "directories"}"
                        else -> "${musicDirectories.size} configured • ${excludedDirectories.size} excluded"
                    }

                    val artistSubtitle = "${artistDelimiters.joinToString(" ")} • ${wordDelimiters.size} word delimiters"

                    SettingsSubsection(title = "LIBRARY STRUCTURE") {
                        ExpressiveSettingsRow(
                            title = "Music Folders",
                            subtitle = folderSubtitle,
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_folder), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 2),
                            onClick = onOpenFoldersDetail
                        )

                        ExpressiveSettingsRow(
                            title = "Artists",
                            subtitle = artistSubtitle,
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_person), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 2),
                            onClick = onOpenArtistsDetail
                        )
                    }
                }

                item {
                    val minSongDuration by viewModel.minSongDurationMs.collectAsState()
                    val minTracksPerAlbum by viewModel.minTracksPerAlbum.collectAsState()
                    val albumArtCacheLimitMb by viewModel.albumArtCacheLimitMb.collectAsState()

                    var minSongDurationDraft by remember(minSongDuration) {
                        mutableStateOf(minSongDuration.toFloat())
                    }
                    var minTracksPerAlbumDraft by remember(minTracksPerAlbum) {
                        mutableStateOf(minTracksPerAlbum.toFloat())
                    }
                    var albumArtCacheLimitDraft by remember(albumArtCacheLimitMb) {
                        mutableStateOf(albumArtCacheLimitMb.toFloat())
                    }

                    SettingsSubsection(title = "FILTERING") {
                        ExpressiveSliderSettingsRow(
                            title = "Minimum Song Duration",
                            value = minSongDurationDraft,
                            valueRange = 0f..120000f,
                            steps = 23,
                            onValueChange = { minSongDurationDraft = it },
                            onValueChangeFinished = {
                                val selected = minSongDurationDraft.roundToInt()
                                if (selected != minSongDuration) {
                                    viewModel.setMinSongDuration(selected)
                                }
                            },
                            valueText = { value -> "${(value / 1000).toInt()}s" },
                            shape = getGroupShape(0, 3)
                        )

                        ExpressiveSliderSettingsRow(
                            title = "Minimum Tracks per Album",
                            value = minTracksPerAlbumDraft,
                            valueRange = 1f..5f,
                            steps = 3,
                            onValueChange = { minTracksPerAlbumDraft = it },
                            onValueChangeFinished = {
                                val selected = minTracksPerAlbumDraft.roundToInt()
                                if (selected != minTracksPerAlbum) {
                                    viewModel.setMinTracksPerAlbum(selected)
                                }
                            },
                            valueText = { value -> "${value.roundToInt()}" },
                            shape = getGroupShape(1, 3)
                        )

                        ExpressiveSliderSettingsRow(
                            title = "Album Art Cache Limit",
                            value = albumArtCacheLimitDraft,
                            valueRange = 50f..1500f,
                            steps = 28,
                            onValueChange = { albumArtCacheLimitDraft = it },
                            onValueChangeFinished = {
                                val selected = albumArtCacheLimitDraft.roundToInt()
                                if (selected != albumArtCacheLimitMb) {
                                    viewModel.setAlbumArtCacheLimitMb(selected)
                                }
                            },
                            valueText = { value -> "${value.roundToInt()} MB" },
                            shape = getGroupShape(2, 3)
                        )
                    }
                }

                item {
                    val scanProgress by viewModel.scanProgress.collectAsState()
                    val isScanning = scanProgress is ScanProgress.Scanning
                    val autoScanLrcFiles by viewModel.autoScanLrcFiles.collectAsState()
                    var showRebuildDialog by remember { mutableStateOf(false) }

                    if (showRebuildDialog) {
                        TrackDialogInput()
                        AlertDialog(
                            onDismissRequest = { showRebuildDialog = false },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_outlined_delete_forever),
                                    contentDescription = null,
                                    tint = colors.error
                                )
                            },
                            title = {
                                Text(
                                    text = "Rebuild Database?",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "This will delete all songs and artists from your local database and perform a clean rescan. Playlists and favorites will be preserved. Are you sure?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showRebuildDialog = false
                                        viewModel.rebuildDatabase()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.error,
                                        contentColor = colors.onError
                                    )
                                ) {
                                    Text("Rebuild")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRebuildDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    SettingsSubsection(title = "SYNC & SCANNING") {
                        Surface(
                            shape = getGroupShape(0, 3),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painterResource(Res.drawable.ic_outlined_sync), null, tint = colors.secondary, modifier = Modifier.size(22.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Refresh Library",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Scan entire library for new and modified files.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val isStacked = maxWidth < 400.dp

                                    if (isStacked) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilledTonalButton(
                                                onClick = onRescanLibrary,
                                                enabled = !isScanning,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth().height(42.dp)
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_rounded_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Full Rescan", textAlign = TextAlign.Center)
                                            }

                                            OutlinedButton(
                                                onClick = { showRebuildDialog = true },
                                                enabled = !isScanning,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = colors.error
                                                ),
                                                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth().height(42.dp)
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_outlined_delete_forever), contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Rebuild Database", textAlign = TextAlign.Center)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            FilledTonalButton(
                                                onClick = onRescanLibrary,
                                                enabled = !isScanning,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f).height(42.dp)
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_rounded_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Full Rescan", textAlign = TextAlign.Center)
                                            }

                                            OutlinedButton(
                                                onClick = { showRebuildDialog = true },
                                                enabled = !isScanning,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = colors.error
                                                ),
                                                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f).height(42.dp)
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_outlined_delete_forever), contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Rebuild Database", textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }

                                if (isScanning) {
                                    val progress = scanProgress as ScanProgress.Scanning
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (progress.totalDiscovered > 0) {
                                        LinearProgressIndicator(
                                            progress = { progress.scannedCount.toFloat() / progress.totalDiscovered.coerceAtLeast(1) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                    Text(
                                        text = "Scanning ${progress.scannedCount} of ${progress.totalDiscovered} songs…",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        ExpressiveSwitchRow(
                            title = "Auto-scan lyric files",
                            subtitle = "Automatically scan and assign .lrc and .ttml files in the same folder during library sync.",
                            checked = autoScanLrcFiles,
                            onCheckedChange = { viewModel.setAutoScanLrcFiles(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_folder), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 3)
                        )

                        ExpressiveSettingsRow(
                            title = "Find duplicate songs",
                            subtitle = "Scan your library for songs that appear more than once.",
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_content_copy), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(2, 3),
                            onClick = onOpenDuplicatesDetail
                        )
                    }
                }

                item {
                    val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
                    val fetchOnlineLyrics by viewModel.fetchOnlineLyrics.collectAsState()

                    SettingsSubsection(title = "ONLINE SERVICES") {
                        ExpressiveSwitchRow(
                            title = "Fetch Artist Portraits from Deezer",
                            subtitle = "Automatically download and cache missing artist portraits",
                            checked = fetchDeezerImages,
                            onCheckedChange = { viewModel.setFetchDeezerArtistImages(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_person), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 2)
                        )

                        ExpressiveSwitchRow(
                            title = "Fetch Online Lyrics (LRCLIB)",
                            subtitle = "Automatically fetch and sync lyrics from LRCLIB online database",
                            checked = fetchOnlineLyrics,
                            onCheckedChange = { viewModel.setFetchOnlineLyrics(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_sync), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 2)
                        )
                    }
                }

                item {
                    val currentLyricsPriority by viewModel.lyricsSourcePreference.collectAsState()
                    var showResetLyricsDialog by remember { mutableStateOf(false) }

                    if (showResetLyricsDialog) {
                        TrackDialogInput()
                        AlertDialog(
                            onDismissRequest = { showResetLyricsDialog = false },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_outlined_clear_all),
                                    contentDescription = null,
                                    tint = colors.error
                                )
                            },
                            title = {
                                Text(
                                    text = "Reset imported lyrics?",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "This will remove all cached lyrics from the local database. Songs will re-fetch lyrics according to your source priority.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showResetLyricsDialog = false
                                        viewModel.resetAllLyrics()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.error,
                                        contentColor = colors.onError
                                    )
                                ) {
                                    Text("Reset")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showResetLyricsDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    SettingsSubsection(title = "LYRICS MANAGEMENT") {
                        Surface(
                            shape = getGroupShape(0, 2),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_lyrics),
                                        contentDescription = null,
                                        tint = colors.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Lyrics Source Priority",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Choose which lyrics source to prioritize when searching",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LyricsSourcePreference.entries.forEach { preference ->
                                        val isSelected = preference == currentLyricsPriority
                                        val animatedContainerColor by animateColorAsState(
                                            targetValue = if (isSelected) colors.primary else colors.surfaceContainerHighest,
                                            label = "lyricsPriorityContainer"
                                        )
                                        val animatedContentColor by animateColorAsState(
                                            targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                            label = "lyricsPriorityContent"
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = animatedContainerColor,
                                            modifier = Modifier
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { viewModel.setLyricsSourcePreference(preference) }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = preference.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = animatedContentColor,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ExpressiveSettingsRow(
                            title = "Reset Imported Lyrics",
                            subtitle = "Remove all imported lyrics from the database",
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_clear_all), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 2),
                            onClick = { showResetLyricsDialog = true }
                        )
                    }
                }
            }

            SettingsSection.APPEARANCE -> {
                item {
                    SettingsSubsection(title = "GLOBAL THEME") {
                        ExpressiveSwitchRow(
                            title = "Dark Theme",
                            subtitle = "Switch between Material You Dark and Light theme",
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() },
                            leadingIcon = {
                                Icon(painterResource(if (isDarkTheme) Res.drawable.ic_rounded_dark_mode else Res.drawable.ic_rounded_light_mode), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 1)
                        )
                    }
                }

                item {
                    val uiScale by viewModel.uiScale.collectAsState()
                    var uiScaleDraft by remember(uiScale) { mutableStateOf(uiScale) }

                    SettingsSubsection(title = "INTERFACE SCALE") {
                        ExpressiveSliderSettingsRow(
                            title = "Display Scale",
                            subtitle = "Adjust UI zoom level (Ctrl/Cmd + Plus / Minus to zoom, Ctrl/Cmd + 0 to reset)",
                            value = uiScaleDraft,
                            valueRange = 0.70f..2.00f,
                            steps = 12,
                            onValueChange = { uiScaleDraft = ((it * 10).roundToInt() / 10f) },
                            onValueChangeFinished = {
                                if (uiScaleDraft != uiScale) {
                                    viewModel.setUiScale(uiScaleDraft)
                                }
                            },
                            shape = getGroupShape(0, 1),
                            valueText = { "${(it * 100).roundToInt()}%" }
                        )
                    }
                }

                item {
                    val currentPaletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
                    val showPlayerFileInfo by viewModel.showPlayerFileInfo.collectAsState()

                    SettingsSubsection(title = "NOW PLAYING") {
                        ExpressiveSwitchRow(
                            title = "Dynamic Color Theme",
                            subtitle = "Tint interface colors dynamically based on active album artwork",
                            checked = isDynamicColor,
                            onCheckedChange = { onToggleDynamicColor() },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_auto_awesome), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = if (isDynamicColor) getGroupShape(0, 3) else getGroupShape(0, 2)
                        )

                        AnimatedVisibility(
                            visible = isDynamicColor,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            ExpressiveSettingsRow(
                                title = "Album Art Palette Style",
                                subtitle = "Current: ${currentPaletteStyle.label}. Open live preview and choose style.",
                                leadingIcon = {
                                    Icon(painterResource(Res.drawable.ic_rounded_palette), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                                },
                                trailingIcon = {
                                    Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                                },
                                shape = getGroupShape(1, 3),
                                onClick = onOpenPaletteStyleDetail
                            )
                        }

                        ExpressiveSwitchRow(
                            title = "Show Player File Info",
                            subtitle = "Show codec, bitrate, and sample rate in the player progress section",
                            checked = showPlayerFileInfo,
                            onCheckedChange = { viewModel.setShowPlayerFileInfo(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_attach_file), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = if (isDynamicColor) getGroupShape(2, 3) else getGroupShape(1, 2)
                        )
                    }
                }

                item {
                    val navBarCompactMode by viewModel.navBarCompactMode.collectAsState()
                    val defaultTabKey by viewModel.defaultTab.collectAsState()
                    var showDefaultTabDialog by remember { mutableStateOf(false) }

                    val availableTabs = remember {
                        DesktopNavDestination.entries.filter { it != DesktopNavDestination.SETTINGS }
                    }
                    val currentSelectedTab = remember(defaultTabKey) {
                        availableTabs.firstOrNull { it.name == defaultTabKey } ?: DesktopNavDestination.HOME
                    }

                    if (showDefaultTabDialog) {
                        TrackDialogInput()
                        AlertDialog(
                            onDismissRequest = { showDefaultTabDialog = false },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_tab),
                                    contentDescription = null,
                                    tint = colors.primary
                                )
                            },
                            title = {
                                Text(
                                    text = "Default Tab",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableTabs.forEach { destination ->
                                        val isSelected = destination == currentSelectedTab
                                        Surface(
                                            onClick = {
                                                viewModel.setDefaultTab(destination.name)
                                                showDefaultTabDialog = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) colors.primaryContainer else colors.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(destination.icon),
                                                    contentDescription = null,
                                                    tint = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = destination.title,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDefaultTabDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    SettingsSubsection(title = "NAVIGATION BAR") {
                        ExpressiveSwitchRow(
                            title = "Compact Mode",
                            subtitle = "Show only icons and reduce navigation bar width",
                            checked = navBarCompactMode,
                            onCheckedChange = { viewModel.setNavBarCompactMode(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_view_week), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 2)
                        )

                        ExpressiveSettingsRow(
                            title = "Default Tab",
                            subtitle = "Choose the default launch destination (Current: ${currentSelectedTab.title})",
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_tab), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 2),
                            onClick = { showDefaultTabDialog = true }
                        )
                    }
                }
            }

            SettingsSection.PLAYBACK -> {
                item {
                    val selectedAudioDevice by viewModel.selectedAudioDevice.collectAsState()
                    val availableAudioDevices by viewModel.availableAudioDevices.collectAsState()
                    var showAudioDeviceDialog by remember { mutableStateOf(false) }

                    SettingsSubsection(title = "AUDIO OUTPUT") {
                        val currentDesc = availableAudioDevices.firstOrNull { it.name == selectedAudioDevice }?.description
                            ?: if (selectedAudioDevice == "auto") "Autoselect device (System Default)" else selectedAudioDevice

                        ExpressiveSettingsRow(
                            title = "Audio Output Device",
                            subtitle = currentDesc,
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_speaker), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_chevron_right), null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 1),
                            onClick = { showAudioDeviceDialog = true }
                        )
                    }

                    if (showAudioDeviceDialog) {
                        AudioDeviceSelectorDialog(
                            selectedDevice = selectedAudioDevice,
                            availableDevices = availableAudioDevices,
                            onSelectDevice = { viewModel.setSelectedAudioDevice(it) },
                            onRefreshDevices = { viewModel.refreshAudioDevices() },
                            onDismiss = { showAudioDeviceDialog = false }
                        )
                    }
                }

                item {
                    val replayGainEnabled by viewModel.replayGainEnabled.collectAsState()
                    val replayGainMode by viewModel.replayGainMode.collectAsState()

                    SettingsSubsection(title = "VOLUME NORMALIZATION (REPLAYGAIN)") {
                        ExpressiveSwitchRow(
                            title = "Enable ReplayGain",
                            subtitle = "Normalize volume levels using ReplayGain metadata from audio files.",
                            checked = replayGainEnabled,
                            onCheckedChange = { viewModel.setReplayGainEnabled(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_automirrored_rounded_volume_down), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = if (replayGainEnabled) getGroupShape(0, 2) else getGroupShape(0, 1)
                        )

                        AnimatedVisibility(
                            visible = replayGainEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = getGroupShape(1, 2),
                                color = colors.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_automirrored_rounded_volume_down),
                                            contentDescription = null,
                                            tint = colors.secondary,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Gain Mode",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "Track: normalize each song. Album: normalize per album.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("track" to "Track", "album" to "Album").forEach { (modeKey, modeLabel) ->
                                            val isSelected = replayGainMode == modeKey
                                            val animatedContainerColor by animateColorAsState(
                                                targetValue = if (isSelected) colors.primary else colors.surfaceContainerHighest,
                                                label = "gainModeContainer"
                                            )
                                            val animatedContentColor by animateColorAsState(
                                                targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                                label = "gainModeContent"
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = animatedContainerColor,
                                                modifier = Modifier
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { viewModel.setReplayGainMode(modeKey) }
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = modeLabel,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = animatedContentColor,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsState()
                    val crossfadeDuration by viewModel.crossfadeDurationSeconds.collectAsState()
                    val audioFadeOnPlayPause by viewModel.audioFadeOnPlayPause.collectAsState()
                    val showQueueHistory by viewModel.showQueueHistory.collectAsState()

                    var crossfadeDurationDraft by remember(crossfadeDuration) {
                        mutableStateOf(crossfadeDuration.toFloat())
                    }

                    SettingsSubsection(title = "QUEUE AND TRANSITIONS") {
                        val crossfadeShape = when {
                            crossfadeEnabled -> getGroupShape(0, 4)
                            else -> getGroupShape(0, 3)
                        }

                        ExpressiveSwitchRow(
                            title = "Crossfade",
                            subtitle = "Enable smooth transition between songs.",
                            checked = crossfadeEnabled,
                            onCheckedChange = { viewModel.setCrossfadeEnabled(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_align_justify_space_even), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = crossfadeShape
                        )

                        AnimatedVisibility(
                            visible = crossfadeEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            ExpressiveSliderSettingsRow(
                                title = "Crossfade Duration",
                                value = crossfadeDurationDraft,
                                valueRange = 1f..12f,
                                steps = 10,
                                onValueChange = { crossfadeDurationDraft = it },
                                onValueChangeFinished = {
                                    val selected = crossfadeDurationDraft.roundToInt()
                                    if (selected != crossfadeDuration) {
                                        viewModel.setCrossfadeDurationSeconds(selected)
                                    }
                                },
                                valueText = { value -> "${value.roundToInt()}s" },
                                shape = getGroupShape(1, 4)
                            )
                        }

                        val audioFadeShape = when {
                            crossfadeEnabled -> getGroupShape(2, 4)
                            else -> getGroupShape(1, 3)
                        }

                        ExpressiveSwitchRow(
                            title = "Audio Fade on Play/Pause",
                            subtitle = "Smoothly fade audio in and out when playing or pausing",
                            checked = audioFadeOnPlayPause,
                            onCheckedChange = { viewModel.setAudioFadeOnPlayPause(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_automirrored_rounded_volume_up), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = audioFadeShape
                        )

                        val queueHistoryShape = when {
                            crossfadeEnabled -> getGroupShape(3, 4)
                            else -> getGroupShape(2, 3)
                        }

                        ExpressiveSwitchRow(
                            title = "Show queue history",
                            subtitle = "Show previously played songs in the queue.",
                            checked = showQueueHistory,
                            onCheckedChange = { viewModel.setShowQueueHistory(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_automirrored_rounded_queue_music), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = queueHistoryShape
                        )
                    }
                }

                item {
                    val savedSpeedsCount by viewModel.savedSongSpeedsCount.collectAsState()
                    var showClearConfirmDialog by remember { mutableStateOf(false) }

                    SettingsSubsection(title = "PLAYBACK SPEED") {
                        val windowInfo = LocalWindowInfo.current
                        val density = LocalDensity.current
                        val isSmallScreen = with(density) { windowInfo.containerSize.width.toDp() } < 800.dp

                        Surface(
                            shape = getGroupShape(0, 1),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                val isCompact = isSmallScreen || maxWidth < 640.dp

                                if (isCompact) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier.size(24.dp).padding(top = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_speed),
                                                    contentDescription = null,
                                                    tint = colors.secondary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Saved Song Speeds",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (savedSpeedsCount > 0) {
                                                        "$savedSpeedsCount song${if (savedSpeedsCount > 1) "s" else ""} with custom playback speed"
                                                    } else {
                                                        "No individual songs saved with custom speed"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (savedSpeedsCount > 0) {
                                            FilledTonalButton(
                                                onClick = { showClearConfirmDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = colors.errorContainer,
                                                    contentColor = colors.onErrorContainer
                                                )
                                            ) {
                                                Text("Clear All")
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_speed),
                                                    contentDescription = null,
                                                    tint = colors.secondary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "Saved Song Speeds",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface
                                                )
                                                Text(
                                                    text = if (savedSpeedsCount > 0) {
                                                        "$savedSpeedsCount song${if (savedSpeedsCount > 1) "s" else ""} with custom playback speed"
                                                    } else {
                                                        "No individual songs saved with custom speed"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (savedSpeedsCount > 0) {
                                            FilledTonalButton(
                                                onClick = { showClearConfirmDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = colors.errorContainer,
                                                    contentColor = colors.onErrorContainer
                                                )
                                            ) {
                                                Text("Clear All")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showClearConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearConfirmDialog = false },
                                title = { Text("Clear All Saved Speeds?") },
                                text = { Text("This will remove custom playback speeds for all $savedSpeedsCount saved songs and revert them to default.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.clearAllSavedSongSpeeds()
                                            showClearConfirmDialog = false
                                        }
                                    ) {
                                        Text("Clear", color = colors.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearConfirmDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    val closeToTray by viewModel.closeToTray.collectAsState()

                    SettingsSubsection(title = "BACKGROUND PLAYBACK") {
                        ExpressiveSwitchRow(
                            title = "Close to system tray",
                            subtitle = "Keep music playing in system tray when closing the window.",
                            checked = closeToTray,
                            onCheckedChange = { viewModel.setCloseToTray(it) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_layers), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 1)
                        )
                    }
                }
            }

            SettingsSection.EQUALIZER -> {
                item {
                    EqualizerContent(viewModel = viewModel)
                }
            }

            SettingsSection.ACCOUNTS -> {
                item {
                    AccountsSectionContent()
                }
            }

            SettingsSection.SHORTCUTS -> {
                item {
                    KeyboardShortcutsContent()
                }
            }

            SettingsSection.ABOUT -> {
                item {
                    AboutSectionContent(
                        viewModel = viewModel,
                        onOpenLicenses = onOpenLicensesDetail
                    )
                }
            }

            SettingsSection.BACKUP -> {
                item {
                    BackupRestoreContent()
                }
            }
        }
    }
}
}
}


