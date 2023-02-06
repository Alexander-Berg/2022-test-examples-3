package ru.yandex.market.logistics.les.objectmapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.crypto.EncryptedString
import ru.yandex.market.logistics.les.exception.DecryptionException
import ru.yandex.market.logistics.les.mapper.component.AesCipherUtil
import ru.yandex.market.logistics.les.mapper.component.CipherTextWithIv
import ru.yandex.market.logistics.les.mapper.component.MapperFactory
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import javax.crypto.BadPaddingException

class EncryptedStringObjectMapperTest : AbstractContextualTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var aesCipherUtil: AesCipherUtil

    private val clientObjectMapper = MapperFactory.getClientObjectMapper()

    private val encryptedString = EncryptedString("hello")

    @Test
    fun testEncryptAndDecryptAreCompatible() {
        val str = objectMapper.writeValueAsString(encryptedString)
        assertThat(objectMapper.readValue<EncryptedString>(str)).isEqualTo(encryptedString)
    }

    @Test
    fun testEncryptFormat() {
        val expected = extractFileContent("objectmapper/crypto/for_encrypt.json")
        doReturn(
            CipherTextWithIv(
                ByteArray(16) { i -> i.toByte() },
                ByteArray(16) { i -> (i + 2).toByte() }
            )
        )
            .whenever(aesCipherUtil)
            .encrypt(any())

        val actual = objectMapper.writeValueAsString(encryptedString)

        JSONAssert.assertEquals(expected, actual, true)
    }

    @Test
    fun testDecrypt() {
        val actual = objectMapper.readValue<EncryptedString>(
            extractFileContent("objectmapper/crypto/for_decrypt.json")
        )
        assertThat(actual).isEqualTo(encryptedString)
    }

    @Test
    fun testDecryptWrongKey() {
        assertThatThrownBy {
            objectMapper.readValue<EncryptedString>(
                extractFileContent("objectmapper/crypto/for_decrypt_wrong_key.json")
            )
        }
            .isInstanceOf(DecryptionException::class.java)
            .hasCauseInstanceOf(BadPaddingException::class.java)
            .hasMessage(
                "Unable to deserialize crypto string " +
                        "CipherTextWithIv(" +
                        "cipherText=p4AGWH2c6uciFPdHktLCrw==, " +
                        "iv=cvVXeWEo0Ya//Q7RtlNkDQ==). " +
                        "Probably it was serialized with different key"
            )
    }

    @Test
    fun serializeWithClientObjectMapper() {
        val expected = extractFileContent("objectmapper/crypto/plain.json")
        JSONAssert.assertEquals(expected, clientObjectMapper.writeValueAsString(encryptedString), true)
    }

    @Test
    fun deserializeWithClientObjectMapper() {
        val actual = clientObjectMapper.readValue<EncryptedString>(
            extractFileContent("objectmapper/crypto/plain.json")
        )
        assertThat(actual).isEqualTo(encryptedString)
    }
}
