package dev.schlaubi.zuegli.android.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias HexString = @Serializable(with = HexStringSerializer::class) ByteArray


class HexStringSerializer : KSerializer<ByteArray> {
    private val format: HexFormat = HexFormat.Default

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "dev.schlaubi.zuegli.android.api.serialization.HexString",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: ByteArray) =
        encoder.encodeString(value.toHexString(format))

    override fun deserialize(decoder: Decoder): ByteArray =
        decoder.decodeString().hexToByteArray(format)
}