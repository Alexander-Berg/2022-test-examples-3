package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO
import ru.yandex.market.core.param.model.BooleanParamValue
import ru.yandex.market.core.param.model.ParamType
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.mbi.open.api.client.model.TestingStatusDTO
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class ShopSelfCheckStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testNone() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(listOf(BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true)))
        mockSelfCheckState(null)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.NONE)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 0.0,
                                "failedCount" to 0.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testSkipFull() {
        mockPartnerInfo(PartnerPlacementType.DBS)
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

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.FULL)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 0.0,
                                "failedCount" to 0.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
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
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.CHECKING)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(
            listOf(
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(1)
                        .withType(CheckOrderScenarioType.SUCCESSFUL_ORDER_DSBS)
                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                        .build()
                )
            )
        )

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.FULL)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 1.0,
                                "failedCount" to 0.0,
                                "successCount" to 1.0,
                                "selfCheckPassed" to true,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
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
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.CHECKING)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(
            listOf(
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(1)
                        .withType(CheckOrderScenarioType.SUCCESSFUL_ORDER_DSBS)
                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                        .build()
                ),
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(2)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_CUSTOMER_DSBS)
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .build()
                )
            )
        )

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.RESTRICTED)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 2.0,
                                "failedCount" to 1.0,
                                "successCount" to 1.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
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
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.CHECKING)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(
            listOf(
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(1)
                        .withType(CheckOrderScenarioType.SUCCESSFUL_ORDER_DSBS)
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .build()
                ),
                SelfCheckDTO(
                    PARTNER_ID,
                    CheckOrderScenarioDTO.builder(2)
                        .withType(CheckOrderScenarioType.CANCELLED_BY_CUSTOMER_DSBS)
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .build()
                )
            )
        )

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.FILLED)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 2.0,
                                "failedCount" to 2.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to true
                            )
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testEnabling() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.READY_FOR_CHECK)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(listOf())

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.ENABLING)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 0.0,
                                "failedCount" to 0.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
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
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.FAILED)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(listOf())

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.FAILED)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 0.0,
                                "failedCount" to 0.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
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
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.INITED)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(listOf())

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.EMPTY)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 0.0,
                                "failedCount" to 0.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testFilledInited() {
        mockPartnerInfo(PartnerPlacementType.DBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockShopOutlets(false)
        mockHasConfiguredDelivery(true)
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, false),
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true),
                BooleanParamValue(ParamType.IS_IN_TEST_INDEX, PARTNER_ID, true)
            )
        )
        mockSelfCheckState(TestingStatusDTO.INITED)
        mockValidOfferWithoutStock(1)
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockShopFeedCheck(1)
        mockAboSelfCheck(listOf())

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.SHOP_SELF_CHECK)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.SHOP_SELF_CHECK)
                    .status(WizardStepStatus.FILLED)
                    .details(
                        mapOf(
                            "selfCheckScenarios" to mapOf(
                                "totalCount" to 0.0,
                                "failedCount" to 0.0,
                                "successCount" to 0.0,
                                "selfCheckPassed" to false,
                                "selfCheckFailed" to false
                            )
                        )
                    )
            ),
            result.steps
        )
    }
}
