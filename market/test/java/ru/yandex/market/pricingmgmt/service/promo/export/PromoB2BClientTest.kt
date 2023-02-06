package ru.yandex.market.pricingmgmt.service.promo.export

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoAdditionalInfoDto
import ru.yandex.market.pricingmgmt.service.promo.export.dto.PromoDescriptionRequestDto


class PromoB2BClientTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var promoB2BClient: PromoB2BClient

    @MockBean
    private lateinit var httpClient: CloseableHttpClient

    @Captor
    private lateinit var requestCaptor: ArgumentCaptor<HttpEntityEnclosingRequestBase>

    @BeforeEach
    fun setUp() {
        Mockito.reset(httpClient)
    }

    @Test
    fun createPromo_enName_ok() {
        baseCreatePromoTest("Test promo")
    }

    @Test
    fun createPromo_ruName_ok() {
        baseCreatePromoTest("Тестовая акция")
    }

    private fun baseCreatePromoTest(promoName: String) {
        // setup
        val promoDto = PromoDescriptionRequestDto(additionalInfo = PromoAdditionalInfoDto(promoName = promoName))

        // act
        promoB2BClient.createPromo(promoDto)

        //verify
        Mockito.verify(httpClient).execute(requestCaptor.capture())

        val expectedSerializedObject = "{\"additionalInfo\":{\"promoName\":\"$promoName\"}}"
        val actualSerializedObject = org.apache.http.util.EntityUtils.toString(requestCaptor.value.entity)

        Assertions.assertEquals(expectedSerializedObject, actualSerializedObject)
    }
}
