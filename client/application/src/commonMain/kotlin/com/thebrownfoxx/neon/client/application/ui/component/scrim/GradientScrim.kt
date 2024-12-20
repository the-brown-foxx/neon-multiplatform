package com.thebrownfoxx.neon.client.application.ui.component.scrim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class GradientDirection {
    StartEnd,
    TopBottom,
    EndStart,
    BottomTop,
}

@Composable
fun GradientScrim(
    direction: GradientDirection,
    color: Color,
    modifier: Modifier,
    maxAlpha: Float = 1f,
    threshold: Float = 0.5f,
    thresholdAlpha: Float = maxAlpha / 2,
) {
    val brush = gradientScrimBrush(
        direction = direction,
        color = color,
        maxAlpha = maxAlpha,
        threshold = threshold,
        thresholdAlpha = thresholdAlpha,
    )

    Box(modifier = modifier.background(brush = brush))
}

@Composable
fun gradientScrimBrush(
    direction: GradientDirection,
    color: Color,
    maxAlpha: Float = 1f,
    threshold: Float = 0.5f,
    thresholdAlpha: Float = maxAlpha / 2,
): Brush {
    val maxAlphaColor = color.copy(alpha = color.alpha * maxAlpha)
    val thresholdColor = color.copy(alpha = color.alpha * thresholdAlpha)

    return when (direction) {
        GradientDirection.StartEnd -> Brush.horizontalGradient(
            0f to maxAlphaColor,
            threshold to thresholdColor,
            1f to Color.Transparent,
        )

        GradientDirection.TopBottom -> Brush.verticalGradient(
            0f to maxAlphaColor,
            threshold to thresholdColor,
            1f to Color.Transparent,
        )

        GradientDirection.EndStart -> Brush.horizontalGradient(
            0f to Color.Transparent,
            1f - threshold to thresholdColor,
            1f to maxAlphaColor,
        )

        GradientDirection.BottomTop -> Brush.verticalGradient(
            0f to Color.Transparent,
            1f - threshold to thresholdColor,
            1f to maxAlphaColor,
        )
    }
}