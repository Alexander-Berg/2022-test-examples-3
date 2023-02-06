package ru.yandex.market.logistics.yard.controller.idm

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.service.AudioSynthesisService


class AudioSynthesisServiceTest(
    @Autowired private val audioSynthesisService: AudioSynthesisService,
    @Autowired @Qualifier("beepBase64") val beepBase64: String
) : AbstractSecurityMockedContextualTest() {

    @Test
    fun testSynthesize() {
        val text = audioSynthesisService.synthesize("test")
        assertions().assertThat(text).isEqualTo(beepBase64)
    }
}
