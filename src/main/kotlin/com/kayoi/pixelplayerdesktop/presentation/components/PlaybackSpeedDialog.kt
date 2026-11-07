package com.kayoi.pixelplayerdesktop.presentation.components

import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_bookmark
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_restart_alt
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speed

private val PRESET_SPEEDS = listOf(0.75f, 0.9f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    isSpeedSavedForThisSong: Boolean,
    onSpeedChange: (Float) -> Unit,
    onSpeedChangeFinished: ((Float) -> Unit)? = null,
    onResetSpeed: () -> Unit,
    onRememberSpeedToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    TrackDialogInput()

    val colors = MaterialTheme.colorScheme
    var sliderValue by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(480.dp)
                .heightIn(max = 640.dp)
                .padding(16.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (currentSpeed != 1.0f) colors.primary else colors.surfaceContainerHighest,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_speed),
                                    contentDescription = null,
                                    tint = if (currentSpeed != 1.0f) colors.onPrimary else colors.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Playback Speed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            val statusText = when {
                                isSpeedSavedForThisSong -> "Saved for this song: ${String.format(Locale.US, "%.2f", currentSpeed)}x"
                                currentSpeed != 1.0f -> "Active: ${String.format(Locale.US, "%.2f", currentSpeed)}x"
                                else -> "Normal (1.00x)"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentSpeed != 1.0f) colors.primary else colors.onSurfaceVariant
                            )
                        }
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

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tempo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", sliderValue)}x",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (sliderValue != 1.0f) colors.primary else colors.onSurfaceVariant
                                    )
                                }

                                if (sliderValue != 1.0f || isSpeedSavedForThisSong) {
                                    TextButton(
                                        onClick = {
                                            sliderValue = 1.0f
                                            onResetSpeed()
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_restart_alt),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reset")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PRESET_SPEEDS.forEach { speed ->
                                    val isSelected = (sliderValue * 100).roundToInt() == (speed * 100).roundToInt()
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            sliderValue = speed
                                            onSpeedChange(speed)
                                            onSpeedChangeFinished?.invoke(speed)
                                        },
                                        label = { Text("${speed}x") },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_check),
                                                    contentDescription = null,
                                                    tint = colors.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.primary,
                                            selectedLabelColor = colors.onPrimary,
                                            selectedLeadingIconColor = colors.onPrimary,
                                            containerColor = colors.surfaceContainerHigh,
                                            labelColor = colors.onSurface
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Slider(
                                value = sliderValue,
                                onValueChange = { newValue ->
                                    val rounded = (newValue * 20).roundToInt() / 20f
                                    sliderValue = rounded
                                    onSpeedChange(rounded)
                                },
                                onValueChangeFinished = {
                                    onSpeedChangeFinished?.invoke(sliderValue)
                                },
                                valueRange = 0.5f..2.0f,
                                steps = 29,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.primary,
                                    activeTrackColor = colors.primary,
                                    inactiveTrackColor = colors.surfaceContainerHighest
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0.5x", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Text("1.0x", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Text("1.5x", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Text("2.0x", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }

                    ExpressiveSwitchRow(
                        title = "Save for this song only",
                        subtitle = "Remember this speed whenever this track plays",
                        checked = isSpeedSavedForThisSong,
                        onCheckedChange = onRememberSpeedToggle,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_bookmark),
                                contentDescription = null,
                                tint = if (isSpeedSavedForThisSong) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}
