package ru.yandex.market.partner.status.wizard.service

import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.mockito.Mockito.mock
import ru.yandex.market.partner.status.wizard.check.WizardCheck
import ru.yandex.market.partner.status.wizard.check.WizardCheckType
import ru.yandex.market.partner.status.wizard.model.WizardStepInfo
import ru.yandex.market.partner.status.wizard.model.WizardStepStatus
import ru.yandex.market.partner.status.wizard.model.WizardStepType
import ru.yandex.market.partner.status.wizard.model.check.WizardCheckResult
import ru.yandex.market.partner.status.wizard.model.partner.OrderProcessingType
import ru.yandex.market.partner.status.wizard.model.partner.PartnerInfo
import ru.yandex.market.partner.status.wizard.model.partner.PartnerPlacementType
import ru.yandex.market.partner.status.wizard.steps.WizardStepCalculator
import kotlin.system.measureTimeMillis

class WizardServiceTest {

//    @Test
    fun testSimpleSteps() {
        val service = testWizardService()

        var steps: List<WizardStepInfo>
        val time = measureTimeMillis {
            steps = service.getPartnerSteps(1L)
        }

        assertThat(steps).hasSize(3)

        val prepayStep = steps[0]
        assertThat(prepayStep.status).isEqualTo(WizardStepStatus.EMPTY)

        val details = prepayStep.details as Map<*, *>
        assertThat(details["someDetail"]).isEqualTo("info1")

        val assortmentStep = steps[1]
        assertThat(assortmentStep.status).isEqualTo(WizardStepStatus.FILLED)

        val marketplaceStep = steps[2]
        assertThat(marketplaceStep.status).isEqualTo(WizardStepStatus.NONE)

        // Проверяем, что проверки исполнялись параллельно в разных потоках
        assertThat(time).isLessThan(2000L)
    }

//    @Test
    fun testStepsWithException() {
        val service = testWizardServiceWithBadCalculator()
        val steps = service.getPartnerSteps(1L)

        assertThat(steps).hasSize(3)

        val prepayStep = steps[0]
        assertThat(prepayStep.status).isEqualTo(WizardStepStatus.INTERNAL_ERROR)

        val assortmentStep = steps[1]
        assertThat(assortmentStep.status).isEqualTo(WizardStepStatus.FILLED)

        val marketplaceStep = steps[2]
        assertThat(marketplaceStep.status).isEqualTo(WizardStepStatus.NONE)
    }

    private fun testWizardService(): WizardService {
        return WizardService(
            mockPartnerInfoService(),
            mockWizardStepsResolver()
        )
    }

    private fun testWizardServiceWithBadCalculator(): WizardService {
        return WizardService(
            mockPartnerInfoService(),
            mockBadWizardStepsResolver()
        )
    }

    private fun mockPartnerInfoService(): PartnerInfoService {
        val mockService = mock(PartnerInfoService::class.java)
        Mockito.`when`(mockService.getPartnerInfo(Mockito.anyLong())).thenReturn(
            PartnerInfo(
                partnerId = 1L,
                orderProcessingType = OrderProcessingType.PI,
                partnerPlacementType = PartnerPlacementType.FBS,
                businessId = 2,
                partnerFFLinks = emptyList()
            )
        )

        return mockService
    }

    private fun mockPrepayRequestCalculator(): WizardStepCalculator {
        return object : WizardStepCalculator() {
            override fun getStepType(): WizardStepType {
                return WizardStepType.PAYMENTS_REQUEST
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                val prepayCheck = context.getWizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST)
                return if (prepayCheck.resultValue == "prepay_ok") {
                    wizardStepInfo(WizardStepStatus.EMPTY, mapOf("someDetail" to "info1"))
                } else {
                    wizardStepInfo(WizardStepStatus.NONE)
                }
            }

            override fun getStepChecks(partnerInfo: PartnerInfo): List<WizardCheck<*>> {
                return listOf(mockPrepayRequestCheck())
            }
        }
    }

    private fun mockBadPrepayRequestCalculator(): WizardStepCalculator {
        return object : WizardStepCalculator() {
            override fun getStepType(): WizardStepType {
                return WizardStepType.PAYMENTS_REQUEST
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                val prepayCheck = context.getWizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST)

                return if (prepayCheck.resultValue == "prepay_ok") {
                    wizardStepInfo(WizardStepStatus.EMPTY, mapOf("someDetail" to "info1"))
                } else {
                    wizardStepInfo(WizardStepStatus.NONE)
                }
            }

            override fun getStepChecks(partnerInfo: PartnerInfo): List<WizardCheck<*>> {
                return listOf(mockPrepayRequestCheckWithError())
            }
        }
    }

    private fun mockAssortmentCalculator(): WizardStepCalculator {
        return object : WizardStepCalculator() {
            override fun getStepType(): WizardStepType {
                return WizardStepType.ASSORTMENT
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                return wizardStepInfo(WizardStepStatus.FILLED)
            }

            override fun getStepChecks(partnerInfo: PartnerInfo): List<WizardCheck<*>> {
                return listOf(mockPriceCheck())
            }
        }
    }

    private fun mockMarketplaceCalculator(): WizardStepCalculator {

        return object : WizardStepCalculator() {

            override fun getStepType(): WizardStepType {
                return WizardStepType.MARKETPLACE
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                return if (!context.getWizardStepInfo(WizardStepType.PAYMENTS_REQUEST).isCompleted) {
                    wizardStepInfo(WizardStepStatus.NONE)
                } else {
                    wizardStepInfo(WizardStepStatus.EMPTY)
                }
            }

            override fun getStepChecks(partnerInfo: PartnerInfo): List<WizardCheck<*>> {
                return emptyList()
            }

            override fun getDependingSteps(partnerInfo: PartnerInfo): List<WizardStepType> {
                return listOf(WizardStepType.PAYMENTS_REQUEST)
            }
        }
    }

    private fun mockWizardStepsResolver(): WizardStepsResolver {
        return WizardStepsResolver(
            mapOf(
                WizardStepType.PAYMENTS_REQUEST to mockPrepayRequestCalculator(),
                WizardStepType.ASSORTMENT to mockAssortmentCalculator(),
                WizardStepType.MARKETPLACE to mockMarketplaceCalculator()
            )
        )
    }

    private fun mockBadWizardStepsResolver(): WizardStepsResolver {
        return WizardStepsResolver(
            mapOf(
                WizardStepType.PAYMENTS_REQUEST to mockBadPrepayRequestCalculator(),
                WizardStepType.ASSORTMENT to mockAssortmentCalculator(),
                WizardStepType.MARKETPLACE to mockMarketplaceCalculator()
            )
        )
    }

    private fun mockPrepayRequestCheck(): ru.yandex.market.partner.status.wizard.check.WizardCheck<MockCheckStatus> {
        return object : ru.yandex.market.partner.status.wizard.check.WizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST) {
            override fun doCheck(partnerInfo: PartnerInfo): WizardCheckResult<MockCheckStatus> {
                Thread.sleep(1000L)
                return WizardCheckResult(MockCheckStatus("prepay_ok"))
            }
        }
    }

    private fun mockPrepayRequestCheckWithError(): ru.yandex.market.partner.status.wizard.check.WizardCheck<MockCheckStatus> {
        return object : ru.yandex.market.partner.status.wizard.check.WizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST) {
            override fun doCheck(partnerInfo: PartnerInfo): WizardCheckResult<MockCheckStatus> {
                throw RuntimeException("Connection error!")
            }
        }
    }

    private fun mockPriceCheck(): ru.yandex.market.partner.status.wizard.check.WizardCheck<MockCheckStatus> {
        return object : ru.yandex.market.partner.status.wizard.check.WizardCheck<MockCheckStatus>(WizardCheckType.PRICE) {
            override fun doCheck(partnerInfo: PartnerInfo): WizardCheckResult<MockCheckStatus> {
                Thread.sleep(1000L)
                return WizardCheckResult(MockCheckStatus("price_ok"))
            }
        }
    }

    private data class MockCheckStatus(
        val resultValue: String
    )
}
