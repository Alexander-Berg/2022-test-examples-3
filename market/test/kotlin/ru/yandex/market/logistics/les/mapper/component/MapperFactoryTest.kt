package ru.yandex.market.logistics.les.mapper.component

import java.security.InvalidAlgorithmParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.yandex.market.logistics.les.base.crypto.EncryptedString
import ru.yandex.market.logistics.les.exception.DecryptionException

internal class MapperFactoryTest {

    private val util = AesCipherUtil("wk7cx+mdVobGCAd72i5nptzwSN8vvFjqMFy1xcrT/DI=");
    private val serializer = EncryptedStringSerializer(util);
    private val deserializer = EncryptedStringDeserializer(util)
    private var objectMapperCryptoTrue = MapperFactory.getApplicationObjectMapper(
        serializer, deserializer, false
    )
    private var objectMapperCryptoFalse = MapperFactory.getApplicationObjectMapper(
        serializer, deserializer, false
    )
    private val encryptedString = EncryptedString("hello")

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun testCryptoTrue() {
        val str = objectMapperCryptoTrue.writeValueAsString(encryptedString)
        Assertions.assertThat(objectMapperCryptoTrue.readValue<EncryptedString>(str)).isEqualTo(encryptedString)
    }

    @Test
    fun testCryptoTrueEmptyStr() {
        val str = "{}"
        Assertions.assertThat(objectMapperCryptoTrue.readValue<EncryptedString>(str)).isEqualTo(EncryptedString(""))
    }

    @Test
    fun testCryptoTrueBadCrypto() {
        val str = "{\"value\": \"test\", \"iv\": \"test\"}"

        val exception =
            assertThrows<InvalidAlgorithmParameterException>("Must throws InvalidAlgorithmParameterException") {
                objectMapperCryptoTrue.readValue<EncryptedString>(str)
            }
        assertEquals("Wrong IV length: must be 16 bytes long", exception.message)
    }

    @Test
    fun testCryptoTrueBad2Crypto() {

        val str = "{" +
                "\"value\":\"PvKENSOjjGI+KI4DksG5aw==\",\n" +
                "\"iv\":\"nQbhQOgMFokyNKzUPTpbaA==\"" +
                "}"

        val exception = assertThrows<DecryptionException>("Must throws InvalidAlgorithmParameterException") {
            objectMapperCryptoTrue.readValue<EncryptedString>(str)
        }
        assertTrue(exception.message!!.contains("Probably it was serialized with different key"))
    }

    @Test
    fun testCryptoFalse() {
        val str = objectMapperCryptoFalse.writeValueAsString(encryptedString)
        Assertions.assertThat(objectMapperCryptoFalse.readValue<EncryptedString>(str)).isEqualTo(encryptedString)
    }
}
