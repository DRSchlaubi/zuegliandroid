@file:Suppress("ArrayInDataClass", "unused")

package dev.schlaubi.zuegli.android.api

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import dev.schlaubi.zuegli.android.api.serialization.Base64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// Adapted from: https://github.com/TheEnbyperor/golden-gate/blob/root/Golden%20Gate/Golden%20Gate/Messages.swift#L11-L140
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface Message {
    @Serializable
    @SerialName("connected")
    data class Connected(
        val aid: String,
        val identifier: Base64,
        @SerialName("historical-bytes")
        val historicalBytes: Base64,
        @SerialName("application-data")
        val applicationData: Base64
    ): Message

    @Serializable
    @SerialName("error")
    data class Error(val message: String): Message

    @Serializable
    @SerialName("message")
    data class MessagePayload(val message: String): Message

    @Serializable
    @SerialName("request-apdu")
    data class RequestAPDU(
        @SerialName("class")
        val clazz: UByte,
        val instruction: UByte,
        val p1: UByte,
        val p2: UByte,
        val data: Base64,
        @SerialName("expected-response-length")
        val expectedResponseLength: Int
    ) : Message

    @Serializable
    @SerialName("request-apdu-raw")
    data class RequestAPDURaw(val data: Base64) : Message

    @Serializable
    @SerialName("response-apdu")
    data class ResponseAPDU(
        val sw1: UByte,
        val sw2: UByte,
        val data: Base64
    ): Message

    @Serializable
    @SerialName("done")
    data class Done(@SerialName("return-url") val returnUrl: String): Message

    @Serializable
    @SerialName("ndef")
    data class NDEF(
        val identifier: Base64,
        val ndef: List<NDEFMessage>
    ): Message
}

// Adapted from: https://github.com/TheEnbyperor/golden-gate/blob/root/Golden%20Gate/Golden%20Gate/Messages.swift#L142-L176
@Serializable
data class NDEFMessage(
    @SerialName("tnf")
    val typeNameFormat: TypeNameFormat,
    val type: Base64,
    val id: Base64,
    val data: Base64
) {
    @Serializable
    enum class TypeNameFormat(private val magicValue: Short) {
        @SerialName("empty")
        EMPTY(NdefRecord.TNF_EMPTY),

        @SerialName("nfc-wk")
        NFC_WELL_KNOWN(NdefRecord.TNF_WELL_KNOWN),

        @SerialName("mime-type")
        MEDIA(NdefRecord.TNF_MIME_MEDIA),

        @SerialName("uri")
        ABSOLUTE_URI(NdefRecord.TNF_ABSOLUTE_URI),

        @SerialName("nfc-ext")
        EXTERNAL(NdefRecord.TNF_EXTERNAL_TYPE),

        @SerialName("unchanged")
        UNCHANGED(NdefRecord.TNF_UNCHANGED),

        @SerialName("unknown")
        UNKNOWN(NdefRecord.TNF_UNKNOWN);

        companion object {
            fun fromMagicValue(magicValue: Short) =
                entries.firstOrNull { it.magicValue == magicValue } ?: UNKNOWN
        }
    }

    companion object {
        fun fromNdefRecord(record: NdefRecord) = NDEFMessage(
            typeNameFormat = TypeNameFormat.fromMagicValue(record.tnf),
            type = record.type,
            id = record.id,
            data = record.payload
        )

        fun fromNdefMessage(message: NdefMessage) = message.records.map(::fromNdefRecord)
    }
}
