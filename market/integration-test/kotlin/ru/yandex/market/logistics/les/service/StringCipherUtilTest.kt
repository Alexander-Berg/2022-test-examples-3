package ru.yandex.market.logistics.les.service

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.mapper.component.AesCipherUtil

internal class StringCipherUtilTest : AbstractContextualTest() {

    @Autowired
    lateinit var aesCipherUtil: AesCipherUtil

    @Autowired
    lateinit var stringCipherUtil: StringCipherUtil

    @Test
    fun encrypt() {
        doReturn(ByteArray(16) { i -> i.toByte() }).whenever(aesCipherUtil).generateIv()
        val actual = stringCipherUtil.encrypt("encrypt test string")
        assertThat(actual).isEqualTo("AAECAwQFBgcICQoLDA0ODw==:2xqlSw5WWK/MCkmRmtXO+E3+fp7wV9xaKMI/yW7qSMw=")
    }

    @Test
    fun decrypt() {
        val actual = stringCipherUtil.decrypt("Pu3ilV0X+NDtK8rMUbybaA==:M+4mpQHNB75K1tHae3idFF9BGPmvqFG1qGEL9qHFlkQ=")
        assertThat(actual).isEqualTo("decrypt test string")
    }

    @Test
    fun encryptAndDecryptLarge() {
        val expected = RandomStringUtils.random(1024 * 1024)
        assertThat(
            stringCipherUtil.decrypt(
                stringCipherUtil.encrypt(expected)
            )
        ).isEqualTo(expected)
    }

    @Test
    fun encryptAndDecryptRandom() {
        val expected = RandomStringUtils.random(1024)
        assertThat(
            stringCipherUtil.decrypt(
                stringCipherUtil.encrypt(expected)
            )
        ).isEqualTo(expected)
    }

    @Test
    internal fun invalidFormat() {
        assertThatThrownBy { stringCipherUtil.decrypt("invalid formatted encrypted string") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(
                "Зашифрованная строка " +
                        "должна иметь формат <base64 вектор инициализации>:<base64 шифр текст>"
            )
    }
}
