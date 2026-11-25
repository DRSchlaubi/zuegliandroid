package dev.schlaubi.zuegli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import dev.schlaubi.zuegli.android.ui.theme.ZuegliTheme

abstract class ZuegliContentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZuegliTheme { Content() }
        }
    }

    @Composable
    abstract fun Content()
}