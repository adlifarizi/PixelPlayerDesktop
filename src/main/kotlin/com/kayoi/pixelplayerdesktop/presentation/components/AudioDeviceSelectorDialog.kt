package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.domain.player.AudioDevice
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_headphones
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speaker
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceSelectorDialog(
    selectedDevice: String,
    availableDevices: List<AudioDevice>,
    onSelectDevice: (String) -> Unit,
    onRefreshDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    TrackDialogInput()

    LaunchedEffect(Unit) {
        onRefreshDevices()
    }

    val displayDevices = remember(availableDevices) {
        val hasAuto = availableDevices.any { it.name == "auto" }
        if (hasAuto) {
            availableDevices
        } else {
            listOf(AudioDevice(name = "auto", description = "Autoselect device (System Default)")) + availableDevices
        }
    }

    val colors = MaterialTheme.colorScheme

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(440.dp)
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_speaker),
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Audio Output Device",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Select active playback output",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    AppTooltip(text = "Refresh devices") {
                        IconButton(
                            onClick = onRefreshDevices,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_refresh),
                                contentDescription = "Refresh devices",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayDevices, key = { it.name }) { device ->
                        val isSelected = (selectedDevice.isBlank() && device.name == "auto") || selectedDevice == device.name
                        val isHeadphone = device.description.contains("headphone", ignoreCase = true) ||
                            device.description.contains("headset", ignoreCase = true)
                        val icon = if (isHeadphone) Res.drawable.ic_rounded_headphones else Res.drawable.ic_rounded_speaker

                        val itemBg = if (isSelected) colors.primaryContainer.copy(alpha = 0.65f) else colors.surfaceContainer
                        val contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(itemBg)
                                .clickable {
                                    onSelectDevice(device.name)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.description.ifBlank { device.name },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (device.name != "auto" && device.name != device.description) {
                                    val driverTag = when {
                                        device.name.startsWith("pipewire") -> "PipeWire"
                                        device.name.startsWith("pulse") -> "PulseAudio"
                                        device.name.startsWith("alsa") -> "ALSA Hardware"
                                        device.name.startsWith("wasapi") -> "WASAPI"
                                        device.name.startsWith("coreaudio") -> "CoreAudio"
                                        device.name.startsWith("avfoundation") -> "AVFoundation"
                                        else -> null
                                    }
                                    val subtitleText = if (driverTag != null) "$driverTag • ${device.name}" else device.name
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isSelected) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_check),
                                    contentDescription = "Selected",
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
