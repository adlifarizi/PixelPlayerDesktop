package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_cloud
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_cloud_download
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_edit
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.metadata.AudioMetadataDetails
import com.kayoi.pixelplayerdesktop.data.metadata.CoverArtUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.util.Locale

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongMetadataDialog(
    song: Song,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.displayArtist) }
    var album by remember { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf(song.albumArtist ?: "") }
    var composer by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(song.genre ?: "") }
    var year by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var trackNumber by remember { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var discNumber by remember { mutableStateOf(song.discNumber?.toString() ?: "") }
    var existingTrackTotal by remember { mutableStateOf<Int?>(null) }
    var existingDiscTotal by remember { mutableStateOf<Int?>(null) }
    var lyrics by remember { mutableStateOf(song.lyrics ?: "") }

    var audioDetails by remember { mutableStateOf<AudioMetadataDetails?>(null) }
    var isCoverArtDeleted by remember { mutableStateOf(false) }
    var newCoverArtUpdate by remember { mutableStateOf<CoverArtUpdate?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOnlineSearchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        isLoadingDetails = true
        val audioFile = File(song.path)
        val readResult = viewModel.readSongMetadataDetails(audioFile)
        if (readResult.isSuccess) {
            val details = readResult.getOrNull()
            if (details != null) {
                audioDetails = details
                title = details.title
                artist = details.artist
                album = details.album
                albumArtist = details.albumArtist ?: ""
                composer = details.composer ?: ""
                genre = details.genre ?: ""
                year = details.year?.toString() ?: ""
                trackNumber = details.trackNumber?.toString() ?: ""
                existingTrackTotal = details.trackTotal
                discNumber = details.discNumber?.toString() ?: ""
                existingDiscTotal = details.discTotal
                lyrics = details.lyrics ?: ""

                if (details.artworkBytes != null && details.artworkBytes.isNotEmpty()) {
                    previewBitmap = runCatching {
                        SkiaImage.makeFromEncoded(details.artworkBytes).toComposeImageBitmap()
                    }.getOrNull()
                }
            }
        }
        isLoadingDetails = false
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(750.dp)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_edit),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Song Metadata",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = File(song.path).name,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = { showOnlineSearchDialog = true },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_cloud_download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Online", fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_close),
                            contentDescription = "Close",
                            tint = colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content: Left = Artwork & Audio Specs, Right = Form Fields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Left Column (Artwork & File Info)
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .padding(end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfaceContainerHighest)
                                .border(
                                    1.dp,
                                    colors.outlineVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                previewBitmap != null -> {
                                    Image(
                                        bitmap = previewBitmap!!,
                                        contentDescription = "Artwork Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                !isCoverArtDeleted && song.albumArtUriString != null -> {
                                    AsyncArtworkImage(
                                        artworkUri = song.albumArtUriString,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(18.dp),
                                        placeholderIcon = Res.drawable.ic_rounded_music_note,
                                        placeholderIconSize = 48.dp
                                    )
                                }
                                else -> {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_music_note),
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = colors.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    DirectoryPicker.chooseImageFile { selectedFile ->
                                        runCatching {
                                            val bytes = selectedFile.readBytes()
                                            val ext = selectedFile.extension.lowercase(Locale.ROOT)
                                            val mime = when (ext) {
                                                "png" -> "image/png"
                                                "webp" -> "image/webp"
                                                else -> "image/jpeg"
                                            }
                                            val bmp = runCatching {
                                                SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                                            }.getOrNull()

                                            previewBitmap = bmp
                                            newCoverArtUpdate = CoverArtUpdate(bytes = bytes, mimeType = mime, isDeletion = false)
                                            isCoverArtDeleted = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Change", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    isCoverArtDeleted = true
                                    newCoverArtUpdate = CoverArtUpdate(isDeletion = true)
                                    previewBitmap = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Remove", fontSize = 12.sp, color = colors.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio specs summary card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val details = audioDetails
                                val ext = File(song.path).extension.uppercase()
                                val formatLabel = details?.format?.ifBlank { null }
                                    ?: AudioMetaUtils.mimeTypeToFormat(song.mimeType).takeIf { it != "-" }?.uppercase(Locale.getDefault())
                                    ?: ext.ifBlank { "AUDIO" }
                                Text(
                                    text = "Format: $formatLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                                val bitrateText = AudioMetaUtils.formatBitrate(details?.bitrate ?: song.bitrate)
                                if (bitrateText != null) {
                                    Text(
                                        text = "Bitrate: $bitrateText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                val sampleRateText = AudioMetaUtils.formatSampleRate(details?.sampleRate ?: song.sampleRate)
                                if (sampleRateText != null) {
                                    Text(
                                        text = "Sample Rate: $sampleRateText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                val durationText = formatDuration(details?.durationMs ?: song.duration)
                                Text(
                                    text = "Duration: $durationText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                                val sizeText = formatFileSize(details?.fileSize ?: File(song.path).length())
                                Text(
                                    text = "Size: $sizeText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Right Column (Metadata Form Fields)
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(end = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainer,
                                    unfocusedContainerColor = colors.surfaceContainer
                                )
                            )

                            OutlinedTextField(
                                value = artist,
                                onValueChange = { artist = it },
                                label = { Text("Artist") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainer,
                                    unfocusedContainerColor = colors.surfaceContainer
                                )
                            )

                            OutlinedTextField(
                                value = album,
                                onValueChange = { album = it },
                                label = { Text("Album") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainer,
                                    unfocusedContainerColor = colors.surfaceContainer
                                )
                            )

                            OutlinedTextField(
                                value = albumArtist,
                                onValueChange = { albumArtist = it },
                                label = { Text("Album Artist") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainer,
                                    unfocusedContainerColor = colors.surfaceContainer
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = genre,
                                    onValueChange = { genre = it },
                                    label = { Text("Genre") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer
                                    )
                                )

                                OutlinedTextField(
                                    value = year,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 4) {
                                            year = input
                                        }
                                    },
                                    label = { Text("Year") },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = trackNumber,
                                    onValueChange = { input -> if (input.all { it.isDigit() }) trackNumber = input },
                                    label = { Text("Track Number") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer
                                    )
                                )

                                OutlinedTextField(
                                    value = discNumber,
                                    onValueChange = { input -> if (input.all { it.isDigit() }) discNumber = input },
                                    label = { Text("Disc Number") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = composer,
                                onValueChange = { composer = it },
                                label = { Text("Composer") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainer,
                                    unfocusedContainerColor = colors.surfaceContainer
                                )
                            )

                            OutlinedTextField(
                                value = lyrics,
                                onValueChange = { lyrics = it },
                                label = { Text("Embedded Lyrics") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainer,
                                    unfocusedContainerColor = colors.surfaceContainer
                                )
                            )
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            style = defaultScrollbarStyle().copy(
                                unhoverColor = colors.onSurfaceVariant.copy(alpha = 0.35f),
                                hoverColor = colors.primary.copy(alpha = 0.85f)
                            )
                        )

                        if (scrollState.canScrollForward) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .padding(end = 14.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                colors.surfaceContainerHigh.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Title cannot be empty"
                                return@Button
                            }

                            val parsedYear = year.trim().toIntOrNull()
                            val parsedTrack = trackNumber.trim().toIntOrNull()
                            val parsedDisc = discNumber.trim().toIntOrNull()

                            val metadata = EditableSongMetadata(
                                title = title.trim(),
                                artist = artist.trim(),
                                album = album.trim(),
                                albumArtist = albumArtist.trim().ifBlank { null },
                                composer = composer.trim().ifBlank { null },
                                genre = genre.trim().ifBlank { null },
                                year = parsedYear,
                                trackNumber = parsedTrack,
                                trackTotal = existingTrackTotal,
                                discNumber = parsedDisc,
                                discTotal = existingDiscTotal,
                                lyrics = lyrics.trim().ifBlank { null },
                                coverArtUpdate = newCoverArtUpdate
                            )

                            isSaving = true
                            errorMessage = null
                            scope.launch {
                                val result = viewModel.updateSongMetadata(song, metadata)
                                isSaving = false
                                if (result.success) {
                                    onDismiss()
                                } else {
                                    errorMessage = result.error ?: "Failed to save metadata"
                                }
                            }
                        },
                        enabled = !isSaving && !isLoadingDetails,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            ExpressiveCircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text("Save Metadata", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showOnlineSearchDialog) {
        SearchOnlineMetadataDialog(
            initialTitle = title.ifBlank { song.title },
            initialArtist = artist.ifBlank { song.displayArtist },
            viewModel = viewModel,
            onDismiss = { showOnlineSearchDialog = false },
            onMetadataSelected = { onlineMeta, coverArtBytes ->
                showOnlineSearchDialog = false
                title = onlineMeta.title
                artist = onlineMeta.artist
                onlineMeta.album?.let { album = it }
                onlineMeta.albumArtist?.let { albumArtist = it }
                onlineMeta.genre?.let { genre = it }
                onlineMeta.year?.let { year = it.toString() }
                onlineMeta.trackNumber?.let { trackNumber = it.toString() }
                onlineMeta.discNumber?.let { discNumber = it.toString() }

                if (coverArtBytes != null && coverArtBytes.isNotEmpty()) {
                    val bmp = runCatching {
                        SkiaImage.makeFromEncoded(coverArtBytes).toComposeImageBitmap()
                    }.getOrNull()
                    if (bmp != null) {
                        previewBitmap = bmp
                        newCoverArtUpdate = CoverArtUpdate(
                            bytes = coverArtBytes,
                            mimeType = "image/jpeg",
                            isDeletion = false
                        )
                        isCoverArtDeleted = false
                    }
                }
            }
        )
    }
}
