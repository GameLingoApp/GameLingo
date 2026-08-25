package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GoogleLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.22f
        val radius = (w - strokeWidth) / 2f
        val center = Offset(w / 2f, h / 2f)

        // Draw standard 4-color Google G
        // Red arc (top-left to top-right)
        drawArc(
            color = Color(0xFFEA4335), // Red
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = androidx.compose.ui.geometry.Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Yellow arc (left)
        drawArc(
            color = Color(0xFFFBBC05), // Yellow
            startAngle = 120f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = androidx.compose.ui.geometry.Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Green arc (bottom)
        drawArc(
            color = Color(0xFF34A853), // Green
            startAngle = 30f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = androidx.compose.ui.geometry.Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Blue bar + arc (right)
        drawArc(
            color = Color(0xFF4285F4), // Blue
            startAngle = 310f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = androidx.compose.ui.geometry.Size(w - strokeWidth, h - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Blue horizontal crossbar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(w - strokeWidth / 2f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String = "Continue with Google"
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFFFF),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Авторизация...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            } else {
                GoogleLogoIcon(size = 22.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
