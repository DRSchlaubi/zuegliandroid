package dev.schlaubi.zuegli.android.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val title: String, val message: String)

@Serializable
data class TicketResponse(
    @SerialName("ticket_id")
    val ticketId: String,
    @SerialName("access_token")
    val accessToken: String
)
