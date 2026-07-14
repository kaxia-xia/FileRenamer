package com.filerenamer.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LightColorScheme = lightColorScheme(
    primary = GlassLightPrimary,
    onPrimary = GlassLightOnPrimary,
    primaryContainer = GlassLightPrimaryContainer,
    onPrimaryContainer = GlassLightOnPrimaryContainer,
    secondary = GlassLightSecondary,
    onSecondary = GlassLightOnSecondary,
    secondaryContainer = GlassLightSecondaryContainer,
    onSecondaryContainer = GlassLightOnSecondaryContainer,
    surface = GlassLightSurface,
    onSurface = GlassLightOnSurface,
    surfaceVariant = GlassLightSurfaceVariant,
    onSurfaceVariant = GlassLightOnSurfaceVariant,
    background = GlassLightBackground,
    onBackground = GlassLightOnBackground,
    outline = GlassLightOutline,
    outlineVariant = GlassLightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = GlassDarkPrimary,
    onPrimary = GlassDarkOnPrimary,
    primaryContainer = GlassDarkPrimaryContainer,
    onPrimaryContainer = GlassDarkOnPrimaryContainer,
    secondary = GlassDarkSecondary,
    onSecondary = GlassDarkOnSecondary,
    secondaryContainer = GlassDarkSecondaryContainer,
    onSecondaryContainer = GlassDarkOnSecondaryContainer,
    surface = GlassDarkSurface,
    onSurface = GlassDarkOnSurface,
    surfaceVariant = GlassDarkSurfaceVariant,
    onSurfaceVariant = GlassDarkOnSurfaceVariant,
    background = GlassDarkBackground,
    onBackground = GlassDarkOnBackground,
    outline = GlassDarkOutline,
    outlineVariant = GlassDarkOutlineVariant,
)

/**
 * 从壁纸中提取主色调
 */
suspend fun extractWallpaperColor(context: Context): Color? {
    return withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.drawable
            } else {
                @Suppress("DEPRECATION")
                wallpaperManager.getDrawable()
            } ?: return@withContext null

            val bitmap = Bitmap.createBitmap(
                wallpaperDrawable.intrinsicWidth.coerceAtLeast(64),
                wallpaperDrawable.intrinsicHeight.coerceAtLeast(64),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            wallpaperDrawable.setBounds(0, 0, canvas.width, canvas.height)
            wallpaperDrawable.draw(canvas)

            val palette = Palette.from(bitmap).generate()
            val dominantSwatch = palette.dominantSwatch
            dominantSwatch?.let {
                Color(it.rgb)
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 根据壁纸颜色生成调整后的主题色
 */
fun adjustColorForTheme(wallpaperColor: Color?, isDark: Boolean): Color {
    val baseColor = wallpaperColor ?: if (isDark) DefaultWallpaperDark else DefaultWallpaperLight
    val alpha = if (isDark) 0.85f else 0.9f
    return Color(
        red = baseColor.red * alpha + (if (isDark) 0.1f else 0.05f),
        green = baseColor.green * alpha + (if (isDark) 0.1f else 0.05f),
        blue = baseColor.blue * alpha + (if (isDark) 0.15f else 0.05f),
        alpha = 1f
    )
}

/**
 * 生成液态玻璃主题色系
 */
fun generateGlassColorScheme(baseColor: Color, isDark: Boolean): Pair<Color, Color> {
    val primary = baseColor
    val container = if (isDark) {
        Color(
            red = primary.red * 0.3f + 0.1f,
            green = primary.green * 0.3f + 0.1f,
            blue = primary.blue * 0.3f + 0.15f,
            alpha = 0.3f
        )
    } else {
        Color(
            red = primary.red * 0.85f + 0.1f,
            green = primary.green * 0.85f + 0.1f,
            blue = primary.blue * 0.85f + 0.1f,
            alpha = 0.25f
        )
    }
    return Pair(primary, container)
}

@Composable
fun FileRenamerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    wallpaperColor: Color? = null,
    content: @Composable () -> Unit
) {
    val adjustedColor = remember(wallpaperColor, darkTheme) {
        adjustColorForTheme(wallpaperColor, darkTheme)
    }
    val (primary, container) = remember(adjustedColor, darkTheme) {
        generateGlassColorScheme(adjustedColor, darkTheme)
    }

    val colorScheme = if (darkTheme) {
        DarkColorScheme.copy(
            primary = primary,
            primaryContainer = container,
            surface = Color(0xFF1A1A2E),
            background = Color(0xFF0F0F1A),
        )
    } else {
        LightColorScheme.copy(
            primary = primary,
            primaryContainer = container,
            surface = Color(0xFFF8F4FF),
            background = Color(0xFFF0ECF9),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
