package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import com.kayoi.pixelplayerdesktop.generated.resources.ic_jellyfin
import com.kayoi.pixelplayerdesktop.generated.resources.ic_navidrome
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_logout
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_graphic_eq
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_sync
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzAccountState
import com.kayoi.pixelplayerdesktop.domain.repository.JellyfinRepository
import com.kayoi.pixelplayerdesktop.domain.repository.ListenBrainzRepository
import com.kayoi.pixelplayerdesktop.domain.repository.NavidromeRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveCircularProgressIndicator
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.components.trackInputFocus
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlaybackStateHolder
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AccountsSectionContent(
    modifier: Modifier = Modifier,
    navidromeRepository: NavidromeRepository = koinInject(),
    jellyfinRepository: JellyfinRepository = koinInject(),
    listenBrainzRepository: ListenBrainzRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    playbackStateHolder: PlaybackStateHolder = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsState()

    val isNavidromeLoggedIn by navidromeRepository.isLoggedInFlow.collectAsState()
    val navidromeSyncProgress by navidromeRepository.syncProgressFlow.collectAsState()

    val isJellyfinLoggedIn by jellyfinRepository.isLoggedInFlow.collectAsState()
    val jellyfinSyncProgress by jellyfinRepository.syncProgressFlow.collectAsState()

    val lbAccountState by listenBrainzRepository.accountState.collectAsState()

    var showNavidromeDialog by remember { mutableStateOf(false) }
    var showJellyfinDialog by remember { mutableStateOf(false) }
    var showListenBrainzDialog by remember { mutableStateOf(false) }
    var disconnectConfirmTarget by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ServerAccountCard(
            title = "Subsonic / Navidrome",
            subtitle = if (isNavidromeLoggedIn) {
                "${settings.navidromeUsername} @ ${settings.navidromeServerUrl}"
            } else {
                "Connect to your self-hosted music server"
            },
            icon = Res.drawable.ic_navidrome,
            isConnected = isNavidromeLoggedIn,
            syncProgress = navidromeSyncProgress,
            onConnectClick = { showNavidromeDialog = true },
            onDisconnectClick = { disconnectConfirmTarget = "Navidrome" },
            onSyncClick = { coroutineScope.launch { navidromeRepository.syncLibrary() } }
        )

        ServerAccountCard(
            title = "Jellyfin",
            subtitle = if (isJellyfinLoggedIn) {
                "${settings.jellyfinUsername} @ ${settings.jellyfinServerUrl}"
            } else {
                "Connect to your Jellyfin media server"
            },
            icon = Res.drawable.ic_jellyfin,
            isConnected = isJellyfinLoggedIn,
            syncProgress = jellyfinSyncProgress,
            onConnectClick = { showJellyfinDialog = true },
            onDisconnectClick = { disconnectConfirmTarget = "Jellyfin" },
            onSyncClick = { coroutineScope.launch { jellyfinRepository.syncLibrary() } }
        )

        ListenBrainzCard(
            accountState = lbAccountState,
            enabled = settings.listenBrainzEnabled,
            scrobbleLocal = settings.listenBrainzScrobbleLocal,
            scrobbleNavidrome = settings.listenBrainzScrobbleNavidrome,
            scrobbleJellyfin = settings.listenBrainzScrobbleJellyfin,
            onConnectClick = { showListenBrainzDialog = true },
            onDisconnectClick = { disconnectConfirmTarget = "ListenBrainz" },
            onFlushClick = { coroutineScope.launch { listenBrainzRepository.flushQueue() } },
            onToggleEnabled = { enabled ->
                coroutineScope.launch { settingsRepository.updateSettings { it.copy(listenBrainzEnabled = enabled) } }
            },
            onToggleLocal = { enabled ->
                coroutineScope.launch { settingsRepository.updateSettings { it.copy(listenBrainzScrobbleLocal = enabled) } }
            },
            onToggleNavidrome = { enabled ->
                coroutineScope.launch { settingsRepository.updateSettings { it.copy(listenBrainzScrobbleNavidrome = enabled) } }
            },
            onToggleJellyfin = { enabled ->
                coroutineScope.launch { settingsRepository.updateSettings { it.copy(listenBrainzScrobbleJellyfin = enabled) } }
            }
        )
    }

    if (showNavidromeDialog) {
        NavidromeLoginDialog(
            initialServerUrl = settings.navidromeServerUrl ?: "",
            initialUsername = settings.navidromeUsername ?: "",
            onDismiss = { showNavidromeDialog = false },
            onLogin = { url, user, pass ->
                val result = navidromeRepository.login(url, user, pass)
                if (result.isSuccess) {
                    coroutineScope.launch {
                        navidromeRepository.syncLibrary()
                    }
                }
                result
            }
        )
    }

    if (showJellyfinDialog) {
        JellyfinLoginDialog(
            initialServerUrl = settings.jellyfinServerUrl ?: "",
            initialUsername = settings.jellyfinUsername ?: "",
            onDismiss = { showJellyfinDialog = false },
            onLogin = { url, user, pass ->
                val result = jellyfinRepository.login(url, user, pass)
                if (result.isSuccess) {
                    coroutineScope.launch {
                        jellyfinRepository.syncLibrary()
                    }
                }
                result
            }
        )
    }

    if (showListenBrainzDialog) {
        ListenBrainzLoginDialog(
            initialToken = settings.listenBrainzToken ?: "",
            initialServerUrl = settings.listenBrainzServerUrl ?: "",
            onDismiss = { showListenBrainzDialog = false },
            onConnect = { token, serverUrl ->
                listenBrainzRepository.connect(token, serverUrl)
            }
        )
    }

    if (disconnectConfirmTarget != null) {
        TrackDialogInput()
        val target = disconnectConfirmTarget!!
        val isStreamingServer = target == "Navidrome" || target == "Jellyfin"
        AlertDialog(
            onDismissRequest = { disconnectConfirmTarget = null },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_automirrored_rounded_logout),
                    contentDescription = null,
                    tint = colors.error
                )
            },
            title = {
                Text(
                    text = "Disconnect $target",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isStreamingServer) {
                        "Are you sure you want to disconnect from $target? All synced cloud songs and playlists will be removed from your library."
                    } else {
                        "Are you sure you want to disconnect from $target? Your scrobbles will no longer be submitted."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDisconnect = target
                        disconnectConfirmTarget = null
                        coroutineScope.launch {
                            when (toDisconnect) {
                                "Navidrome" -> {
                                    if (playbackStateHolder.currentSong.value?.path?.startsWith("navidrome://") == true) {
                                        playbackStateHolder.stopPlayback()
                                    }
                                    navidromeRepository.logout()
                                }
                                "Jellyfin" -> {
                                    if (playbackStateHolder.currentSong.value?.path?.startsWith("jellyfin://") == true) {
                                        playbackStateHolder.stopPlayback()
                                    }
                                    jellyfinRepository.logout()
                                }
                                "ListenBrainz" -> {
                                    listenBrainzRepository.disconnect()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) {
                    Text("Disconnect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { disconnectConfirmTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ServerAccountCard(
    title: String,
    subtitle: String,
    icon: DrawableResource,
    isConnected: Boolean,
    syncProgress: Pair<Float, String>?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val isSmallScreen = with(density) { windowInfo.containerSize.width.toDp() } < 800.dp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
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
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    tint = colors.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(isConnected = isConnected)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        ServerActionButtons(
                            isConnected = isConnected,
                            syncProgress = syncProgress,
                            onConnectClick = onConnectClick,
                            onDisconnectClick = onDisconnectClick,
                            onSyncClick = onSyncClick,
                            isVertical = true
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    tint = colors.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(isConnected = isConnected)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ServerActionButtons(
                                isConnected = isConnected,
                                syncProgress = syncProgress,
                                onConnectClick = onConnectClick,
                                onDisconnectClick = onDisconnectClick,
                                onSyncClick = onSyncClick
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = syncProgress != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (syncProgress != null) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        LinearProgressIndicator(
                            progress = { syncProgress.first },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            strokeCap = StrokeCap.Round,
                            gapSize = 4.dp,
                            trackColor = colors.surfaceContainerHighest
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = syncProgress.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerActionButtons(
    isConnected: Boolean,
    syncProgress: Pair<Float, String>?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSyncClick: () -> Unit,
    isVertical: Boolean = false
) {
    if (isConnected) {
        if (isVertical) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncClick,
                    enabled = syncProgress == null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (syncProgress != null) {
                        ExpressiveCircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(painter = painterResource(Res.drawable.ic_rounded_sync), contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (syncProgress != null) "Syncing…" else "Sync Library")
                }
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painter = painterResource(Res.drawable.ic_automirrored_rounded_logout), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disconnect")
                }
            }
        } else {
            OutlinedButton(
                onClick = onDisconnectClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(painter = painterResource(Res.drawable.ic_automirrored_rounded_logout), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Disconnect")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSyncClick,
                enabled = syncProgress == null,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (syncProgress != null) {
                    ExpressiveCircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(painter = painterResource(Res.drawable.ic_rounded_sync), contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (syncProgress != null) "Syncing…" else "Sync Library")
            }
        }
    } else {
        Button(
            onClick = onConnectClick,
            shape = RoundedCornerShape(12.dp),
            modifier = if (isVertical) Modifier.fillMaxWidth() else Modifier
        ) {
            Text("Connect Server")
        }
    }
}

@Composable
private fun ListenBrainzCard(
    accountState: ListenBrainzAccountState,
    enabled: Boolean,
    scrobbleLocal: Boolean,
    scrobbleNavidrome: Boolean,
    scrobbleJellyfin: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onFlushClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleLocal: (Boolean) -> Unit,
    onToggleNavidrome: (Boolean) -> Unit,
    onToggleJellyfin: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isConnected = accountState.isConnected

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val isSmallScreen = with(density) { windowInfo.containerSize.width.toDp() } < 800.dp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
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
                                    painter = painterResource(Res.drawable.ic_rounded_graphic_eq),
                                    contentDescription = null,
                                    tint = colors.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ListenBrainz",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(isConnected = isConnected)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isConnected) {
                                        val pendingPart = if (accountState.pendingCount > 0) " • ${accountState.pendingCount} pending" else ""
                                        "Connected as @${accountState.userName ?: "User"}$pendingPart"
                                    } else {
                                        "Scrobble your listening history to ListenBrainz or custom Maloja"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        ListenBrainzActionButtons(
                            isConnected = isConnected,
                            pendingCount = accountState.pendingCount,
                            onConnectClick = onConnectClick,
                            onDisconnectClick = onDisconnectClick,
                            onFlushClick = onFlushClick,
                            isVertical = true
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_graphic_eq),
                                    contentDescription = null,
                                    tint = colors.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ListenBrainz",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(isConnected = isConnected)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isConnected) {
                                        val pendingPart = if (accountState.pendingCount > 0) " • ${accountState.pendingCount} pending" else ""
                                        "Connected as @${accountState.userName ?: "User"}$pendingPart"
                                    } else {
                                        "Scrobble your listening history to ListenBrainz or custom Maloja"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ListenBrainzActionButtons(
                                isConnected = isConnected,
                                pendingCount = accountState.pendingCount,
                                onConnectClick = onConnectClick,
                                onDisconnectClick = onDisconnectClick,
                                onFlushClick = onFlushClick
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isConnected && accountState.lastError != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_warning),
                            contentDescription = null,
                            tint = colors.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = accountState.lastError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onErrorContainer
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isConnected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    ScrobbleToggleRow(
                        title = "Enable Scrobbling",
                        subtitle = "Submit playback sessions to your ListenBrainz account",
                        checked = enabled,
                        onCheckedChange = onToggleEnabled
                    )
                    ScrobbleToggleRow(
                        title = "Scrobble local files",
                        subtitle = "Record listens from your local music collection",
                        checked = scrobbleLocal,
                        onCheckedChange = onToggleLocal
                    )
                    ScrobbleToggleRow(
                        title = "Scrobble Subsonic streaming",
                        subtitle = "Record listens streamed from Navidrome/Subsonic",
                        checked = scrobbleNavidrome,
                        onCheckedChange = onToggleNavidrome
                    )
                    ScrobbleToggleRow(
                        title = "Scrobble Jellyfin streaming",
                        subtitle = "Record listens streamed from Jellyfin",
                        checked = scrobbleJellyfin,
                        onCheckedChange = onToggleJellyfin
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If your server already scrobbles to ListenBrainz, turn that source off here to avoid duplicate listens.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ListenBrainzActionButtons(
    isConnected: Boolean,
    pendingCount: Int,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onFlushClick: () -> Unit,
    isVertical: Boolean = false
) {
    if (isConnected) {
        if (isVertical) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onFlushClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painter = painterResource(Res.drawable.ic_rounded_refresh), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (pendingCount > 0) "Flush Queue ($pendingCount)" else "Flush Queue")
                }
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painter = painterResource(Res.drawable.ic_automirrored_rounded_logout), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disconnect")
                }
            }
        } else {
            OutlinedButton(
                onClick = onDisconnectClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(painter = painterResource(Res.drawable.ic_automirrored_rounded_logout), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Disconnect")
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onFlushClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(painter = painterResource(Res.drawable.ic_rounded_refresh), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (pendingCount > 0) "Flush Queue ($pendingCount)" else "Flush Queue")
            }
        }
    } else {
        Button(
            onClick = onConnectClick,
            shape = RoundedCornerShape(12.dp),
            modifier = if (isVertical) Modifier.fillMaxWidth() else Modifier
        ) {
            Text("Connect ListenBrainz")
        }
    }
}


@Composable
private fun ScrobbleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusBadge(isConnected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val bgColor = if (isConnected) colors.primaryContainer else colors.surfaceContainerHighest
    val textColor = if (isConnected) colors.onPrimaryContainer else colors.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isConnected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun NavidromeLoginDialog(
    initialServerUrl: String,
    initialUsername: String,
    onDismiss: () -> Unit,
    onLogin: suspend (String, String, String) -> Result<String>
) {
    TrackDialogInput()
    val coroutineScope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 440.dp, max = 520.dp),
        title = { Text("Subsonic / Navidrome") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Supports Navidrome, Airsonic, Gonic, Ampache and other servers compatible with the Subsonic API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; errorMessage = null },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.100:4533") },
                    supportingText = { Text("Use the full server URL, including http:// or https://") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("Username") },
                    placeholder = { Text("admin") },
                    supportingText = { Text("Your Subsonic or Navidrome account name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    supportingText = { Text("App password also works if supported by your server") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = onLogin(serverUrl, username, password)
                        isLoading = false
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    ExpressiveCircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun JellyfinLoginDialog(
    initialServerUrl: String,
    initialUsername: String,
    onDismiss: () -> Unit,
    onLogin: suspend (String, String, String) -> Result<String>
) {
    TrackDialogInput()
    val coroutineScope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 440.dp, max = 520.dp),
        title = { Text("Jellyfin") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Connects to Jellyfin servers. Both HTTP and HTTPS are supported for local network access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; errorMessage = null },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.100:8096") },
                    supportingText = { Text("Full URL including port. Local and remote addresses supported") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("Username") },
                    placeholder = { Text("admin") },
                    supportingText = { Text("Your Jellyfin account username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    supportingText = { Text("Your Jellyfin account password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serverUrl.isBlank() || username.isBlank()) {
                        errorMessage = "Please enter server URL and username"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = onLogin(serverUrl, username, password)
                        isLoading = false
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    ExpressiveCircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ListenBrainzLoginDialog(
    initialToken: String,
    initialServerUrl: String,
    onDismiss: () -> Unit,
    onConnect: suspend (String, String?) -> Result<String>
) {
    TrackDialogInput()
    val coroutineScope = rememberCoroutineScope()
    var token by remember { mutableStateOf(initialToken) }
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 440.dp, max = 520.dp),
        title = { Text("ListenBrainz") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Paste your user token to scrobble your listens to ListenBrainz. You can find it in your settings on listenbrainz.org.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it; errorMessage = null },
                    label = { Text("User Token") },
                    placeholder = { Text("Your user token from listenbrainz.org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; errorMessage = null },
                    label = { Text("Server URL (optional)") },
                    placeholder = { Text("https://api.listenbrainz.org/") },
                    supportingText = { Text("Leave empty for listenbrainz.org. Works with Maloja or custom servers.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (token.isBlank()) {
                        errorMessage = "Please enter your user token"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = onConnect(token, serverUrl.takeIf { it.isNotBlank() })
                        isLoading = false
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Validation failed"
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    ExpressiveCircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
