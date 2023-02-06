package ru.yandex.market.wms.dimensionmanagement.security

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AESCypherTest {
    @Test
    fun encryptDecryptTest() {
        val sample = "equipPwd#4"
        val encrypted = AESCypher.encrypt(sample, "pwdABC12", "pwdSalt8")
        val decrypted = AESCypher.decrypt(encrypted, "pwdABC12", "pwdSalt8")

        Assertions.assertEquals(sample, decrypted)
    }

    @Test
    fun encryptDecryptSmallTextTest() {
        val sample = "test"
        val encrypted = AESCypher.encrypt(sample, "pwdABC123456", "saltTst0")
        val decrypted = AESCypher.decrypt(encrypted, "pwdABC123456", "saltTst0")

        Assertions.assertEquals(sample, decrypted)
    }
}
