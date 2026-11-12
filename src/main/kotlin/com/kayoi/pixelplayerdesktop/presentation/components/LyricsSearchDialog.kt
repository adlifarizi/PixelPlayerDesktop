package com.kayoi.pixelplayerdesktop.presentation.components

import java.io.File

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_lyrics
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_file_open
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_search
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.LyricsSearchUiState
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSearchDialog(
    currentSong: Song?,
    uiState: LyricsSearchUiState,
    onSearch: (String) -> Unit,
    onSelectLyrics: (Lyrics) -> Unit,
    onImportFile: (File) -> Unit,
    onDismiss: () -> Unit
) {
    TrackDialogInput()
    val initialQuery = remember(currentSong) {
        if (currentSong != null) {
            val cleanArtist = currentSong.artist.split(",", "/", "&", "feat.", "ft.").firstOrNull()?.trim().orEmpty()
            if (cleanArtist.isNotBlank()) "$cleanArtist ${currentSong.title}".trim() else currentSong.title.trim()
        } else ""
    }

    var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery) }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(currentSong) {
        if (initialQuery.isNotBlank() && uiState is LyricsSearchUiState.Idle) {
            onSearch(initialQuery)
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(520.dp)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_lyrics),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Search Lyrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_close),
                            contentDescription = "Close",
                            tint = colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Song title or artist...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.isNotBlank() && uiState !is LyricsSearchUiState.Loading) {
                                onSearch(searchQuery)
                            }
                        }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .trackInputFocus()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    if (searchQuery.isNotBlank() && uiState !is LyricsSearchUiState.Loading) {
                                        onSearch(searchQuery)
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceContainer,
                            unfocusedContainerColor = colors.surfaceContainer,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outlineVariant.copy(alpha = 0.5f)
                        )
                    )

                    Button(
                        onClick = { onSearch(searchQuery) },
                        shape = RoundedCornerShape(14.dp),
                        enabled = searchQuery.isNotBlank() && uiState !is LyricsSearchUiState.Loading
                    ) {
                        Icon(painterResource(Res.drawable.ic_rounded_search), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState) {
                        is LyricsSearchUiState.Idle -> {
                            Text(
                                text = "Enter search terms above or import a local lyric file",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }

                        is LyricsSearchUiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ContainedLoadingIndicator(
                                    containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                                    indicatorColor = colors.primary
                                )
                                Text(
                                    text = "Searching online...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        is LyricsSearchUiState.NotFound -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No lyrics found for \"${uiState.query}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Try adjusting the title or artist name above",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        is LyricsSearchUiState.Error -> {
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.error
                            )
                        }

                        is LyricsSearchUiState.PickResult -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.results) { candidate ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectLyrics(candidate.lyrics) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = candidate.trackName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val details = buildString {
                                                    append(candidate.artistName)
                                                    if (!candidate.albumName.isNullOrBlank()) {
                                                        append(" • ").append(candidate.albumName)
                                                    }
                                                }
                                                Text(
                                                    text = details,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (candidate.isSynced) colors.primaryContainer else colors.secondaryContainer
                                            ) {
                                                Text(
                                                    text = if (candidate.isSynced) "SYNCED" else "PLAIN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (candidate.isSynced) colors.onPrimaryContainer else colors.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            DirectoryPicker.chooseLrcFile { file ->
                                onImportFile(file)
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_outlined_file_open),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Lyrics")
                    }

                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
