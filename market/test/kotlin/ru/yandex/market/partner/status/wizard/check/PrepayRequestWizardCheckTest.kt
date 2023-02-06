package ru.yandex.market.partner.status.wizard.check

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.open.api.client.model.PartnerLastPrepayRequestDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingLegalDataResponse
import ru.yandex.market.mbi.open.api.client.model.PrepayType
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.wizard.model.check.prepay.OrganizationType
import ru.yandex.market.partner.status.wizard.model.check.prepay.PartnerApplicationStatus
import ru.yandex.market.partner.status.wizard.model.partner.OrderProcessingType
import ru.yandex.market.partner.status.wizard.model.partner.PartnerInfo
import ru.yandex.market.partner.status.wizard.model.partner.PartnerPlacementType

class PrepayRequestWizardCheckTest : AbstractFunctionalTest() {
    @Autowired
    protected lateinit var prepayRequestWizardCheck: PrepayRequestWizardCheck

    @Test
    fun testCheck() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(1L)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .id(1)
                            .inn("2347293470")
                            .partnerId(1)
                            .datasourceId(1)
                            .organizationName("orgName")
                            .organizationType(ru.yandex.market.mbi.open.api.client.model.OrganizationType.OOO)
                            .status(ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus.COMPLETED)
                            .prepayType(PrepayType.YANDEX_MARKET)
                    )
            )

        val result = prepayRequestWizardCheck.check(
            PartnerInfo(
                partnerId = 1L,
                orderProcessingType = OrderProcessingType.PI,
                partnerPlacementType = PartnerPlacementType.FBS,
                isClickAndCollect = false,
                businessId = 2,
                partnerFFLinks = emptyList()
            )
        )
        val prepayRequest = result.result?.prepayRequest

        assertThat(prepayRequest).isNotNull
        assertThat(prepayRequest?.id).isEqualTo(1)
        assertThat(prepayRequest?.partnerId).isEqualTo(1)
        assertThat(prepayRequest?.datasourceId).isEqualTo(1)
        assertThat(prepayRequest?.status).isEqualTo(
            PartnerApplicationStatus.COMPLETED
        )
        assertThat(prepayRequest?.inn).isEqualTo("2347293470")
        assertThat(prepayRequest?.organizationName).isEqualTo("orgName")
        assertThat(prepayRequest?.organizationType).isEqualTo(OrganizationType.OOO)
    }
}
