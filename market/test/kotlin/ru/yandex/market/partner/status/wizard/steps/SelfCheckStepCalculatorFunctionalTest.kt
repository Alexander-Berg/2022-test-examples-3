package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO
import ru.yandex.market.core.feature.model.FeatureType
import ru.yandex.market.core.param.model.BooleanParamValue
import ru.yandex.market.core.param.model.ParamCheckStatus
import ru.yandex.market.core.param.model.ParamType
import ru.yandex.market.mbi.api.client.entity.shops.ShopFeatureInfoDTO
import ru.yandex.market.mbi.open.api.client.model.DeliveryServiceTypeDTO
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class SelfCheckStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testFailed() {
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
        mockPartnerParams(listOf())
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.DONT_WANT.name,
                    false,
                    null,
                    null,
                    true
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
                    .status(WizardStepStatus.FAILED)
            ),
            result.steps
        )
    }

    @Test
    fun testNone() {
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
        mockPartnerParams(listOf())
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.DONT_WANT.name,
                    false,
                    null,
                    null,
                    null
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
                    .status(WizardStepStatus.NONE)
            ),
            result.steps
        )
    }

    @Test
    fun testFilled() {
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
        mockPartnerParams(listOf())
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.NEW.name,
                    false,
                    null,
                    null,
                    true
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
                    .status(WizardStepStatus.FILLED)
            ),
            result.steps
        )
    }

    @Test
    fun testNoneZeroCount() {
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
        mockPartnerParams(listOf(BooleanParamValue(ParamType.IS_IN_TEST_INDEX, PARTNER_ID, true)))
        mockAboSelfCheck(listOf())
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.NEW.name,
                    false,
                    null,
                    null,
                    true
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
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
        mockWarehouse(false)
        mockPartnerParams(listOf(BooleanParamValue(ParamType.IS_IN_TEST_INDEX, PARTNER_ID, true)))
        mockAboSelfCheck(
            listOf(
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(1L)
                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_PARTNER)
                        .build()
                )
            )
        )
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.NEW.name,
                    false,
                    null,
                    null,
                    true
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
                    .status(WizardStepStatus.FULL)
            ),
            result.steps
        )
    }

    @Test
    fun testRestricted() {
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
        mockPartnerParams(listOf(BooleanParamValue(ParamType.IS_IN_TEST_INDEX, PARTNER_ID, true)))
        mockAboSelfCheck(
            listOf(
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(1L)
                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_PARTNER)
                        .build()
                ),
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(2L)
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_CUSTOMER)
                        .build()
                )
            )
        )
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.NEW.name,
                    false,
                    null,
                    null,
                    true
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
                    .status(WizardStepStatus.RESTRICTED)
            ),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
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
        mockPartnerParams(listOf(BooleanParamValue(ParamType.IS_IN_TEST_INDEX, PARTNER_ID, true)))
        mockAboSelfCheck(
            listOf(
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(1L)
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_PARTNER)
                        .build()
                ),
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(2L)
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_CUSTOMER)
                        .build()
                )
            )
        )
        Mockito.`when`(mbiApiClient.enableShopFeature(Mockito.eq(PARTNER_ID), Mockito.eq(FeatureType.DROPSHIP.id)))
            .thenReturn(
                ShopFeatureInfoDTO(
                    PARTNER_ID,
                    FeatureType.DROPSHIP,
                    ParamCheckStatus.NEW.name,
                    false,
                    null,
                    null,
                    true
                )
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SELF_CHECK)
                    .status(WizardStepStatus.EMPTY)
            ),
            result.steps
        )
    }
}
