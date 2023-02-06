package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class ShopDeliveryStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testFullOutlets() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockShopOutlets(true)
        mockHasConfiguredDelivery(false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_DELIVERY)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.SHOP_DELIVERY).status(WizardStepStatus.FULL)),
            result.steps
        )
    }

    @Test
    fun testFullDelivery() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_DELIVERY)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.SHOP_DELIVERY).status(WizardStepStatus.FULL)),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockShopOutlets(false)
        mockHasConfiguredDelivery(false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_DELIVERY)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.SHOP_DELIVERY).status(WizardStepStatus.EMPTY)),
            result.steps
        )
    }
}
