package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_auto_fix_high
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_cloud
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_cloud_download
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_drive_file_rename_outline
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_format_list_numbered
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.metadata.BatchSongMetadataUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.CoverArtUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.DesktopMetadataEditor
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.util.Locale

enum class RenumberSortOrder(val label: String) {
    ORIGINAL_TRACK("Track #"),
    FILENAME("File Name"),
    TITLE("Title")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchEditMetadataDialog(
    songs: List<Song>,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    if (songs.isEmpty()) {
        onDismiss()
        return
    }

    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    val commonAlbum = remember(songs) {
        val albums = songs.map { it.album.trim() }.distinct()
        if (albums.size == 1) albums.first() else ""
    }
    val commonArtist = remember(songs) {
        val artists = songs.map { it.artist.trim() }.distinct()
        if (artists.size == 1) artists.first() else ""
    }
    val commonAlbumArtist = remember(songs) {
        val albumArtists = songs.mapNotNull { it.albumArtist?.trim()?.ifBlank { null } }.distinct()
        if (albumArtists.size == 1) albumArtists.first() else ""
    }
    val commonGenre = remember(songs) {
        val genres = songs.mapNotNull { it.genre?.trim()?.ifBlank { null } }.distinct()
        if (genres.size == 1) genres.first() else ""
    }
    val commonYear = remember(songs) {
        val years = songs.map { it.year }.filter { it > 0 }.distinct()
        if (years.size == 1) years.first().toString() else ""
    }
    val commonDisc = remember(songs) {
        val discs = songs.mapNotNull { it.discNumber }.distinct()
        if (discs.size == 1) discs.first().toString() else ""
    }

    var applyAlbum by remember { mutableStateOf(false) }
    var album by remember { mutableStateOf(commonAlbum) }

    var applyArtist by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf(commonArtist) }

    var applyAlbumArtist by remember { mutableStateOf(false) }
    var albumArtist by remember { mutableStateOf(commonAlbumArtist) }

    var applyGenre by remember { mutableStateOf(false) }
    var genre by remember { mutableStateOf(commonGenre) }

    var applyYear by remember { mutableStateOf(false) }
    var year by remember { mutableStateOf(commonYear) }

    var applyDiscNumber by remember { mutableStateOf(false) }
    var discNumber by remember { mutableStateOf(commonDisc) }

    var autoRenumberTracks by remember { mutableStateOf(false) }
    var renumberSortOrder by remember { mutableStateOf(RenumberSortOrder.ORIGINAL_TRACK) }

    val sortedSongs = remember(songs, renumberSortOrder) {
        when (renumberSortOrder) {
            RenumberSortOrder.ORIGINAL_TRACK -> songs.sortedWith(
                compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy { File(it.path).name.lowercase() }
            )
            RenumberSortOrder.FILENAME -> songs.sortedBy { File(it.path).name.lowercase() }
            RenumberSortOrder.TITLE -> songs.sortedBy { it.title.lowercase() }
        }
    }

    var enableFileRename by remember { mutableStateOf(false) }
    var filenamePattern by remember { mutableStateOf("{track} - {title}") }

    var newCoverArtUpdate by remember { mutableStateOf<CoverArtUpdate?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var savedProgress by remember { mutableIntStateOf(0) }
    var progressRatio by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOnlineReleaseDialog by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(880.dp)
                .heightIn(max = 740.dp)
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
                        painter = painterResource(Res.drawable.ic_rounded_auto_fix_high),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batch Edit Metadata",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "${songs.size} songs selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showOnlineReleaseDialog = true },
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
                        Text("Search Album Online", fontSize = 13.sp)
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

                // Content: Left = Artwork & Selection Stats, Right = Form Fields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Left Column (Artwork & Overview)
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .padding(end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfaceContainerHighest),
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
                                newCoverArtUpdate?.isDeletion == true -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_close),
                                            contentDescription = null,
                                            tint = colors.error,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Artwork will be removed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.error,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                                else -> {
                                    val commonArt = songs.firstOrNull()?.albumArtUriString
                                    if (!commonArt.isNullOrBlank()) {
                                        AsyncArtworkImage(
                                            artworkUri = commonArt,
                                            modifier = Modifier.fillMaxSize(),
                                            contentDescription = "Existing artwork",
                                            placeholderIcon = Res.drawable.ic_rounded_album,
                                            placeholderIconSize = 64.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_album),
                                            contentDescription = null,
                                            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val artStatusText = when {
                            newCoverArtUpdate?.isDeletion == true -> "Artwork: Remove from all"
                            newCoverArtUpdate?.bytes != null -> "Artwork: New image selected"
                            else -> "Artwork: Keep existing"
                        }
                        Text(
                            text = artStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("Change", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    newCoverArtUpdate = CoverArtUpdate(isDeletion = true)
                                    previewBitmap = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("Remove", fontSize = 11.sp, color = colors.error)
                            }
                        }

                        if (newCoverArtUpdate != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    newCoverArtUpdate = null
                                    previewBitmap = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Reset Artwork", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Selection Summary",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                                Text(
                                    text = "Total: ${songs.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                                Text(
                                    text = "Artists: ${songs.map { it.artist }.distinct().size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                                Text(
                                    text = "Albums: ${songs.map { it.album }.distinct().size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Right Column (Batch Form Fields)
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
                            Text(
                                text = "Select fields to apply to all selected tracks:",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant
                            )

                            // Album Field with Checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyAlbum,
                                    onCheckedChange = { applyAlbum = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = album,
                                    onValueChange = { album = it },
                                    label = { Text("Album") },
                                    enabled = applyAlbum,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer,
                                        disabledContainerColor = colors.surfaceContainerLow
                                    )
                                )
                            }

                            // Artist Field with Checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyArtist,
                                    onCheckedChange = { applyArtist = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = artist,
                                    onValueChange = { artist = it },
                                    label = { Text("Artist") },
                                    enabled = applyArtist,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer,
                                        disabledContainerColor = colors.surfaceContainerLow
                                    )
                                )
                            }

                            // Album Artist Field with Checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyAlbumArtist,
                                    onCheckedChange = { applyAlbumArtist = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = albumArtist,
                                    onValueChange = { albumArtist = it },
                                    label = { Text("Album Artist") },
                                    enabled = applyAlbumArtist,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer,
                                        disabledContainerColor = colors.surfaceContainerLow
                                    )
                                )
                            }

                            // Genre Field with Checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyGenre,
                                    onCheckedChange = { applyGenre = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = genre,
                                    onValueChange = { genre = it },
                                    label = { Text("Genre") },
                                    enabled = applyGenre,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.surfaceContainer,
                                        unfocusedContainerColor = colors.surfaceContainer,
                                        disabledContainerColor = colors.surfaceContainerLow
                                    )
                                )
                            }

                            // Year & Disc Number Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = applyYear,
                                        onCheckedChange = { applyYear = it },
                                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = year,
                                        onValueChange = { if (it.all { ch -> ch.isDigit() }) year = it },
                                        label = { Text("Year") },
                                        enabled = applyYear,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = colors.surfaceContainer,
                                            unfocusedContainerColor = colors.surfaceContainer,
                                            disabledContainerColor = colors.surfaceContainerLow
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = applyDiscNumber,
                                        onCheckedChange = { applyDiscNumber = it },
                                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = discNumber,
                                        onValueChange = { if (it.all { ch -> ch.isDigit() }) discNumber = it },
                                        label = { Text("Disc") },
                                        enabled = applyDiscNumber,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = colors.surfaceContainer,
                                            unfocusedContainerColor = colors.surfaceContainer,
                                            disabledContainerColor = colors.surfaceContainerLow
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Auto-renumber tracks section with intuitive Order Selector and Live Tracklist Preview
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_format_list_numbered),
                                            contentDescription = null,
                                            tint = if (autoRenumberTracks) colors.primary else colors.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Auto-Renumber Track Numbers",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "Sequentially number tracks 1 to ${songs.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = autoRenumberTracks,
                                            onCheckedChange = { autoRenumberTracks = it }
                                        )
                                    }

                                    if (autoRenumberTracks) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Order by:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                RenumberSortOrder.entries.forEach { order ->
                                                    FilterChip(
                                                        selected = renumberSortOrder == order,
                                                        onClick = { renumberSortOrder = order },
                                                        label = { Text(order.label, fontSize = 12.sp) }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "Numbered Tracklist Preview:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        val previewScrollState = rememberScrollState()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 140.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.surfaceContainerHighest)
                                                .padding(vertical = 6.dp, horizontal = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .verticalScroll(previewScrollState)
                                                    .padding(end = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                sortedSongs.forEachIndexed { index, s ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(colors.surfaceContainer, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = colors.primaryContainer,
                                                            modifier = Modifier.size(width = 28.dp, height = 20.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = String.format("%02d", index + 1),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = colors.onPrimaryContainer,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = s.title,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = colors.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = File(s.path).name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(0.9f)
                                                        )
                                                    }
                                                }
                                            }

                                            VerticalScrollbar(
                                                adapter = rememberScrollbarAdapter(previewScrollState),
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .fillMaxHeight(),
                                                style = defaultScrollbarStyle().copy(
                                                    unhoverColor = colors.onSurfaceVariant.copy(alpha = 0.3f),
                                                    hoverColor = colors.primary.copy(alpha = 0.8f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Filename Renamer section
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_drive_file_rename_outline),
                                            contentDescription = null,
                                            tint = if (enableFileRename) colors.primary else colors.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Rename Audio Files",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "Standardize file names on disk using a pattern",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = enableFileRename,
                                            onCheckedChange = { enableFileRename = it }
                                        )
                                    }

                                    if (enableFileRename) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = filenamePattern,
                                            onValueChange = { filenamePattern = it },
                                            label = { Text("Filename Pattern") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = colors.surfaceContainerHigh,
                                                unfocusedContainerColor = colors.surfaceContainerHigh
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Placeholder chips using FlowRow
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val placeholders = listOf("{track}", "{title}", "{artist}", "{album}")
                                            for (placeholder in placeholders) {
                                                FilterChip(
                                                    selected = false,
                                                    onClick = {
                                                        filenamePattern = if (filenamePattern.isBlank()) placeholder else "$filenamePattern $placeholder"
                                                    },
                                                    label = {
                                                        Text("+ $placeholder", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Clean, non-cramped Live Preview
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Filename Preview (First 2 tracks):",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.primary
                                                )

                                                val previewList = (if (autoRenumberTracks) sortedSongs else songs).take(2)
                                                previewList.forEachIndexed { idx, sampleSong ->
                                                    val sampleExt = File(sampleSong.path).extension
                                                    val sampleTrack = if (autoRenumberTracks) (idx + 1) else (if (sampleSong.trackNumber > 0) sampleSong.trackNumber else (idx + 1))
                                                    val modifiedSong = sampleSong.copy(
                                                        artist = if (applyArtist && artist.isNotBlank()) artist else sampleSong.artist,
                                                        album = if (applyAlbum && album.isNotBlank()) album else sampleSong.album
                                                    )
                                                    val previewName = DesktopMetadataEditor.generateNewFilename(
                                                        pattern = filenamePattern,
                                                        song = modifiedSong,
                                                        trackNumber = sampleTrack,
                                                        extension = sampleExt
                                                    )

                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(colors.surfaceContainer, RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = "Original: ",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colors.onSurfaceVariant,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = File(sampleSong.path).name,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = colors.onSurfaceVariant,
                                                                fontFamily = FontFamily.Monospace,
                                                                softWrap = true,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = "Renamed:  ",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colors.primary,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = previewName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.onSurface,
                                                                fontFamily = FontFamily.Monospace,
                                                                softWrap = true,
                                                                modifier = Modifier.weight(1f)
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

                        // Scrollbar
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            style = defaultScrollbarStyle().copy(
                                unhoverColor = colors.onSurfaceVariant.copy(alpha = 0.35f),
                                hoverColor = colors.primary.copy(alpha = 0.85f)
                            )
                        )

                        // Scroll indicator
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

                if (isSaving) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier.fillMaxWidth(),
                            strokeCap = StrokeCap.Round,
                            gapSize = 4.dp,
                            trackColor = colors.surfaceContainerHighest
                        )
                        Text(
                            text = "Saving tag $savedProgress of ${songs.size}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
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
                            isSaving = true
                            errorMessage = null
                            coroutineScope.launch {
                                val update = BatchSongMetadataUpdate(
                                    album = if (applyAlbum) album.trim() else null,
                                    applyAlbum = applyAlbum,
                                    artist = if (applyArtist) artist.trim() else null,
                                    applyArtist = applyArtist,
                                    albumArtist = if (applyAlbumArtist) albumArtist.trim() else null,
                                    applyAlbumArtist = applyAlbumArtist,
                                    genre = if (applyGenre) genre.trim() else null,
                                    applyGenre = applyGenre,
                                    year = if (applyYear) year.toIntOrNull() else null,
                                    applyYear = applyYear,
                                    discNumber = if (applyDiscNumber) discNumber.toIntOrNull() else null,
                                    applyDiscNumber = applyDiscNumber,
                                    autoRenumberTracks = autoRenumberTracks,
                                    coverArtUpdate = newCoverArtUpdate,
                                    filenamePattern = if (enableFileRename && filenamePattern.isNotBlank()) filenamePattern.trim() else null
                                )

                                val targetSongs = if (autoRenumberTracks) sortedSongs else songs
                                val result = viewModel.batchUpdateMetadata(
                                    songs = targetSongs,
                                    update = update,
                                    onProgress = { completed, total ->
                                        savedProgress = completed
                                        progressRatio = if (total > 0) completed.toFloat() / total.toFloat() else 0f
                                    }
                                )

                                isSaving = false
                                if (result.failureCount == 0) {
                                    onDismiss()
                                } else {
                                    errorMessage = "Saved ${result.successCount} of ${result.total}. ${result.failureCount} failed: ${result.errors.take(2).joinToString("; ")}"
                                }
                            }
                        },
                        enabled = !isSaving && (applyAlbum || applyArtist || applyAlbumArtist || applyGenre || applyYear || applyDiscNumber || autoRenumberTracks || enableFileRename || newCoverArtUpdate != null),
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
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }

    if (showOnlineReleaseDialog) {
        SearchOnlineReleaseDialog(
            initialAlbum = if (applyAlbum && album.isNotBlank()) album else commonAlbum,
            initialArtist = if (applyArtist && artist.isNotBlank()) artist else (if (applyAlbumArtist && albumArtist.isNotBlank()) albumArtist else commonArtist),
            viewModel = viewModel,
            onDismiss = { showOnlineReleaseDialog = false },
            onReleaseSelected = { release, coverArtBytes ->
                showOnlineReleaseDialog = false
                applyAlbum = true
                album = release.title

                if (release.artist.isNotBlank()) {
                    applyArtist = true
                    artist = release.artist
                    applyAlbumArtist = true
                    albumArtist = release.artist
                }

                if (release.year != null && release.year > 0) {
                    applyYear = true
                    year = release.year.toString()
                }

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
                    }
                }
            }
        )
    }
}
