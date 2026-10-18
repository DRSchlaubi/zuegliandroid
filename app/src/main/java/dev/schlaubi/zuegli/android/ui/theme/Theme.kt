package dev.schlaubi.zuegli.android.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun rememberDynamicColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
        context
    )
}

@Composable
private fun rememberStaticColorScheme() =
    if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

@Composable
private fun rememberColorScheme() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    rememberDynamicColorScheme()
} else {
    rememberStaticColorScheme()
}

@Composable
fun ZuegliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = rememberColorScheme(),
        content = content
    )
}