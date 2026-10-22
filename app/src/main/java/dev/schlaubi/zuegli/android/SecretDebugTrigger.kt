package dev.schlaubi.zuegli.android

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SecretDebugTrigger(
    modifier: Modifier = Modifier,
    requiredTaps: Int = 5,
    timeoutMs: Long = 2_000,
    onTriggered: () -> Unit,
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .size(64.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTapTime > timeoutMs) 1 else tapCount + 1
                    lastTapTime = now

                    if (tapCount >= requiredTaps) {
                        tapCount = 0
                        onTriggered()
                    }
                }
            })
}