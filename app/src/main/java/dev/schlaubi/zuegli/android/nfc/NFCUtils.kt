package dev.schlaubi.zuegli.android.nfc

import android.nfc.tech.IsoDep
import dev.schlaubi.zuegli.android.api.Message

val selectableAids = listOf("D2760001354B414E4D303100".hexToByteArray())

fun Message.RequestAPDU.toByteArray() =
    buildApdu(clazz, instruction, p1, p2, data, expectedResponseLength)

fun buildApdu(
    clazz: UByte, instruction: UByte, p1: UByte, p2: UByte,
    data: ByteArray?, expectedResponseLength: Int
): ByteArray {
    val body = data ?: ByteArray(0)
    val hasLe = expectedResponseLength >= 0
    val extended = body.size > 255 || expectedResponseLength > 256

    val out = java.io.ByteArrayOutputStream()
    out.write(clazz.toInt()); out.write(instruction.toInt())
    out.write(p1.toInt()); out.write(p2.toInt())

    if (body.isNotEmpty()) {
        if (extended) {
            out.write(0x00)
            out.write(body.size shr 8); out.write(body.size and 0xFF)
        } else {
            out.write(body.size)
        }
        out.write(body)
    }

    if (hasLe) {
        if (extended) {
            if (body.isEmpty()) out.write(0x00)
            val le = expectedResponseLength and 0xFFFF
            out.write(le shr 8); out.write(le and 0xFF)
        } else {
            out.write(expectedResponseLength and 0xFF)
        }
    }
    return out.toByteArray()
}

@Suppress("ArrayInDataClass")
data class APDUResponse(
    val sw1: UByte,
    val sw2: UByte,
    val data: ByteArray
) {
    fun toAPDUResponse() = Message.ResponseAPDU(sw1, sw2, data)

    companion object {
        fun parse(raw: ByteArray): APDUResponse {
            val sw1 = raw[raw.size - 2].toUByte()
            val sw2 = raw[raw.size - 1].toUByte()
            val response = raw.copyOfRange(0, raw.size - 2)

            return APDUResponse(sw1, sw2, response)
        }
    }
}

fun IsoDep.getAID() = selectableAids.firstOrNull { aid ->
    val selectCommand =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte(), *aid, 0x00)
    val response = runCatching { transceive(selectCommand) }.getOrNull()

    // Check if selection was successful (Trailing bytes 90 00)
    response != null && response.size >= 2 &&
            response[response.size - 2] == 0x90.toByte() &&
            response[response.size - 1] == 0x00.toByte()
}
