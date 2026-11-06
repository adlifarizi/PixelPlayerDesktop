package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ShortcutItem(
    val title: String,
    val description: String? = null,
    val keyCombos: List<List<String>>
)

data class ShortcutCategory(
    val categoryName: String,
    val items: List<ShortcutItem>
)

private val isMacOs = System.getProperty("os.name")?.lowercase()?.contains("mac") == true
private val modKey = if (isMacOs) "⌘" else "Ctrl"

val defaultShortcutCategories = listOf(
    ShortcutCategory(
        categoryName = "Playback Controls",
        items = listOf(
            ShortcutItem(
                title = "Play / Pause",
                description = "Toggle audio playback when not in a text field",
                keyCombos = listOf(listOf("Space"))
            ),
            ShortcutItem(
                title = "Next Track",
                description = "Skip to the next song in the active queue",
                keyCombos = listOf(listOf(modKey, "→"))
            ),
            ShortcutItem(
                title = "Previous Track",
                description = "Return to previous track or start of current song",
                keyCombos = listOf(listOf(modKey, "←"))
            ),
            ShortcutItem(
                title = "Seek Forward",
                description = "Fast forward by 5 seconds",
                keyCombos = listOf(listOf("→"))
            ),
            ShortcutItem(
                title = "Seek Backward",
                description = "Rewind by 5 seconds",
                keyCombos = listOf(listOf("←"))
            ),
            ShortcutItem(
                title = "Toggle Shuffle",
                description = "Randomize queue playback order",
                keyCombos = listOf(listOf(modKey, "S"))
            ),
            ShortcutItem(
                title = "Cycle Repeat Mode",
                description = "Cycle between Repeat Off, Repeat All, and Repeat One",
                keyCombos = listOf(listOf(modKey, "R"))
            ),
            ShortcutItem(
                title = "Favorite Song",
                description = "Add or remove current song from Favorites",
                keyCombos = listOf(listOf("L"))
            )
        )
    ),
    ShortcutCategory(
        categoryName = "Volume & Audio",
        items = listOf(
            ShortcutItem(
                title = "Volume Up",
                description = "Increase playback volume by 5%",
                keyCombos = listOf(listOf(modKey, "↑"))
            ),
            ShortcutItem(
                title = "Volume Down",
                description = "Decrease playback volume by 5%",
                keyCombos = listOf(listOf(modKey, "↓"))
            ),
            ShortcutItem(
                title = "Mute / Unmute",
                description = "Toggle mute state for current audio output",
                keyCombos = listOf(listOf("M"))
            )
        )
    ),
    ShortcutCategory(
        categoryName = "Navigation & Interface",
        items = listOf(
            ShortcutItem(
                title = "Search Library",
                description = "Quickly focus the search bar from anywhere",
                keyCombos = listOf(listOf(modKey, "K"))
            ),
            ShortcutItem(
                title = "Toggle Queue Panel",
                description = "Open or collapse the right-side play queue drawer",
                keyCombos = listOf(listOf(modKey, "Q"), listOf("Alt", "Q"))
            ),
            ShortcutItem(
                title = "Toggle Fullscreen",
                description = "Switch between maximized and fullscreen mode",
                keyCombos = listOf(listOf("F11"))
            ),
            ShortcutItem(
                title = "Navigate Back",
                description = "Go back to previous screen or exit modal overlay",
                keyCombos = listOf(listOf("Alt", "←"), listOf("Esc"))
            ),
            ShortcutItem(
                title = "Navigate Forward",
                description = "Go forward in the navigation history",
                keyCombos = listOf(listOf("Alt", "→"))
            ),
            ShortcutItem(
                title = "Toggle Mini Player",
                description = "Switch between full player and compact floating mini player",
                keyCombos = listOf(listOf(modKey, "Shift", "M"))
            ),
            ShortcutItem(
                title = "Keyboard Shortcuts Cheatsheet",
                description = "Show the quick reference dialog anywhere",
                keyCombos = listOf(listOf(modKey, "/"), listOf("F1"))
            ),
            ShortcutItem(
                title = "Zoom In",
                description = "Increase application interface scale",
                keyCombos = listOf(listOf(modKey, "+"))
            ),
            ShortcutItem(
                title = "Zoom Out",
                description = "Decrease application interface scale",
                keyCombos = listOf(listOf(modKey, "-"))
            ),
            ShortcutItem(
                title = "Reset Zoom",
                description = "Reset interface scale to 100%",
                keyCombos = listOf(listOf(modKey, "0"))
            )
        )
    )
)

@Composable
fun KeyCap(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = colors.surfaceContainerHighest,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = colors.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutRow(
    item: ShortcutItem,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = shape,
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item.keyCombos.forEachIndexed { comboIndex, combo ->
                    if (comboIndex > 0) {
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        combo.forEachIndexed { keyIndex, key ->
                            if (keyIndex > 0) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            KeyCap(text = key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutGroup(
    category: ShortcutCategory
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = category.categoryName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            category.items.forEachIndexed { index, item ->
                ShortcutRow(
                    item = item,
                    shape = getGroupShape(index, category.items.size)
                )
            }
        }
    }
}

@Composable
fun KeyboardShortcutsContent(
    modifier: Modifier = Modifier,
    categories: List<ShortcutCategory> = defaultShortcutCategories
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        categories.forEach { category ->
            ShortcutGroup(category = category)
        }
    }
}
