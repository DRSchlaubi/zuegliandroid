package dev.schlaubi.zuegli.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import io.ktor.http.Url

class RemoteNFCViewModel(private val server: String) : ViewModel() {

}

class RemoteNFCReaderActivity : ZuegliContentActivity() {

    @Composable
    override fun Content() {
        val server = remember(intent) {
            val uri = intent.data?.toString() ?: return@remember null
            val url = Url(uri)

            url.parameters["server"]
        }

        Scaffold(modifier = Modifier.fillMaxSize()) {
            val modifier = Modifier.padding(it)
            if (server == null) {
                InvalidServerError(modifier)
            } else {
                ServerSession(server, modifier)
            }
        }
    }
}

@Composable
private fun InvalidServerError(modifier: Modifier = Modifier) =
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.error_uri_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.error_uri_description))
    }

@Composable
private fun ServerSession(server: String, modifier: Modifier = Modifier) = Column(modifier) {
    Text(server)
}