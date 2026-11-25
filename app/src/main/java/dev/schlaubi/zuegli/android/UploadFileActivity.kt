package dev.schlaubi.zuegli.android

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import dev.schlaubi.zuegli.android.api.APIClient
import dev.schlaubi.zuegli.android.api.ErrorResponse
import dev.schlaubi.zuegli.android.api.TicketResponse
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.ensureActive

private class File(val bytes: ByteArray, val contentType: String)

private sealed interface ShareState {
    data object Initial : ShareState
    data class ReadFile(val file: File) : ShareState
    data class Error(val response: ErrorResponse) : ShareState
    data class Success(val response: TicketResponse) : ShareState
}

class UploadFileActivity : ZuegliContentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun Content() {
        var state by remember { mutableStateOf<ShareState>(ShareState.Initial) }
        val currentState = state
        val uriHandler = LocalUriHandler.current

        if (currentState is ShareState.Initial) {
            LaunchedEffect(intent) {
                val uri =
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?: return@LaunchedEffect

                val bytes = contentResolver.openInputStream(uri)?.use { it.buffered().readBytes() }
                    ?: return@LaunchedEffect
                state = ShareState.ReadFile(File(bytes, intent.type ?: "image/png"))
            }
        }

        if (currentState is ShareState.ReadFile) {
            LaunchedEffect(currentState) {
                val currentFile = currentState.file
                val response = try {
                    APIClient.uploadImage(currentFile.bytes, currentFile.contentType)
                } catch (_: CancellationException) {
                    ensureActive()
                    return@LaunchedEffect
                } catch (e: Exception) {
                    state = ShareState.Error(
                        ErrorResponse(
                            resources.getString(R.string.error_generic),
                            e.message.toString()
                        )
                    )
                    return@LaunchedEffect
                }

                if (response.status.isSuccess()) {
                    val ticket = response.body<TicketResponse>()
                    state = ShareState.Success(ticket)
                } else {
                    val error = response.body<ErrorResponse>()
                    state = ShareState.Error(error)
                }
            }
        }


        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentState) {
                is ShareState.Initial, is ShareState.ReadFile -> {
                    LoadingIndicator()
                    Text(stringResource(R.string.uploading_image))
                }

                is ShareState.Error -> {
                    Text(currentState.response.title, style = MaterialTheme.typography.titleMedium)
                    Text(currentState.response.message)
                }

                is ShareState.Success -> {
                    Text(
                        stringResource(R.string.pass_ready_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button({ uriHandler.openUri("https://zügli.app/ticket/${currentState.response.ticketId}") }) {
                        Text(stringResource(R.string.open_pass))
                    }
                }
            }
        }
    }
}