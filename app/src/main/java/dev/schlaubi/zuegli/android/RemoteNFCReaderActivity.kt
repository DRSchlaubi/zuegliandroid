package dev.schlaubi.zuegli.android

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcB
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.schlaubi.zuegli.android.api.APIClient
import dev.schlaubi.zuegli.android.api.Message
import dev.schlaubi.zuegli.android.api.NDEFMessage
import dev.schlaubi.zuegli.android.nfc.APDUResponse
import dev.schlaubi.zuegli.android.nfc.getAID
import dev.schlaubi.zuegli.android.nfc.toByteArray
import dev.schlaubi.zuegli.android.ui.theme.icons.OpenInBrowser
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.http.Url
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed interface State {
    sealed interface Error : State {
        @get:Composable
        val title: String

        @get:Composable
        val description: String
    }

    data object Dead : State
    data class DynamicError(private val _title: String, private val _description: String) : Error {
        @get:Composable
        override val title: String get() = _title

        @get:Composable
        override val description: String get() = _description

    }

    data class GenericError(@StringRes val titleRes: Int, @StringRes val descriptionRes: Int) :
        Error {
        override val title: String
            @Composable
            get() = stringResource(titleRes)
        override val description: String
            @Composable
            get() = stringResource(descriptionRes)
    }

    data class Connecting(val serverUrl: String) : State

    data class Connected(val session: DefaultClientWebSocketSession) : State, CoroutineScope,
        NfcAdapter.ReaderCallback {
        data class ReaderState(
            val waitingForCard: Boolean = true,
            val tagIsInvalid: Boolean = false,
            val parsingTag: Boolean = false,
            val isoTag: IsoDep? = null,
            val error: String? = null,
            val infoMessage: String? = null,
            val passUrl: String? = null
        )

        private val _uiState = MutableStateFlow(ReaderState())
        val uiState = _uiState.asStateFlow()

        override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

        init {
            launch {
                try {
                    Log.d("ZUEGLI", "started")
                    while (isActive) {
                        Log.d("ZUEGLI", "running")
                        val receiveDeserialized = session.receiveDeserialized<Message>()
                        Log.d("ZUEGLI", "Got event: $receiveDeserialized")
                        when (receiveDeserialized) {
                            is Message.Error -> handleInvalidTag(receiveDeserialized.message)
                            is Message.MessagePayload -> _uiState.tryEmit(
                                _uiState.value.copy(
                                    infoMessage = receiveDeserialized.message
                                )
                            )

                            is Message.Done -> {
                                session.close()
                                _uiState.tryEmit(
                                    _uiState.value.copy(
                                        passUrl = receiveDeserialized.returnUrl,
                                        parsingTag = false
                                    )
                                )
                            }

                            is Message.RequestAPDURaw -> requestADPU(receiveDeserialized)
                            is Message.RequestAPDU -> requestADPU(receiveDeserialized)

                            else -> {}
                        }
                    }
                    Log.d("ZUEGLI", "stopped")

                } catch (_: ClosedReceiveChannelException) {
                    // close
                }
            }
        }

        private fun handleInvalidTag(error: String? = null) {
            _uiState.tryEmit(
                _uiState.value.copy(
                    waitingForCard = false,
                    parsingTag = false,
                    tagIsInvalid = true,
                    error = error
                )
            )
            launch {
                session.close()
                cancel()
            }
        }

        override fun onTagDiscovered(tag: Tag?) {
            if (tag == null) return handleInvalidTag()
            Log.d(
                "ZUEGLI",
                "Discovered NFC tag with following techs: ${tag.techList.joinToString(", ")}"
            )
            _uiState.tryEmit(
                _uiState.value.copy(
                    waitingForCard = false,
                    parsingTag = true
                )
            )
            if (IsoDep::class.qualifiedName in tag.techList) return handleIsoTag(tag)
            if (MifareUltralight::class.qualifiedName in tag.techList) return handleMifare(tag)
            handleInvalidTag()
        }

        private fun handleIsoTag(tag: Tag) {
            launch(Dispatchers.IO) {
                val iso = IsoDep.get(tag) ?: return@launch handleInvalidTag()
                iso.connect()
                _uiState.tryEmit(_uiState.value.copy(isoTag = iso))
                val nfc = NfcB.get(tag)
                val aid = iso.getAID() ?: return@launch handleInvalidTag()

                sendMessage(
                    Message.Connected(
                        aid,
                        tag.id,
                        iso.historicalBytes ?: byteArrayOf(),
                        nfc?.applicationData ?: byteArrayOf()
                    )
                )
            }
        }

        private fun handleMifare(tag: Tag) {
            launch(Dispatchers.IO) {
                val ndef = Ndef.get(tag) ?: return@launch handleInvalidTag()
                ndef.connect()
                val ndefMessage = ndef.cachedNdefMessage ?: return@launch handleInvalidTag()
                val apiNdefMessage = NDEFMessage.fromNdefMessage(ndefMessage)

                sendMessage(Message.NDEF(tag.id, apiNdefMessage))
            }
        }

        private suspend fun sendMessage(message: Message) {
            Log.d("ZUEGLI", "Sending WS message: $message")
            session.sendSerialized(message)
        }

        private fun requestADPU(request: Message.RequestAPDU) =
            requestADPURaw(request.toByteArray())

        private fun requestADPU(request: Message.RequestAPDURaw) = requestADPURaw(request.data)
        private fun requestADPURaw(raw: ByteArray) {
            val tag = _uiState.value.isoTag ?: return handleInvalidTag()
            launch(Dispatchers.IO) {
                val raw = tag.transceive(raw)

                val response = APDUResponse.parse(raw)

                sendMessage(response.toAPDUResponse())
            }
        }
    }
}

class RemoteNFCViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<State>(State.Dead)
    val state = _uiState.asStateFlow()

    suspend fun consumeIntent(intent: Intent) {
        val url = intent.data?.toString()?.let(::Url)
        val server = url?.parameters["server"]

        if (server == null) {
            _uiState.tryEmit(
                State.GenericError(
                    R.string.error_uri_title,
                    R.string.error_uri_description
                )
            )
            return
        }
        (_uiState.value as? State.Connected)?.session?.close()
        val session = try {
            APIClient.connect(server)
        } catch (e: Exception) {
            Log.e("ZUEGLI", "Could not connect", e)
            _uiState.tryEmit(State.DynamicError("Errohr", e.message.toString()))
            return
        }
        _uiState.tryEmit(State.Connected(session))
    }
}

class RemoteNFCReaderActivity : ZuegliContentActivity() {

    @Composable
    override fun Content() {
        val viewModel = viewModel { RemoteNFCViewModel() }
        val state by viewModel.state.collectAsState()

        LaunchedEffect(intent) {
            viewModel.consumeIntent(intent)
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxSize()
        ) {
            val modifier = Modifier.padding(it)
            when (val state = state) {
                is State.Error -> Error(state.title, state.description, modifier = modifier)
                is State.Connecting -> Connecting(modifier)
                is State.Connected -> NFCSession(this, state, modifier = modifier)
                else -> {}
            }
        }
    }
}

@Composable
private fun Connecting(modifier: Modifier = Modifier) = Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxSize()
) {
    CircularProgressIndicator()
    Text(stringResource(R.string.connecting))
}

@Composable
private fun Error(title: String, description: String, modifier: Modifier = Modifier) =
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description)
    }

@Composable
private fun NFCSession(
    activity: Activity,
    state: State.Connected,
    modifier: Modifier = Modifier
) = Column(modifier) {
    val adapter = remember(activity) { NfcAdapter.getDefaultAdapter(activity) }
    val readerState by state.uiState.collectAsState()

    DisposableEffect(state) {
        adapter.enableReaderMode(
            activity, state,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B, null
        )
        onDispose { state.cancel(CancellationException("Composable left composition")) }
    }

    NFCSessionUI(readerState)
}

@Composable
private fun NFCSessionUI(readerState: State.Connected.ReaderState) {
    Text(readerState.toString())
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        val passUrl = readerState.passUrl
        if (passUrl != null) {
            val urlHandler = LocalUriHandler.current
            SideEffect { urlHandler.openUri(passUrl) }

            Text(stringResource(R.string.pass_ready_title))
            Button({ urlHandler.openUri(passUrl) }) {
                Icon(OpenInBrowser, null)
                Text(stringResource(R.string.open_pass))
            }
        } else if (readerState.parsingTag) {
            Text(
                stringResource(R.string.reading_card),
                style = MaterialTheme.typography.titleMedium
            )
            CircularProgressIndicator()
            val message = readerState.infoMessage ?: return@Column
            Text(message)
        } else if (readerState.tagIsInvalid) {
            Text(stringResource(R.string.invalid_tag), style = MaterialTheme.typography.titleMedium)
            val errorMessage = readerState.error ?: return@Column
            Text(errorMessage)
        } else if (readerState.waitingForCard) {
            Text(
                stringResource(R.string.waiting_for_tag),
                style = MaterialTheme.typography.titleMedium
            )
            CircularProgressIndicator()
        }
    }
}

@Composable
@Preview
fun ErrorPreview() {
    NFCSessionUI(
        State.Connected.ReaderState(
            waitingForCard = false,
            tagIsInvalid = true,
            error = "Invalid message received"
        )
    )
}

@Composable
@Preview
fun Scanning() {
    NFCSessionUI(
        State.Connected.ReaderState(
            waitingForCard = false,
            parsingTag = true,
            infoMessage = "Reading smart card"
        )
    )
}