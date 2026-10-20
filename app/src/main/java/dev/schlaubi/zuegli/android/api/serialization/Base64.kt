package dev.schlaubi.zuegli.android.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64

typealias Base64 = @Serializable(with = Base64Serializer::class) ByteArray

object Base64Serializer : KSerializer<ByteArray> {
    private val base64 = Base64.Default

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "dev.schlaubi.zuegli.android.api.serialization.Base64",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: ByteArray) = encoder.encodeString(base64.encode(value))
    override fun deserialize(decoder: Decoder): ByteArray = base64.decode(decoder.decodeString())
}
