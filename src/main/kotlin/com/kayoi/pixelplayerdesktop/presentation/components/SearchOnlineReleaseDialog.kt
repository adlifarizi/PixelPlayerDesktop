package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image as SkiaImage

import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check_circle
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_cloud_download
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_search
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOnlineReleaseDialog(
    initialAlbum: String,
    initialArtist: String,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onReleaseSelected: (release: OnlineReleaseMetadata, coverArtBytes: ByteArray?) -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("$initialAlbum $initialArtist".trim()) }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineReleaseMetadata>>(emptyList()) }
    var selectedRelease by remember { mutableStateOf<OnlineReleaseMetadata?>(null) }
    var isLoadingCoverArt by remember { mutableStateOf(false) }
    var isCoverArtFailed by remember { mutableStateOf(false) }
    var coverArtBytes by remember { mutableStateOf<ByteArray?>(null) }
    var coverArtJob by remember { mutableStateOf<Job?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun loadCoverArt(releaseId: String?) {
        coverArtJob?.cancel()
        if (releaseId.isNullOrBlank()) {
            coverArtBytes = null
            isCoverArtFailed = false
            return
        }
        coverArtJob = coroutineScope.launch {
            isLoadingCoverArt = true
            isCoverArtFailed = false
            val result = viewModel.fetchOnlineCoverArt(releaseId)
            result.fold(
                onSuccess = { bytes ->
                    coverArtBytes = bytes
                    isCoverArtFailed = false
                },
                onFailure = {
                    coverArtBytes = null
                    isCoverArtFailed = true
                }
            )
            isLoadingCoverArt = false
        }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        selectedRelease = null
        coverArtBytes = null
        isCoverArtFailed = false
        hasSearched = true
        coroutineScope.launch {
            val list = viewModel.searchOnlineReleases(query)
            results = list
            val first = list.firstOrNull()
            selectedRelease = first
            isSearching = false
            loadCoverArt(first?.id)
        }
    }

    LaunchedEffect(Unit) {
        if (searchQuery.isNotBlank()) {
            performSearch(searchQuery)
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(760.dp)
                .heightIn(max = 660.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_cloud_download),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MusicBrainz Album Auto-Tagger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Search online releases for album information and cover art",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(Res.drawable.ic_rounded_close), contentDescription = "Close", tint = colors.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search album title and artist") },
                        modifier = Modifier
                            .weight(1f)
                            .trackInputFocus()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    if (!isSearching && searchQuery.isNotBlank()) {
                                        performSearch(searchQuery)
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (!isSearching && searchQuery.isNotBlank()) {
                                performSearch(searchQuery)
                            }
                        }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceContainer,
                            unfocusedContainerColor = colors.surfaceContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { performSearch(searchQuery) },
                        enabled = !isSearching && searchQuery.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(Res.drawable.ic_rounded_search), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val listState = rememberLazyListState()
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        when {
                            isSearching -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        ContainedLoadingIndicator(
                                            containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                                            indicatorColor = colors.primary
                                        )
                                        Text(
                                            text = "Searching MusicBrainz...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            results.isEmpty() && hasSearched -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No album releases found. Try adjusting query.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(results) { item ->
                                        val isSelected = selectedRelease == item
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedRelease = item
                                                    loadCoverArt(item.id)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val subText = buildString {
                                                        append(item.artist)
                                                        if (item.year != null && item.year > 0) {
                                                            append(" • ")
                                                            append(item.year)
                                                        }
                                                        if (item.trackCount != null && item.trackCount > 0) {
                                                            append(" • ")
                                                            append("${item.trackCount} tracks")
                                                        }
                                                        if (!item.country.isNullOrBlank()) {
                                                            append(" [${item.country}]")
                                                        }
                                                    }
                                                    Text(
                                                        text = subText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) colors.primary else colors.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        text = "${item.score}%",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(listState),
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    style = defaultScrollbarStyle().copy(
                                        unhoverColor = colors.onSurfaceVariant.copy(alpha = 0.3f),
                                        hoverColor = colors.primary.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val release = selectedRelease
                        if (release == null) {
                            Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Select an album release to preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isLoadingCoverArt -> ExpressiveLoadingIndicator(size = 32.dp)
                                        isCoverArtFailed -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { loadCoverArt(release.id) }
                                                    .padding(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_refresh),
                                                    contentDescription = "Retry fetching cover art",
                                                    tint = colors.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Failed to load",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.error,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "Click to retry",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = colors.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        coverArtBytes != null -> {
                                            val bmp = remember(coverArtBytes) {
                                                runCatching { SkiaImage.makeFromEncoded(coverArtBytes!!).toComposeImageBitmap() }.getOrNull()
                                            }
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp,
                                                    contentDescription = "Cover Art",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_album),
                                                    contentDescription = null,
                                                    tint = colors.onSurfaceVariant,
                                                    modifier = Modifier.size(42.dp)
                                                )
                                            }
                                        }
                                        else -> Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_album),
                                            contentDescription = null,
                                            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = release.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = release.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (release.year != null && release.year > 0) {
                                        Text(
                                            text = "Year: ${release.year}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    if (release.trackCount != null && release.trackCount > 0) {
                                        Text(
                                            text = "Tracks: ${release.trackCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    if (!release.country.isNullOrBlank()) {
                                        Text(
                                            text = "Country: ${release.country}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val release = selectedRelease
                            if (release != null) {
                                onReleaseSelected(release, coverArtBytes)
                                onDismiss()
                            }
                        },
                        enabled = selectedRelease != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(Res.drawable.ic_rounded_check_circle), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to Selected Songs")
                    }
                }
            }
        }
    }
}
