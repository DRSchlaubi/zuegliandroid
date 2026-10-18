package dev.schlaubi.zuegli.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.schlaubi.zuegli.android.ui.theme.ZuegliTheme

class MainActivity : ZuegliContentActivity() {
    @Composable
    override fun Content() = HomeView()
}

@Composable
private fun HomeView() = ZuegliTheme {
    Scaffold(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.home_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Preview
@Composable
fun HomeViewPreview() {
    HomeView()
}
