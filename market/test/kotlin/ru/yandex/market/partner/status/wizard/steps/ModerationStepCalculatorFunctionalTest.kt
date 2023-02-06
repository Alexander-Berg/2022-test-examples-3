package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.core.param.model.BooleanParamValue
import ru.yandex.market.core.param.model.ParamType
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.mbi.open.api.client.model.TestingStatusDTO
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class ModerationStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testNone() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, false)
            )
        )
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockModeration(TestingStatusDTO.PENDING_CHECK_START, false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MODERATION)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MODERATION).status(WizardStepStatus.NONE)),
            result.steps
        )
    }

    @Test
    fun testFull() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockModeration(TestingStatusDTO.PASSED, false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MODERATION)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MODERATION).status(WizardStepStatus.FULL)),
            result.steps
        )
    }

    @Test
    fun testRestricted() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockModeration(TestingStatusDTO.NEED_INFO, false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MODERATION)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MODERATION).status(WizardStepStatus.RESTRICTED)),
            result.steps
        )
    }

    @Test
    fun testFailed() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockModeration(TestingStatusDTO.INITED, true)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MODERATION)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MODERATION).status(WizardStepStatus.FAILED)),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockModeration(TestingStatusDTO.INITED, false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MODERATION)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MODERATION).status(WizardStepStatus.EMPTY)),
            result.steps
        )
    }

    @Test
    fun testFilled() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockModeration(TestingStatusDTO.PENDING_CHECK_START, false)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.MODERATION)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.MODERATION).status(WizardStepStatus.FILLED)),
            result.steps
        )
    }
}
