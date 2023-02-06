package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.ff4shops.api.model.DebugStatus
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason
import ru.yandex.market.mbi.open.api.client.model.DeliveryServiceTypeDTO
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class StocksUpdateStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testNoneWarehouse() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            orderProcessingType = OrderProcessingType.API,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockWarehouse(false)
        mockStocksDebugStatus(DebugStatus.NO_REQUESTS)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.STOCK_UPDATE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.STOCK_UPDATE)
                    .status(WizardStepStatus.NONE)
            ),
            result.steps
        )
    }

    @Test
    fun testFull() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            orderProcessingType = OrderProcessingType.API,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockWarehouse(true)
        mockLmsPartner(
            PartnerResponse.newBuilder()
                .id(WAREHOUSE_ID)
                .stockSyncEnabled(true)
                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                .build()
        )
        mockStocksDebugStatus(DebugStatus.SUCCESS)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.STOCK_UPDATE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.STOCK_UPDATE)
                    .status(WizardStepStatus.FULL)
            ),
            result.steps
        )
    }

    @Test
    fun testNoneEmptyNoRequest() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            orderProcessingType = OrderProcessingType.API,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockWarehouse(true)
        mockStocksDebugStatus(DebugStatus.NO_REQUESTS)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.STOCK_UPDATE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.STOCK_UPDATE)
                    .status(WizardStepStatus.EMPTY)
            ),
            result.steps
        )
    }

    @Test
    fun testNoneStockDebugStatus() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            orderProcessingType = OrderProcessingType.API,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockWarehouse(true)
        mockStocksDebugStatus(DebugStatus.ERROR)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.STOCK_UPDATE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.STOCK_UPDATE)
                    .status(WizardStepStatus.NONE)
            ),
            result.steps
        )
    }

    @Test
    fun testEmptyStockSwitchReason() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            orderProcessingType = OrderProcessingType.API,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockWarehouse(true)
        mockStocksDebugStatus(DebugStatus.SUCCESS)
        mockLmsPartner(
            PartnerResponse.newBuilder()
                .id(WAREHOUSE_ID)
                .stockSyncEnabled(false)
                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                .build()
        )

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.STOCK_UPDATE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.STOCK_UPDATE)
                    .status(WizardStepStatus.EMPTY)
            ),
            result.steps
        )
    }

    @Test
    fun testFail() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            orderProcessingType = OrderProcessingType.API,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockWarehouse(true)
        mockStocksDebugStatus(DebugStatus.SUCCESS)
        mockLmsPartner(
            PartnerResponse.newBuilder()
                .id(WAREHOUSE_ID)
                .stockSyncEnabled(false)
                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_LMS_UI)
                .build()
        )

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.STOCK_UPDATE)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.STOCK_UPDATE)
                    .status(WizardStepStatus.FAILED)
            ),
            result.steps
        )
    }
}
