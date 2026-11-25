package dev.schlaubi.zuegli.android.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.jsonIo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.IDN
import kotlin.time.Duration.Companion.seconds

object APIClient {
    @OptIn(ExperimentalSerializationApi::class)
    private val http = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingInterval = 30.seconds
        }
        install(ContentNegotiation) {
            jsonIo()
        }
    }

    suspend fun connect(url: Url): DefaultClientWebSocketSession {
        return http.webSocketSession {
            this.url.takeFrom(url)

            header(HttpHeaders.Origin, "${url.protocol.name}://${IDN.toASCII(url.host)}")
        }
    }

    suspend fun uploadImage(file: ByteArray, contentType: String) =
        http.submitFormWithBinaryData(
            url = "https://zügli.app/api/upload_image",
            formData = formData {
                append("img", file, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"image.pnf\"")
                })
            }) {
            expectSuccess = false
        }
}