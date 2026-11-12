package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.network.deezer.DeezerClient
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_app_logo
import com.kayoi.pixelplayerdesktop.generated.resources.ic_artist
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_playlist_play
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_show_chart
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_auto_awesome
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_edit
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_history
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavTarget
import com.kayoi.pixelplayerdesktop.presentation.navigation.NavSection

@Composable
fun AppSidebar(
    currentDestination: DesktopNavDestination,
    currentTarget: DesktopNavTarget? = null,
    quickAccessItems: List<QuickAccessItem> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    playlists: List<Playlist> = emptyList(),
    fetchDeezerImages: Boolean = true,
    onNavigate: (DesktopNavDestination) -> Unit,
    onNavigateToTarget: ((DesktopNavTarget) -> Unit)? = null,
    onUnpinQuickAccess: ((String) -> Unit)? = null,
    onOpenQuickAccessSettings: (() -> Unit)? = null,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val animatedWidth by animateDpAsState(
        targetValue = if (isCompact) 72.dp else 200.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sidebarWidth"
    )

    Surface(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight(),
        color = colors.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = colors.surfaceContainerHighest.copy(alpha = 0.60f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_app_logo),
                        contentDescription = "PixelPlayer",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    AnimatedVisibility(
                        visible = !isCompact,
                        enter = fadeIn(tween(180, delayMillis = 60)) + expandHorizontally(tween(200)),
                        exit = fadeOut(tween(80)) + shrinkHorizontally(tween(150))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PixelPlayer",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val navScrollState = rememberScrollState()

            val enabledQuickAccess = remember(quickAccessItems, playlists, albums, artists) {
                quickAccessItems
                    .filter { it.isEnabled }
                    .filter { item ->
                        when (item.type) {
                            QuickAccessType.PLAYLIST -> playlists.isEmpty() || playlists.any { it.id == item.targetKey }
                            QuickAccessType.ALBUM -> albums.isEmpty() || albums.any { it.id.toString() == item.targetKey }
                            QuickAccessType.ARTIST -> artists.isEmpty() || artists.any { it.id.toString() == item.targetKey }
                            QuickAccessType.SMART_PRESET -> true
                        }
                    }
                    .sortedBy { it.order }
            }

            val isCurrentTargetInQuickAccess = remember(enabledQuickAccess, currentTarget) {
                enabledQuickAccess.any { item ->
                    when (item.type) {
                        QuickAccessType.SMART_PRESET -> when (item.targetKey) {
                            QuickAccessItem.PRESET_RECENTLY_PLAYED -> currentTarget is DesktopNavTarget.RecentlyPlayed
                            QuickAccessItem.PRESET_RECENTLY_ADDED -> currentTarget is DesktopNavTarget.RecentlyAdded
                            QuickAccessItem.PRESET_MOST_LISTENED -> currentTarget is DesktopNavTarget.MostListened
                            else -> false
                        }
                        QuickAccessType.ALBUM -> currentTarget is DesktopNavTarget.AlbumDetail && currentTarget.album.id.toString() == item.targetKey
                        QuickAccessType.ARTIST -> currentTarget is DesktopNavTarget.ArtistDetail && currentTarget.artist.id.toString() == item.targetKey
                        QuickAccessType.PLAYLIST -> currentTarget is DesktopNavTarget.PlaylistDetail && currentTarget.playlistId == item.targetKey
                    }
                }
            }

            val pinnedAlbumKeys = remember(enabledQuickAccess) {
                enabledQuickAccess.filter { it.type == QuickAccessType.ALBUM }.map { it.targetKey }.toSet()
            }
            val pinnedArtistKeys = remember(enabledQuickAccess) {
                enabledQuickAccess.filter { it.type == QuickAccessType.ARTIST }.map { it.targetKey }.toSet()
            }
            val pinnedPlaylistKeys = remember(enabledQuickAccess) {
                enabledQuickAccess.filter { it.type == QuickAccessType.PLAYLIST }.map { it.targetKey }.toSet()
            }

            val albumArtMap = remember(albums, pinnedAlbumKeys) {
                if (pinnedAlbumKeys.isEmpty()) emptyMap()
                else {
                    val map = mutableMapOf<String, String?>()
                    for (album in albums) {
                        val key = album.id.toString()
                        if (key in pinnedAlbumKeys) {
                            map[key] = album.albumArtUriString
                            if (map.size == pinnedAlbumKeys.size) break
                        }
                    }
                    map
                }
            }
            val artistImageMap = remember(artists, albums, pinnedArtistKeys, enabledQuickAccess) {
                if (pinnedArtistKeys.isEmpty()) emptyMap()
                else {
                    val map = mutableMapOf<String, String?>()
                    for (key in pinnedArtistKeys) {
                        val artist = artists.find { it.id.toString() == key }
                        val directImage = artist?.effectiveImageUrl
                        val artistName = artist?.name ?: enabledQuickAccess.find { it.targetKey == key }?.title
                        val fallbackAlbumArt = if (directImage.isNullOrBlank() && !artistName.isNullOrBlank()) {
                            albums.firstOrNull { album ->
                                (album.artist.equals(artistName, ignoreCase = true) ||
                                    album.albumArtist?.equals(artistName, ignoreCase = true) == true) &&
                                    !album.albumArtUriString.isNullOrBlank()
                            }?.albumArtUriString
                        } else null
                        map[key] = directImage ?: fallbackAlbumArt
                    }
                    map
                }
            }
            val playlistMap = remember(playlists, pinnedPlaylistKeys) {
                if (pinnedPlaylistKeys.isEmpty()) emptyMap()
                else {
                    val map = mutableMapOf<String, Playlist>()
                    for (playlist in playlists) {
                        if (playlist.id in pinnedPlaylistKeys) {
                            map[playlist.id] = playlist
                            if (map.size == pinnedPlaylistKeys.size) break
                        }
                    }
                    map
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(navScrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavSection.entries.forEach { section ->
                    if (section != NavSection.SYSTEM) {
                        if (isCompact && section == NavSection.LIBRARY) {
                            HorizontalDivider(
                                color = colors.outline.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = !isCompact && section != NavSection.MENU,
                            enter = fadeIn(tween(180, delayMillis = 40)) + expandVertically(tween(180)),
                            exit = fadeOut(tween(60)) + shrinkVertically(tween(140))
                        ) {
                            Text(
                                text = section.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 10.dp, bottom = 4.dp),
                                fontSize = 11.5.sp,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        DesktopNavDestination.entries
                            .filter { it.section == section }
                            .forEach { destination ->
                                SidebarNavItem(
                                    destination = destination,
                                    isSelected = currentDestination == destination && !isCurrentTargetInQuickAccess && (currentTarget == null || currentTarget.rootDestination == destination),
                                    isCompact = isCompact,
                                    onClick = { onNavigate(destination) }
                                )
                            }

                        if (section == NavSection.LIBRARY) {
                            if (isCompact) {
                                HorizontalDivider(
                                    color = colors.outline.copy(alpha = 0.5f),
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = !isCompact,
                                enter = fadeIn(tween(180, delayMillis = 40)) + expandVertically(tween(180)),
                                exit = fadeOut(tween(60)) + shrinkVertically(tween(140))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "QUICK ACCESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                                        fontSize = 11.5.sp,
                                        letterSpacing = 1.1.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )

                                    if (onOpenQuickAccessSettings != null) {
                                        IconButton(
                                            onClick = onOpenQuickAccessSettings,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_rounded_edit),
                                                contentDescription = "Customize Quick Access",
                                                tint = colors.onSurfaceVariant.copy(alpha = 0.75f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (enabledQuickAccess.isNotEmpty()) {
                                enabledQuickAccess.forEach { item ->
                                    val matchedPlaylist = if (item.type == QuickAccessType.PLAYLIST) playlistMap[item.targetKey] else null
                                    val artworkUri = when (item.type) {
                                        QuickAccessType.ALBUM -> albumArtMap[item.targetKey]
                                        QuickAccessType.ARTIST -> artistImageMap[item.targetKey]
                                        QuickAccessType.PLAYLIST -> matchedPlaylist?.coverImageUri
                                        QuickAccessType.SMART_PRESET -> null
                                    }
                                    val displayTitle = when (item.type) {
                                        QuickAccessType.PLAYLIST -> matchedPlaylist?.name ?: item.title
                                        QuickAccessType.ALBUM -> albums.find { it.id.toString() == item.targetKey }?.title ?: item.title
                                        QuickAccessType.ARTIST -> artists.find { it.id.toString() == item.targetKey }?.name ?: item.title
                                        QuickAccessType.SMART_PRESET -> item.title
                                    }

                                    QuickAccessSidebarNavItem(
                                        item = item,
                                        currentTarget = currentTarget,
                                        displayTitle = displayTitle,
                                        artworkUri = artworkUri,
                                        playlistCoverColorArgb = matchedPlaylist?.coverColorArgb,
                                        playlistCoverIconName = matchedPlaylist?.coverIconName,
                                        playlistCoverShapeType = matchedPlaylist?.coverShapeType,
                                        isCompact = isCompact,
                                        fetchDeezerImages = fetchDeezerImages,
                                        onClick = {
                                            if (onNavigateToTarget != null) {
                                                when (item.type) {
                                                    QuickAccessType.SMART_PRESET -> {
                                                        when (item.targetKey) {
                                                            QuickAccessItem.PRESET_RECENTLY_PLAYED -> onNavigateToTarget(DesktopNavTarget.RecentlyPlayed)
                                                            QuickAccessItem.PRESET_RECENTLY_ADDED -> onNavigateToTarget(DesktopNavTarget.RecentlyAdded)
                                                            QuickAccessItem.PRESET_MOST_LISTENED -> onNavigateToTarget(DesktopNavTarget.MostListened)
                                                        }
                                                    }
                                                    QuickAccessType.ALBUM -> {
                                                        val albumId = item.targetKey.toLongOrNull() ?: 0L
                                                        val matched = albums.find { it.id == albumId } ?: Album(id = albumId, title = item.title, artist = "", albumArtUriString = artworkUri)
                                                        onNavigateToTarget(DesktopNavTarget.AlbumDetail(matched))
                                                    }
                                                    QuickAccessType.ARTIST -> {
                                                        val artistId = item.targetKey.toLongOrNull() ?: 0L
                                                        val matched = artists.find { it.id == artistId } ?: Artist(id = artistId, name = item.title, customImageUri = artworkUri)
                                                        onNavigateToTarget(DesktopNavTarget.ArtistDetail(matched))
                                                    }
                                                    QuickAccessType.PLAYLIST -> {
                                                        onNavigateToTarget(DesktopNavTarget.PlaylistDetail(item.targetKey))
                                                    }
                                                }
                                            }
                                        },
                                        onUnpin = {
                                            onUnpinQuickAccess?.invoke(item.id)
                                        },
                                        onOpenSettings = onOpenQuickAccessSettings
                                    )
                                }
                            } else {
                                if (!isCompact) {
                                    Surface(
                                        onClick = { onOpenQuickAccessSettings?.invoke() },
                                        shape = RoundedCornerShape(14.dp),
                                        color = colors.surfaceContainerHighest.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_rounded_edit),
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Customize",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface
                                                )
                                                Text(
                                                    text = "Pin playlists & artists",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    AppTooltip(
                                        text = "Customize Quick Access",
                                        alignment = Alignment.CenterEnd,
                                        offset = DpOffset(12.dp, 0.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onOpenQuickAccessSettings?.invoke() },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(colors.surfaceContainerHighest.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_edit),
                                                    contentDescription = "Customize Quick Access",
                                                    tint = colors.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
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

            HorizontalDivider(
                color = if (isCompact) colors.outline.copy(alpha = 0.5f) else colors.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 4.dp, vertical = 10.dp)
            )

            SidebarNavItem(
                destination = DesktopNavDestination.SETTINGS,
                isSelected = currentDestination == DesktopNavDestination.SETTINGS,
                isCompact = isCompact,
                onClick = { onNavigate(DesktopNavDestination.SETTINGS) }
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    destination: DesktopNavDestination,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor = when {
        isSelected -> colors.secondaryContainer
        isHovered -> colors.surfaceContainerHighest.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val iconColor = when {
        isSelected -> colors.onSecondaryContainer
        isHovered -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    val textColor = when {
        isSelected -> colors.onSurface
        isHovered -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    AppTooltip(
        text = if (isCompact) destination.title else "",
        alignment = Alignment.CenterEnd,
        offset = DpOffset(12.dp, 0.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = containerColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(destination.icon),
                    contentDescription = destination.title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )

                AnimatedVisibility(
                    visible = !isCompact,
                    enter = fadeIn(tween(180, delayMillis = 60)) + expandHorizontally(tween(200)),
                    exit = fadeOut(tween(80)) + shrinkHorizontally(tween(150))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAccessSidebarNavItem(
    item: QuickAccessItem,
    currentTarget: DesktopNavTarget?,
    displayTitle: String,
    artworkUri: String?,
    playlistCoverColorArgb: Int? = null,
    playlistCoverIconName: String? = null,
    playlistCoverShapeType: String? = null,
    isCompact: Boolean,
    fetchDeezerImages: Boolean = true,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    var resolvedArtworkUri by remember(artworkUri) { mutableStateOf(artworkUri) }

    LaunchedEffect(item.targetKey, item.type, displayTitle, fetchDeezerImages) {
        if (item.type == QuickAccessType.ARTIST && displayTitle.isNotBlank()) {
            val fetched = DeezerClient.getArtistImageUrl(
                artistName = displayTitle,
                fetchEnabled = fetchDeezerImages
            )
            if (!fetched.isNullOrBlank()) {
                resolvedArtworkUri = fetched
            }
        }
    }

    val isSelected = when (item.type) {
        QuickAccessType.SMART_PRESET -> {
            when (item.targetKey) {
                QuickAccessItem.PRESET_RECENTLY_PLAYED -> currentTarget is DesktopNavTarget.RecentlyPlayed
                QuickAccessItem.PRESET_RECENTLY_ADDED -> currentTarget is DesktopNavTarget.RecentlyAdded
                QuickAccessItem.PRESET_MOST_LISTENED -> currentTarget is DesktopNavTarget.MostListened
                else -> false
            }
        }
        QuickAccessType.ALBUM -> {
            currentTarget is DesktopNavTarget.AlbumDetail && currentTarget.album.id.toString() == item.targetKey
        }
        QuickAccessType.ARTIST -> {
            currentTarget is DesktopNavTarget.ArtistDetail && currentTarget.artist.id.toString() == item.targetKey
        }
        QuickAccessType.PLAYLIST -> {
            currentTarget is DesktopNavTarget.PlaylistDetail && currentTarget.playlistId == item.targetKey
        }
    }

    val iconResource = when (item.type) {
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

    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }

    val containerColor = when {
        isSelected -> colors.secondaryContainer
        isHovered -> colors.surfaceContainerHighest.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val iconColor = when {
        isSelected -> colors.onSecondaryContainer
        isHovered -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    val textColor = when {
        isSelected -> colors.onSurface
        isHovered -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        AppTooltip(
            text = if (isCompact) displayTitle else "",
            alignment = Alignment.CenterEnd,
            offset = DpOffset(12.dp, 0.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .hoverable(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Secondary),
                        onClick = { isMenuOpen = true }
                    ),
                shape = RoundedCornerShape(16.dp),
                color = containerColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val currentArtworkUri = resolvedArtworkUri ?: artworkUri
                    if (!currentArtworkUri.isNullOrBlank()) {
                        val shape = if (item.type == QuickAccessType.ARTIST) CircleShape else RoundedCornerShape(6.dp)
                        AsyncArtworkImage(
                            artworkUri = currentArtworkUri,
                            targetSize = ArtworkSize.SMALL,
                            shape = shape,
                            placeholderIcon = iconResource,
                            placeholderIconSize = 14.dp,
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = iconColor,
                            contentDescription = displayTitle,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (playlistCoverColorArgb != null) {
                        val bg = Color(playlistCoverColorArgb)
                        val iconTint = if (isColorDark(bg)) Color.White else Color.Black
                        val pShape = remember(playlistCoverShapeType) { getPlaylistShape(playlistCoverShapeType) }
                        val isPill = playlistCoverShapeType == "RotatedPill"
                        val shapeMod = if (isPill) Modifier.graphicsLayer(rotationZ = 45f) else Modifier
                        val iconMod = if (isPill) Modifier.graphicsLayer(rotationZ = -45f) else Modifier
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .then(shapeMod)
                                .clip(pShape)
                                .background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(getPlaylistIconByName(playlistCoverIconName)),
                                contentDescription = displayTitle,
                                tint = iconTint,
                                modifier = Modifier.size(13.dp).then(iconMod)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(iconResource),
                            contentDescription = displayTitle,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = !isCompact,
                        enter = fadeIn(tween(180, delayMillis = 60)) + expandHorizontally(tween(200)),
                        exit = fadeOut(tween(80)) + shrinkHorizontally(tween(150))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("Unpin from Quick Access") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_delete),
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    isMenuOpen = false
                    onUnpin()
                }
            )
            if (onOpenSettings != null) {
                DropdownMenuItem(
                    text = { Text("Customize Quick Access…") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_edit),
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        isMenuOpen = false
                        onOpenSettings()
                    }
                )
            }
        }
    }
}
