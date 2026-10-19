package dev.schlaubi.zuegli.android.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.hostWithPortIfSpecified
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.IDN
import kotlin.time.Duration.Companion.seconds

object APIClient {
    private val http = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingInterval = 30.seconds
        }
    }

    suspend fun connect(url: String): DefaultClientWebSocketSession {
        val url = Url(url)
        return http.webSocketSession {
            this.url.takeFrom(url)

            header(HttpHeaders.Origin, "${url.protocol.name}://${IDN.toASCII(url.host)}")
        }
    }
}