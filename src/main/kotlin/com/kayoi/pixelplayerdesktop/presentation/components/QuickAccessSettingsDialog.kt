package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_artist
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_playlist_play
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_show_chart
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_auto_awesome
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_drag_indicator
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_edit
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_history

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAccessSettingsDialog(
    items: List<QuickAccessItem>,
    playlists: List<Playlist> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (List<QuickAccessItem>) -> Unit
) {
    var workingList by remember(items, playlists, albums, artists) {
        val syncedList = items
            .filter { item ->
                when (item.type) {
                    QuickAccessType.PLAYLIST -> playlists.isEmpty() || playlists.any { it.id == item.targetKey }
                    QuickAccessType.ALBUM -> albums.isEmpty() || albums.any { it.id.toString() == item.targetKey }
                    QuickAccessType.ARTIST -> artists.isEmpty() || artists.any { it.id.toString() == item.targetKey }
                    QuickAccessType.SMART_PRESET -> true
                }
            }
            .map { item ->
                val resolvedTitle = when (item.type) {
                    QuickAccessType.PLAYLIST -> playlists.find { it.id == item.targetKey }?.name ?: item.title
                    QuickAccessType.ALBUM -> albums.find { it.id.toString() == item.targetKey }?.title ?: item.title
                    QuickAccessType.ARTIST -> artists.find { it.id.toString() == item.targetKey }?.name ?: item.title
                    QuickAccessType.SMART_PRESET -> item.title
                }
                if (resolvedTitle != item.title) item.copy(title = resolvedTitle) else item
            }
        mutableStateOf(syncedList.sortedBy { it.order })
    }
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            val currentList = workingList.toMutableList()
            val moved = currentList.removeAt(from.index)
            currentList.add(to.index, moved)
            workingList = currentList.mapIndexed { idx, itm -> itm.copy(order = idx) }
        }
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(460.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_edit),
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Customize Quick Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Toggle visibility and drag to reorder items",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = workingList,
                        key = { it.id }
                    ) { item ->
                        ReorderableItem(
                            state = reorderableState,
                            key = item.id
                        ) { isDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 1.025f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "scaleAnimation"
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDragging) colors.surfaceContainerHighest else colors.surfaceContainer,
                                tonalElevation = if (isDragging) 4.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .draggableHandle()
                                            .size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_drag_indicator),
                                            contentDescription = "Reorder item",
                                            tint = if (isDragging) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    val itemIcon = when (item.type) {
                                        QuickAccessType.SMART_PRESET -> when (item.targetKey) {
                                            QuickAccessItem.PRESET_RECENTLY_ADDED -> Res.drawable.ic_rounded_auto_awesome
                                            QuickAccessItem.PRESET_MOST_LISTENED -> Res.drawable.ic_automirrored_rounded_show_chart
                                            QuickAccessItem.PRESET_RECENTLY_PLAYED -> Res.drawable.ic_rounded_history
                                            else -> Res.drawable.ic_rounded_auto_awesome
                                        }
                                        QuickAccessType.ALBUM -> Res.drawable.ic_rounded_album
                                        QuickAccessType.ARTIST -> Res.drawable.ic_artist
                                        QuickAccessType.PLAYLIST -> Res.drawable.ic_automirrored_rounded_playlist_play
                                    }

                                    Icon(
                                        painter = painterResource(itemIcon),
                                        contentDescription = null,
                                        tint = if (item.isEnabled) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (item.isEnabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val subtitle = when (item.type) {
                                            QuickAccessType.SMART_PRESET -> "Smart Playlist"
                                            QuickAccessType.ALBUM -> "Album"
                                            QuickAccessType.ARTIST -> "Artist"
                                            QuickAccessType.PLAYLIST -> "Playlist"
                                        }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }

                                    if (item.type != QuickAccessType.SMART_PRESET) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                val mutable = workingList.toMutableList()
                                                mutable.removeAll { it.id == item.id }
                                                workingList = mutable.mapIndexed { idx, itm -> itm.copy(order = idx) }
                                            },
                                            modifier = Modifier.size(28.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = colors.errorContainer.copy(alpha = 0.6f),
                                                contentColor = colors.onErrorContainer
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_rounded_delete),
                                                contentDescription = "Unpin",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Checkbox(
                                        checked = item.isEnabled,
                                        onCheckedChange = { checked ->
                                            workingList = workingList.map { itm ->
                                                if (itm.id == item.id) itm.copy(isEnabled = checked) else itm
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            workingList = QuickAccessItem.DEFAULT_ITEMS
                        }
                    ) {
                        Text("Reset to Defaults", color = colors.primary)
                    }

                    Button(
                        onClick = {
                            onSave(workingList.mapIndexed { idx, itm -> itm.copy(order = idx) })
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
