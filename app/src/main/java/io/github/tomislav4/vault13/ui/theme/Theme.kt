package io.github.tomislav4.vault13.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MatrixColorScheme = darkColorScheme(
    primary = MatrixGreen,
    secondary = MatrixDarkGreen,
    tertiary = MatrixTextGreen,
    background = MatrixBlack,
    surface = MatrixBlack,
    onPrimary = MatrixBlack,
    onSecondary = MatrixGreen,
    onTertiary = MatrixBlack,
    onBackground = MatrixGreen,
    onSurface = MatrixGreen
)

@Composable
fun Vault13Theme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            @Suppress("DEPRECATION")
            window.statusBarColor = MatrixBlack.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = MatrixBlack.toArgb()

            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = MatrixColorScheme,
        typography = Typography,
        content = content
    )
}
