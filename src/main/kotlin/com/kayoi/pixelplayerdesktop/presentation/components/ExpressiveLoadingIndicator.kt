package com.kayoi.pixelplayerdesktop.presentation.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 38.dp
) {
    val isMinimized = LocalWindowMinimized.current

    val transition = rememberInfiniteTransition(label = "expressiveLoading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isMinimized) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val path = remember { Path() }
    val pointCount = 16
    val angles = remember {
        FloatArray(pointCount) { i -> (i.toFloat() / pointCount) * 2f * PI.toFloat() }
    }
    val points = remember { Array(pointCount) { Offset.Zero } }

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = (this.size.width.coerceAtMost(this.size.height) / 2f) * 0.88f

        val totalPhases = 4f
        val currentStageProgress = (phase * totalPhases) % totalPhases
        val stageIndex = currentStageProgress.toInt()
        val stageFraction = currentStageProgress - stageIndex

        val rotation = phase * 2f * PI.toFloat()

        for (i in 0 until pointCount) {
            val theta = angles[i]
            val r0 = when (stageIndex) {
                0 -> baseRadius
                1 -> baseRadius * (0.80f + 0.20f * cos(4f * theta))
                2 -> baseRadius * (0.84f + 0.16f * cos(4f * theta + PI.toFloat()))
                else -> baseRadius * (0.82f + 0.18f * sin(3f * theta))
            }
            val nextStage = (stageIndex + 1) % 4
            val r1 = when (nextStage) {
                0 -> baseRadius
                1 -> baseRadius * (0.80f + 0.20f * cos(4f * theta))
                2 -> baseRadius * (0.84f + 0.16f * cos(4f * theta + PI.toFloat()))
                else -> baseRadius * (0.82f + 0.18f * sin(3f * theta))
            }

            val smoothFraction = 0.5f - 0.5f * cos(stageFraction * PI.toFloat())
            val r = r0 + (r1 - r0) * smoothFraction
            val currentAngle = theta + rotation
            points[i] = Offset(
                x = center.x + r * cos(currentAngle),
                y = center.y + r * sin(currentAngle)
            )
        }

        path.rewind()
        path.moveTo(points[0].x, points[0].y)
        for (i in 0 until pointCount) {
            val p0 = points[(i - 1 + pointCount) % pointCount]
            val p1 = points[i]
            val p2 = points[(i + 1) % pointCount]
            val p3 = points[(i + 2) % pointCount]

            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }
        path.close()

        drawPath(path = path, color = color)
    }
}

@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    containerSize: Dp = 48.dp,
    indicatorSize: Dp = 38.dp
) {
    Surface(
        modifier = modifier.size(containerSize),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            ExpressiveLoadingIndicator(
                size = indicatorSize,
                color = indicatorColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    strokeCap: StrokeCap = StrokeCap.Round,
    gapSize: Dp = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        gapSize = gapSize
    )
}
