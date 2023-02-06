package ru.yandex.market.logistics.les.compatibility.crypto

import ru.yandex.market.logistics.les.mapper.component.AesCipherUtil
import ru.yandex.market.logistics.les.mapper.component.EncryptedStringDeserializer
import ru.yandex.market.logistics.les.mapper.component.EncryptedStringSerializer

private const val BASE_64_KEY: String = "wk7cx+mdVobGCAd72i5nptzwSN8vvFjqMFy1xcrT/DI="

class AesFixedIvCipher : AesCipherUtil(BASE_64_KEY) {
    override fun generateIv(): ByteArray {
        return ByteArray(16) { i -> i.toByte() }
    }

    companion object {
        private val INSTANCE = AesFixedIvCipher()
        val SERIALIZER: EncryptedStringSerializer = EncryptedStringSerializer(INSTANCE)
        val DESERIALIZER: EncryptedStringDeserializer = EncryptedStringDeserializer(INSTANCE)
    }
}
