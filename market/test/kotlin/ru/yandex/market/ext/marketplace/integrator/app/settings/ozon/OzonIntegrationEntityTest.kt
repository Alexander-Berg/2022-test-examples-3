package ru.yandex.market.ext.marketplace.integrator.app.settings.ozon

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.ext.marketplace.integrator.app.AbstractFunctionalTest
import ru.yandex.mj.generated.server.model.OzonIntegrationDTO

class OzonIntegrationEntityTest : AbstractFunctionalTest() {

    @Test
    fun testToDto() {
        val entity = OzonIntegrationEntity(1L, 11L, "MyKey", true);
        val dto = entityToDto(entity)
        Assertions.assertThat(dto.partnerId).isEqualTo(entity.partnerId)
        Assertions.assertThat(dto.clientId).isEqualTo(entity.clientId)
        Assertions.assertThat(dto.apiKey).isEqualTo("***")
        Assertions.assertThat(dto.enabled).isEqualTo(entity.enabled)
    }

    @Test
    fun testFromDto() {
        val dto = OzonIntegrationDTO().partnerId(1L).clientId(11L).apiKey("abcd").enabled(true);
        val entity = OzonIntegrationEntity(dto.partnerId, dto.clientId, dto.apiKey, dto.enabled)
        Assertions.assertThat(entity.partnerId).isEqualTo(dto.partnerId)
        Assertions.assertThat(entity.clientId).isEqualTo(dto.clientId)
        Assertions.assertThat(entity.apiKey).isEqualTo(dto.apiKey)
        Assertions.assertThat(entity.enabled).isEqualTo(dto.enabled)
    }
}
