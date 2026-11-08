package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_arrow_forward
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_lightbulb
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_add
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_library_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_restart_alt
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_warning
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveCircularProgressIndicator
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveSwitchRow
import com.kayoi.pixelplayerdesktop.presentation.components.trackInputFocus
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistSettingsDetailScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val artistDelimiters by viewModel.artistDelimiters.collectAsState()
    val wordDelimiters by viewModel.wordDelimiters.collectAsState()
    val extractArtistFromTitle by viewModel.extractArtistFromTitle.collectAsState()
    val rescanRequired by viewModel.rescanRequired.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val isScanning = scanProgress is ScanProgress.Scanning

    var newCharDelimiter by remember { mutableStateOf("") }
    var newWordDelimiter by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4 }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = "Artists",
                horizontalPadding = horizontalPadding
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

        item {
            AnimatedVisibility(
                visible = rescanRequired,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                RescanRequiredBanner(
                    isResyncing = isScanning,
                    onRescanClick = onRescanLibrary
                )
            }
        }

        item {
            SettingsSubsection(title = "MULTI-ARTIST PARSING") {
                // 1. Character Delimiters Card
                Surface(
                    shape = getGroupShape(0, 3),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isHeaderCompact = maxWidth < 420.dp

                            if (isHeaderCompact) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Character Delimiters",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )

                                        FilledTonalButton(
                                            onClick = { viewModel.resetArtistDelimiters() },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(painterResource(Res.drawable.ic_rounded_restart_alt), contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Text(
                                        text = "Single symbols used to split multiple artists in song tags",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Character Delimiters",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Single symbols used to split multiple artists in song tags",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { viewModel.resetArtistDelimiters() },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(painterResource(Res.drawable.ic_rounded_restart_alt), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            artistDelimiters.forEach { delimiter ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (artistDelimiters.size > 1) {
                                            viewModel.removeArtistDelimiter(delimiter)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (delimiter == " ") "Space ( )" else "\"$delimiter\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = if (artistDelimiters.size > 1) {
                                        {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_rounded_close),
                                                contentDescription = "Remove delimiter",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer,
                                        selectedTrailingIconColor = colors.onPrimaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCharDelimiter,
                                onValueChange = { newCharDelimiter = it },
                                placeholder = {
                                    Text("Add character delimiter (e.g. / or ;)", style = MaterialTheme.typography.bodyMedium)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newCharDelimiter.isNotBlank()) {
                                            viewModel.addArtistDelimiter(newCharDelimiter)
                                            newCharDelimiter = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f).trackInputFocus()
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            FilledIconButton(
                                onClick = {
                                    if (newCharDelimiter.isNotBlank()) {
                                        viewModel.addArtistDelimiter(newCharDelimiter)
                                        newCharDelimiter = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(painterResource(Res.drawable.ic_rounded_add), contentDescription = "Add character delimiter")
                            }
                        }
                    }
                }

                // 2. Word Delimiters Card
                Surface(
                    shape = getGroupShape(1, 3),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isHeaderCompact = maxWidth < 420.dp

                            if (isHeaderCompact) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Word Delimiters",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )

                                        FilledTonalButton(
                                            onClick = { viewModel.resetWordDelimiters() },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(painterResource(Res.drawable.ic_rounded_restart_alt), contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Text(
                                        text = "Phrases separating featured or collaborating artists",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Word Delimiters",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Phrases separating featured or collaborating artists",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { viewModel.resetWordDelimiters() },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(painterResource(Res.drawable.ic_rounded_restart_alt), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordDelimiters.forEach { delimiter ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeWordDelimiter(delimiter) },
                                    label = {
                                        Text(
                                            text = "\"$delimiter\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_close),
                                            contentDescription = "Remove delimiter",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = colors.secondaryContainer,
                                        selectedLabelColor = colors.onSecondaryContainer,
                                        selectedTrailingIconColor = colors.onSecondaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newWordDelimiter,
                                onValueChange = { newWordDelimiter = it },
                                placeholder = {
                                    Text("Add word delimiter (e.g. feat. or with)", style = MaterialTheme.typography.bodyMedium)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newWordDelimiter.isNotBlank()) {
                                            viewModel.addWordDelimiter(newWordDelimiter)
                                            newWordDelimiter = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f).trackInputFocus()
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            FilledIconButton(
                                onClick = {
                                    if (newWordDelimiter.isNotBlank()) {
                                        viewModel.addWordDelimiter(newWordDelimiter)
                                        newWordDelimiter = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(painterResource(Res.drawable.ic_rounded_add), contentDescription = "Add word delimiter")
                            }
                        }
                    }
                }

                // 3. Extract Artists from Title Switch
                ExpressiveSwitchRow(
                    title = "Extract Artists from Title",
                    subtitle = "Extract featured artists from titles like 'Song (feat. Artist)' or 'Track [with Guest]'",
                    checked = extractArtistFromTitle,
                    onCheckedChange = { viewModel.setExtractArtistFromTitle(it) },
                    leadingIcon = {
                        Icon(painterResource(Res.drawable.ic_rounded_library_music), null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                    },
                    shape = getGroupShape(2, 3)
                )
            }
        }

        item {
            SettingsSubsection(title = "EXAMPLES & TIPS") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_outlined_lightbulb),
                                contentDescription = null,
                                tint = colors.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "How parsing works",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                        }

                        Text(
                            text = "To preserve a delimiter as part of an artist name without splitting, escape it with a double backslash (\\\\). Example: AC\\\\+DC with delimiter \"+\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val samplePairs = listOf(
                            "Calvin Harris, Pharrell Williams" to "[\"Calvin Harris\", \"Pharrell Williams\"]",
                            "Feels (feat. Katy Perry, Big Sean)" to "[\"Katy Perry\", \"Big Sean\"]",
                            "AC\\\\+DC; Queen" to "[\"AC+DC\", \"Queen\"]"
                        )

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isItemCompact = maxWidth < 480.dp

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                samplePairs.forEach { (raw, parsed) ->
                                    if (isItemCompact) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.surfaceContainerHighest.copy(alpha = 0.45f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = raw,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.onSurface
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.ic_automirrored_rounded_arrow_forward),
                                                        contentDescription = null,
                                                        tint = colors.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = parsed,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "• $raw",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_automirrored_rounded_arrow_forward),
                                                contentDescription = null,
                                                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = parsed,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.primary,
                                                fontWeight = FontWeight.SemiBold
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
    }
}
}
}

@Composable
private fun RescanRequiredBanner(
    isResyncing: Boolean,
    onRescanClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 460.dp

            if (isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_warning),
                            contentDescription = null,
                            tint = colors.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )

                        Column {
                            Text(
                                text = "Library rescan required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onTertiaryContainer
                            )
                            Text(
                                text = "Artist delimiter changes will apply after rescanning your media library",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Button(
                        onClick = onRescanClick,
                        enabled = !isResyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.tertiary,
                            contentColor = colors.onTertiary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        if (isResyncing) {
                            ExpressiveCircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.onTertiary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isResyncing) "Scanning..." else "Rescan",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_warning),
                        contentDescription = null,
                        tint = colors.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Library rescan required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onTertiaryContainer
                        )
                        Text(
                            text = "Artist delimiter changes will apply after rescanning your media library",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onRescanClick,
                        enabled = !isResyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.tertiary,
                            contentColor = colors.onTertiary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isResyncing) {
                            ExpressiveCircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.onTertiary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isResyncing) "Scanning..." else "Rescan",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
