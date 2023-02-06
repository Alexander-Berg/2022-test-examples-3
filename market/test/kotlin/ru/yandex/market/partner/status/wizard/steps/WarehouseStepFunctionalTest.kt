package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.open.api.client.model.DeliveryServiceTypeDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class WarehouseStepFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testNone() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.IN_PROGRESS)
        mockWarehouse(false)
        mockContractOptions()
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.WAREHOUSE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.WAREHOUSE)
                    .status(WizardStepStatus.NONE)
            ),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
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
        mockWarehouse(false)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.WAREHOUSE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.WAREHOUSE)
                    .status(WizardStepStatus.EMPTY)
            ),
            result.steps
        )
    }

    @Test
    fun testFull() {
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
        mockWarehouse(true)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.WAREHOUSE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.WAREHOUSE)
                    .status(WizardStepStatus.FULL)
            ),
            result.steps
        )
    }
}
