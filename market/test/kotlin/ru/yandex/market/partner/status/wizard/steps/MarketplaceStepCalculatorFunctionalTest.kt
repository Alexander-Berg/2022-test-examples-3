package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.core.feature.model.FeatureType
import ru.yandex.market.core.param.model.ParamCheckStatus
import ru.yandex.market.mbi.open.api.client.model.DeliveryServiceTypeDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.partner.status.wizard.model.check.feed.LastUploadedFeedInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType
import java.time.OffsetDateTime

class MarketplaceStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testFbyNone() {
        mockPartnerInfo(PartnerPlacementType.FBY)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockFeedInfo(LastUploadedFeedInfo("name", "ya.ru", OffsetDateTime.now(), null))
        mockValidOfferWithoutStock(0)
        mockTotalBusinessOffers(1)
        mockTotalPartnerOffers(1)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MARKETPLACE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MARKETPLACE).status(WizardStepStatus.NONE)),
            result.steps
        )
    }

    @Test
    fun testFbyEmpty() {
        mockPartnerInfo(PartnerPlacementType.FBY)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockFeedInfo(LastUploadedFeedInfo("name", "ya.ru", OffsetDateTime.now(), 1))
        mockValidOfferWithoutStock(1)
        mockTotalBusinessOffers(1)
        mockTotalPartnerOffers(1)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MARKETPLACE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MARKETPLACE).status(WizardStepStatus.EMPTY)),
            result.steps
        )
    }

    @Test
    fun testFbsNone() {
        mockPartnerInfo(PartnerPlacementType.FBS)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockFeedInfo(LastUploadedFeedInfo("name", "ya.ru", OffsetDateTime.now(), null))
        mockValidOfferWithStock(0)
        mockTotalBusinessOffers(1)
        mockTotalPartnerOffers(1)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MARKETPLACE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MARKETPLACE).status(WizardStepStatus.NONE)),
            result.steps
        )
    }

    @Test
    fun testFbsEmpty() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockFeedInfo(LastUploadedFeedInfo("name", "ya.ru", OffsetDateTime.now(), 1))
        mockValidOfferWithStock(1)
        mockTotalBusinessOffers(1)
        mockTotalPartnerOffers(1)
        mockWarehouse(true)
        mockFeature(FeatureType.DROPSHIP, ParamCheckStatus.DONT_WANT)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MARKETPLACE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MARKETPLACE).status(WizardStepStatus.EMPTY)),
            result.steps
        )
        Mockito.verify(mbiApiClient).successShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id))
    }
}
