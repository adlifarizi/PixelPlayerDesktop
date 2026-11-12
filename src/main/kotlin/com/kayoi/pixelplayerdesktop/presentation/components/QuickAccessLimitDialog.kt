package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_push_pin

@Composable
fun QuickAccessLimitDialog(
    onDismiss: () -> Unit,
    onManageQuickAccess: () -> Unit,
    maxLimit: Int = QuickAccessItem.MAX_PINNED_CUSTOM_ITEMS
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_push_pin),
                contentDescription = null,
                tint = colors.primary
            )
        },
        title = {
            Text(
                text = "Quick Access Limit Reached",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "You can pin up to $maxLimit custom items to Quick Access. Unpin an existing item or manage your pinned items to add more."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onManageQuickAccess()
                }
            ) {
                Text("Manage Quick Access")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
