package ru.yandex.market.partner.status.wizard

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingInfoResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.partner.status.AbstractFunctionalTest

class PartnerPlacementWizardControllerTest : AbstractFunctionalTest() {

    @Test
    @Disabled
    fun testWizard() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(1L)))
            .thenReturn(
                PartnerOnboardingInfoResponse()
                    .partnerId(1L)
                    .orderProcessingType(OrderProcessingType.PI)
                    .partnerPlacementType(PartnerPlacementType.FBS)
            )
        Mockito.`when`(mbiApiClient.getPartnerSuperAdmin(Mockito.eq(1L)))
            .thenReturn(
                BusinessOwnerDTO(10, 100, "login", setOf("a@ya.ru"))
            )
        val result = partnerPlacementWizardApiClient.getPartnerSteps(1L).schedule().join()
        Assertions.assertNotNull(result)
    }
}
