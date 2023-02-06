package ru.yandex.market.ext.marketplace.integrator.app.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.ext.marketplace.integrator.app.AbstractFunctionalTest
import ru.yandex.mj.generated.server.api.OzonIntegrationSettingsApi
import ru.yandex.mj.generated.server.model.OzonIntegrationDTO

@DbUnitDataSet(before = ["ozon-integration-settings-api-test-before.csv"])
class OzonIntegrationSettingsApiServiceTest : AbstractFunctionalTest() {

    @Autowired
    lateinit var ozonSettingsApi: OzonIntegrationSettingsApi

    @Test
    fun testGetSettings() {
        val expected = OzonIntegrationDTO().partnerId(1L).clientId(11).apiKey("***").enabled(true);
        val expectedNewPartner = OzonIntegrationDTO().partnerId(6L).clientId(0).apiKey("***").enabled(false);
        val result = ozonSettingsApi.getOzonSettings(1L)
        val newPartnerResult = ozonSettingsApi.getOzonSettings(6L)

        Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(result.body).isEqualTo(expected)

        Assertions.assertThat(newPartnerResult.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(newPartnerResult.body).isEqualTo(expectedNewPartner)
    }

    @DbUnitDataSet(after = ["ozon-integration-settings-api-test-update-after.csv"])
    @Test
    fun testUpdate() {
        val upd2 = OzonIntegrationDTO().partnerId(2L).enabled(true)
        val upd3 = OzonIntegrationDTO().partnerId(3L)
        val upd4 = OzonIntegrationDTO().partnerId(4L).enabled(true).clientId(44).apiKey("yyy")
        val updateOzonSettings2 = ozonSettingsApi.updateOzonSettings(2L, upd2)
        val updateOzonSettings3 = ozonSettingsApi.updateOzonSettings(3L, upd3)
        val updateOzonSettings4 = ozonSettingsApi.updateOzonSettings(4L, upd4)
        Assertions.assertThat(updateOzonSettings2.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(updateOzonSettings3.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(updateOzonSettings4.statusCode).isEqualTo(HttpStatus.OK)

    }
}
