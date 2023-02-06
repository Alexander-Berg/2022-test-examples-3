package ru.yandex.market.contentmapping.services.image

import org.assertj.core.api.Assertions
import org.junit.Test

class ReversibleEncryptServiceTest {

    companion object {
        const val hostApi = "api.ru:8080"
        const val hostAvatarTestEnv = "test.ru"
        const val hostAvatarProdEnv = "prod.ru"
        private val hostAvatarAllEnv = listOf(hostAvatarTestEnv, hostAvatarProdEnv)
        val reversibleEncryptService = ReversibleEncryptService(hostAvatarTestEnv, hostAvatarAllEnv, hostApi)
        const val internalImagePath = "/image.jpg"
    }

    @Test
    fun `check crypto`() {
        val text = "d290bebb8d390441da300173ad74c4948f92ddac53c4c5a8da4a22b9469b0ece"
        val encryptedText: String = reversibleEncryptService.encrypt(text)
        Assertions.assertThat(reversibleEncryptService.decrypt(encryptedText)).isEqualTo(text)
        Assertions.assertThat(encryptedText).isNotEqualTo(text)
        Assertions.assertThat(reversibleEncryptService.decrypt(encryptedText)).isNotEqualTo(encryptedText)
    }

    @Test
    fun `check external url`() {
        val externalImagePath = "https://somehost.ru/image.jpg"
        val encryptedExternalImagePath: String = reversibleEncryptService.processManualUrl(externalImagePath)
        Assertions.assertThat(encryptedExternalImagePath).isEqualTo(externalImagePath)
        val decryptedExternalImagePath: String = reversibleEncryptService.hideAvatarsUrl(externalImagePath)
        Assertions.assertThat(decryptedExternalImagePath).isEqualTo(externalImagePath)
    }

    @Test
    fun `check from bad api url`() {
        val badApiUrl = "https://$hostApi$internalImagePath"
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { reversibleEncryptService.processManualUrl(badApiUrl) }

    }

    @Test
    fun `check full url(test env)`() {
        val internalImageUrl = "http://$hostAvatarTestEnv$internalImagePath"
        val apiUrl: String = reversibleEncryptService.hideAvatarsUrl(internalImageUrl)
        Assertions.assertThat(apiUrl).isNotEqualTo(internalImageUrl)
        val internalImageUrlDecrypted = reversibleEncryptService.processManualUrl(apiUrl)
        Assertions.assertThat(internalImageUrlDecrypted).isEqualTo(internalImageUrl)
    }

    @Test
    fun `check full url(prod env)`() {
        val internalImageUrl = "http://$hostAvatarProdEnv$internalImagePath"
        val apiUrl: String = reversibleEncryptService.hideAvatarsUrl(internalImageUrl)
        Assertions.assertThat(apiUrl).isNotEqualTo(internalImageUrl)
        val internalImageUrlDecrypted = reversibleEncryptService.processManualUrl(apiUrl)
        Assertions.assertThat(internalImageUrl).isEqualTo(internalImageUrlDecrypted)
    }
}
