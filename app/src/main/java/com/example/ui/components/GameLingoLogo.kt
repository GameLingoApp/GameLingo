package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BlueAccentDark

@Composable
fun GameLingoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    primaryColor: Color = MaterialTheme.colorScheme.onBackground,
    accentColor: Color = BlueAccentDark
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = w * 0.12f

            // Draw clean 'G' curve
            val gPath = Path().apply {
                // Outer circle arc from 45 deg to 360 deg
                val radius = (w - strokeW) / 2f
                val center = Offset(w / 2f, h / 2f)

                // Arc around
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = strokeW / 2f,
                        top = strokeW / 2f,
                        right = w - strokeW / 2f,
                        bottom = h - strokeW / 2f
                    ),
                    startAngleDegrees = 40f,
                    sweepAngleDegrees = -300f,
                    forceMoveTo = true
                )
            }

            drawPath(
                path = gPath,
                color = primaryColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // G horizontal bar
            val barY = h * 0.52f
            drawLine(
                color = primaryColor,
                start = Offset(w * 0.5f, barY),
                end = Offset(w * 0.88f, barY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Translation Transformation Lines inside (Blue)
            val lineThickness = strokeW * 0.7f
            // Top translation line
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(w * 0.44f, h * 0.38f),
                size = Size(w * 0.36f, lineThickness),
                cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
            )

            // Bottom translation line (slightly offset for transformation effect)
            drawRoundRect(
                color = accentColor.copy(alpha = 0.8f),
                topLeft = Offset(w * 0.36f, h * 0.65f),
                size = Size(w * 0.34f, lineThickness),
                cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
            )
        }
    }
}
