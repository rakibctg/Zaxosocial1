package com.zaxo.ui.neumorphic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Color palette for Neumorphic styling
object NeuTheme {
    val LightBg = Color(0xFFFEF7FF)
    val LightLightShadow = Color(0xFFFFFFFF)
    val LightDarkShadow = Color(0xFFE4DBE6)

    val DarkBg = Color(0xFF141218)
    val DarkLightShadow = Color(0xFF232128)
    val DarkDarkShadow = Color(0xFF0A090B)

    val AccentColor = Color(0xFF6750A4)
    val AccentLight = Color(0xFFEADDFF)

    @Composable
    fun getBgColor(darkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (darkTheme) DarkBg else LightBg
    }

    @Composable
    fun getLightShadow(darkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (darkTheme) DarkLightShadow else LightLightShadow
    }

    @Composable
    fun getDarkShadow(darkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (darkTheme) DarkDarkShadow else LightDarkShadow
    }

    @Composable
    fun getTextColor(darkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (darkTheme) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    }

    @Composable
    fun getSecondaryTextColor(darkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (darkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)
    }
}

// Custom modifier for Neumorphic outer (extruded) shadows
fun Modifier.neuShadow(
    cornerRadius: Dp = 16.dp,
    offset: Dp = 6.dp,
    blurRadius: Dp = 8.dp,
    isSunken: Boolean = false,
    darkTheme: Boolean = false
): Modifier = this.drawBehind {
    val lightShadowColor = if (darkTheme) NeuTheme.DarkLightShadow else NeuTheme.LightLightShadow
    val darkShadowColor = if (darkTheme) NeuTheme.DarkDarkShadow else NeuTheme.LightDarkShadow

    val shadowPaintLight = Paint().apply {
        color = lightShadowColor
        asFrameworkPaint().apply {
            maskFilter = android.graphics.BlurMaskFilter(
                blurRadius.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
    }

    val shadowPaintDark = Paint().apply {
        color = darkShadowColor
        asFrameworkPaint().apply {
            maskFilter = android.graphics.BlurMaskFilter(
                blurRadius.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
    }

    drawIntoCanvas { canvas ->
        val sizePx = size
        val offsetPx = offset.toPx()
        val rPx = cornerRadius.toPx()

        if (!isSunken) {
            // Extruded look
            // Top-left light shadow
            canvas.save()
            canvas.drawRoundRect(
                left = -offsetPx,
                top = -offsetPx,
                right = sizePx.width - offsetPx,
                bottom = sizePx.height - offsetPx,
                radiusX = rPx,
                radiusY = rPx,
                paint = shadowPaintLight
            )
            canvas.restore()

            // Bottom-right dark shadow
            canvas.save()
            canvas.drawRoundRect(
                left = offsetPx,
                top = offsetPx,
                right = sizePx.width + offsetPx,
                bottom = sizePx.height + offsetPx,
                radiusX = rPx,
                radiusY = rPx,
                paint = shadowPaintDark
            )
            canvas.restore()
        } else {
            // Sunken (inset) shadow look - simplified via inner dark/light accents
            canvas.save()
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = sizePx.width,
                bottom = sizePx.height,
                radiusX = rPx,
                radiusY = rPx,
                paint = Paint().apply {
                    color = if (darkTheme) Color(0xFF1A1721) else Color(0xFFF3EDF7)
                }
            )
            canvas.restore()
        }
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isSunken: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg

    Box(
        modifier = modifier
            .neuShadow(cornerRadius, isSunken = isSunken, darkTheme = darkTheme)
            .background(bgColor, RoundedCornerShape(cornerRadius))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    darkTheme: Boolean = isSystemInDarkTheme(),
    testTag: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .neuShadow(cornerRadius, isSunken = isPressed, darkTheme = darkTheme)
            .background(bgColor, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun NeumorphicIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    isPressed: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg

    Box(
        modifier = modifier
            .neuShadow(cornerRadius, isSunken = isPressed, darkTheme = darkTheme)
            .background(bgColor, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val trackBg = if (checked) NeuTheme.AccentColor else (if (darkTheme) NeuTheme.DarkLightShadow else NeuTheme.LightDarkShadow)
    val thumbOffset by animateFloatAsState(targetValue = if (checked) 24f else 0f, label = "thumb")

    Box(
        modifier = modifier
            .width(50.dp)
            .height(26.dp)
            .background(trackBg, RoundedCornerShape(13.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(20.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
        )
    }
}
