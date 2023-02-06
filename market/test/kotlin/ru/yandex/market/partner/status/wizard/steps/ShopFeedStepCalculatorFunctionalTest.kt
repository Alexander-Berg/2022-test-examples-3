package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class ShopFeedStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testFull() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockTotalPartnerOffers(1)
        mockValidOfferWithoutStock(1)
        mockShopFeedCheck(1)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_FEED)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_FEED)
                    .status(WizardStepStatus.FULL)
                    .details(mapOf("isUsingFeedLink" to true, "hasValidOffer" to true))
            ),
            result.steps
        )
    }

    @Test
    fun testFilled() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockTotalPartnerOffers(1)
        mockShopFeedCheck()
        mockValidOfferWithoutStock(0)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_FEED)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_FEED)
                    .status(WizardStepStatus.FILLED)
                    .details(mapOf("isUsingFeedLink" to false, "hasValidOffer" to false))
            ),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockTotalPartnerOffers(0)
        mockShopFeedCheck()
        mockValidOfferWithoutStock(0)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_FEED)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_FEED)
                    .status(WizardStepStatus.EMPTY)
                    .details(mapOf("isUsingFeedLink" to false, "hasValidOffer" to false))
            ),
            result.steps
        )
    }
}
