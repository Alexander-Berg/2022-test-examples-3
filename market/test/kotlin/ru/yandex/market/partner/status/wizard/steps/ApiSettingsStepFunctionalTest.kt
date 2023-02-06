package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.core.param.model.BooleanParamValue
import ru.yandex.market.core.param.model.ParamType
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.partner.status.wizard.model.check.apilog.LogStat
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType
import java.time.Instant

class ApiSettingsStepFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testNone() {
        mockPartnerInfo(PartnerPlacementType.FBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.IN_PROGRESS)
        mockContractOptions()
        mockPartnerParams(listOf())
        mockApiLogStat(
            LogStat.Builder()
                .setCount(0)
                .setErrorCount(0)
                .setSuccessCount(0)
                .setMinEventTime(Instant.now(clock))
                .setMaxEventTime(Instant.now(clock))
                .build()
        )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.API_SETTINGS)
                    .status(WizardStepStatus.NONE)
            ),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
        mockPartnerInfo(PartnerPlacementType.FBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(listOf())
        mockApiLogStat(
            LogStat.Builder()
                .setCount(0)
                .setErrorCount(0)
                .setSuccessCount(0)
                .setMinEventTime(Instant.now(clock))
                .setMaxEventTime(Instant.now(clock))
                .build()
        )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.API_SETTINGS)
                    .status(WizardStepStatus.EMPTY)
            ),
            result.steps
        )
    }

    @Test
    fun testFilled() {
        mockPartnerInfo(PartnerPlacementType.FBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(listOf(BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)))
        val logStat = LogStat.Builder()
            .setCount(0)
            .setErrorCount(0)
            .setSuccessCount(0)
            .setMinEventTime(Instant.now(clock))
            .setMaxEventTime(Instant.now(clock))
            .build()
        mockApiLogStat(logStat)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.API_SETTINGS)
                    .status(WizardStepStatus.FILLED)
                    .details(
                        mapOf(
                            "logStat" to mapOf(
                                "minEventTime" to "2022-02-17T00:00:00Z",
                                "maxEventTime" to "2022-02-17T08:24:00Z",
                                "successCount" to 0.0,
                                "errorCount" to 0.0,
                                "count" to 0.0,
                                "successRate" to 0.0
                            )
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testFull() {
        mockPartnerInfo(PartnerPlacementType.FBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(listOf(BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)))
        val logStat = LogStat.Builder()
            .setCount(1)
            .setErrorCount(0)
            .setSuccessCount(1)
            .setMinEventTime(Instant.now(clock))
            .setMaxEventTime(Instant.now(clock))
            .build()
        mockApiLogStat(logStat)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.API_SETTINGS)
                    .status(WizardStepStatus.FULL)
                    .details(
                        mapOf(
                            "logStat" to mapOf(
                                "minEventTime" to "2022-02-17T00:00:00Z",
                                "maxEventTime" to "2022-02-17T08:24:00Z",
                                "successCount" to 1.0,
                                "errorCount" to 0.0,
                                "count" to 1.0,
                                "successRate" to 1.0
                            )
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testRestricted() {
        mockPartnerInfo(PartnerPlacementType.FBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(listOf(BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)))
        val logStat = LogStat.Builder()
            .setCount(2)
            .setErrorCount(1)
            .setSuccessCount(1)
            .setMinEventTime(Instant.now(clock))
            .setMaxEventTime(Instant.now(clock))
            .build()
        mockApiLogStat(logStat)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.API_SETTINGS)
                    .status(WizardStepStatus.RESTRICTED)
                    .details(
                        mapOf(
                            "logStat" to mapOf(
                                "minEventTime" to "2022-02-17T00:00:00Z",
                                "maxEventTime" to "2022-02-17T08:24:00Z",
                                "successCount" to 1.0,
                                "errorCount" to 1.0,
                                "count" to 2.0,
                                "successRate" to 0.5
                            )
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testFail() {
        mockPartnerInfo(PartnerPlacementType.FBS, OrderProcessingType.API)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(listOf(BooleanParamValue(ParamType.CPA_IS_API_PARAMS_READY, PARTNER_ID, true)))
        val logStat = LogStat.Builder()
            .setCount(1)
            .setErrorCount(1)
            .setSuccessCount(0)
            .setMinEventTime(Instant.now(clock))
            .setMaxEventTime(Instant.now(clock))
            .build()
        mockApiLogStat(logStat)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.API_SETTINGS)
                    .status(WizardStepStatus.FAILED)
                    .details(
                        mapOf(
                            "logStat" to mapOf(
                                "minEventTime" to "2022-02-17T00:00:00Z",
                                "maxEventTime" to "2022-02-17T08:24:00Z",
                                "successCount" to 0.0,
                                "errorCount" to 1.0,
                                "count" to 1.0,
                                "successRate" to 0.0
                            )
                        )
                    )
            ),
            result.steps
        )
    }
}
