package ru.yandex.market.partner.status.steps

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOption
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOptionsResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerLastPrepayRequestDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingInfoResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingLegalDataResponse
import ru.yandex.market.mbi.open.api.client.model.PrepayType
import ru.yandex.market.mbi.open.api.client.model.RequestType
import ru.yandex.market.partner.status.AbstractWizardTest
import ru.yandex.market.partner.status.wizard.model.WizardStepStatus
import ru.yandex.market.partner.status.wizard.model.WizardStepType
import java.time.OffsetDateTime

class PrepayRequestStepCalculatorTest : AbstractWizardTest() {

    private val PARTNER_ID = 1L
    private val BUSINESS_ID = 10L

    @Test
    @DisplayName("Заявки нет - шаг в EMPTY")
    fun testEmpty() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(null)
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.EMPTY)
    }

    @Test
    @DisplayName("Заявка в статусе NEW и у нее нет стартовой даты - шаг в EMPTY")
    fun testEmptyOnNoDate() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.NEW)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.EMPTY)
    }

    @Test
    @DisplayName("Заявка в статусе NEW и у нее есть стартовая дата - шаг в FILLED")
    fun testFilled() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .startDate(OffsetDateTime.now())
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.NEW)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.FILLED)
    }

    @Test
    @DisplayName("Заявка в статусе INIT - шаг в ENABLING")
    fun testStepEnabledOnInit() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.INIT)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.ENABLING)
    }

    @Test
    @DisplayName("Заявка в статусе IN_PROGRESS - шаг в ENABLING")
    fun testStepEnabledOnInProgress() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.IN_PROGRESS)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.ENABLING)
    }

    @Test
    @DisplayName("Заявка в статусе COMPLETED и нет частоты выплаты - шаг в FILLED")
    fun testStepFilledOnCompletedAppAndNoFrequency() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.COMPLETED)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
                    .currentContractId(1)
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.FILLED)
    }

    @Test
    @DisplayName("Заявка в статусе COMPLETED и нет контракта (частоту выплаты можно пропустить) - шаг в FULL")
    fun testStepFullOnCompletedWhenFrequencyCanBeSkipped() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.COMPLETED)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
                // Нет currentContractId, в таких случаях можно пропустить проверку частоты
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                        .isDefaultCurrentMonthFrequency(true)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.FULL)
    }

    @Test
    @DisplayName("Заявка в статусе COMPLETED и есть частота выплаты - шаг в FULL")
    fun testStepFullOnCompletedAppAndPresentFrequency() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(PARTNER_ID))
            .thenReturn(mockPartnerInfo())

        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .prepayType(PrepayType.YANDEX_MONEY)
                            .requestType(RequestType.MARKETPLACE)
                            .id(1L)
                            .datasourceId(PARTNER_ID)
                            .partnerId(PARTNER_ID)
                            .status(PartnerApplicationStatus.COMPLETED)
                    )
            )

        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(PARTNER_ID))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .jurName("Contract 1")
                        )
                    )
                    .currentContractId(1)
            )

        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(1L))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1)
                        .currentMonthFrequency(PayoutFrequencyDTO.DAILY)
                        .isDefaultCurrentMonthFrequency(false)
                        .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                )
            )

        val result = wizardService.getPartnerSteps(1L, WizardStepType.PAYMENTS_REQUEST)

        assertWizardStatus(result, WizardStepType.PAYMENTS_REQUEST, WizardStepStatus.FULL)
    }

    private fun mockPartnerInfo(): PartnerOnboardingInfoResponse? {
        return PartnerOnboardingInfoResponse()
            .partnerId(PARTNER_ID)
            .businessId(BUSINESS_ID)
            .orderProcessingType(ru.yandex.market.mbi.open.api.client.model.OrderProcessingType.PI)
            .partnerPlacementType(ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType.FBS)
    }
}
