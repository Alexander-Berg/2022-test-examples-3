package ru.yandex.market.partner.placement.wizard.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import ru.yandex.market.partner.placement.wizard.check.WizardCheck
import ru.yandex.market.partner.placement.wizard.check.WizardCheckType
import ru.yandex.market.partner.placement.wizard.model.WizardStepInfo
import ru.yandex.market.partner.placement.wizard.model.WizardStepStatus
import ru.yandex.market.partner.placement.wizard.model.WizardStepType
import ru.yandex.market.partner.placement.wizard.model.check.WizardCheckResult
import ru.yandex.market.partner.placement.wizard.model.partner.OrderProcessingType
import ru.yandex.market.partner.placement.wizard.model.partner.PartnerInfo
import ru.yandex.market.partner.placement.wizard.model.partner.PartnerPlacementType
import ru.yandex.market.partner.placement.wizard.steps.WizardStepCalculator
import kotlin.system.measureTimeMillis

class WizardServiceTest {

    @Test
    fun testSimpleSteps() {
        val service = testWizardService();

        var steps: List<WizardStepInfo>
        val time = measureTimeMillis {
            steps = service.getPartnerSteps(1L);
        }

        assertThat(steps).hasSize(3);

        val prepayStep = steps[0]
        assertThat(prepayStep.status).isEqualTo(WizardStepStatus.EMPTY)

        val details = prepayStep.details as Map<*, *>
        assertThat(details["someDetail"]).isEqualTo("info1")

        val assortmentStep = steps[1]
        assertThat(assortmentStep.status).isEqualTo(WizardStepStatus.FILLED)

        val marketplaceStep = steps[2]
        assertThat(marketplaceStep.status).isEqualTo(WizardStepStatus.NONE)

        //Проверяем, что проверки исполнялись параллельно в разных потоках
        assertThat(time).isLessThan(2000L)
    }

    @Test
    fun testStepsWithException() {
        val service = testWizardServiceWithBadCalculator()
        val steps = service.getPartnerSteps(1L);

        assertThat(steps).hasSize(3);

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
            PartnerInfo(1L, OrderProcessingType.PI, PartnerPlacementType.FBS)
        )

        return mockService
    }

    private fun mockPrepayRequestCalculator(): WizardStepCalculator {
        return object : WizardStepCalculator(emptyList(), listOf(mockPrepayRequestCheck())) {
            override fun getStepType(): WizardStepType {
                return WizardStepType.SUPPLIER_INFO
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                val prepayCheck = context.getWizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST).result
                return if(prepayCheck!!.resultValue == "prepay_ok") {
                    wizardStepInfo(WizardStepStatus.EMPTY, mapOf("someDetail" to "info1"))
                } else {
                    wizardStepInfo(WizardStepStatus.NONE)
                }
            }
        }
    }

    private fun mockBadPrepayRequestCalculator(): WizardStepCalculator {
        return object : WizardStepCalculator(emptyList(), listOf(mockPrepayRequestCheckWithError())) {
            override fun getStepType(): WizardStepType {
                return WizardStepType.SUPPLIER_INFO
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                val prepayCheck = context.getWizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST)
                if (prepayCheck.hasError()) {
                    return wizardStepInfo(WizardStepStatus.INTERNAL_ERROR)
                }
                return if(prepayCheck.result!!.resultValue == "prepay_ok") {
                    wizardStepInfo(WizardStepStatus.EMPTY, mapOf("someDetail" to "info1"))
                } else {
                    wizardStepInfo(WizardStepStatus.NONE)
                }
            }
        }
    }

    private fun mockAssortmentCalculator(): WizardStepCalculator {
        return object : WizardStepCalculator(emptyList(), listOf(mockPriceCheck())) {
            override fun getStepType(): WizardStepType {
                return WizardStepType.ASSORTMENT
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                return wizardStepInfo(WizardStepStatus.FILLED)
            }
        }
    }

    private fun mockMarketplaceCalculator(): WizardStepCalculator {

        return object : WizardStepCalculator(listOf(WizardStepType.SUPPLIER_INFO), emptyList()) {

            override fun getStepType(): WizardStepType {
                return WizardStepType.MARKETPLACE
            }

            override suspend fun calculate(context: WizardContext): WizardStepInfo {
                return if (!context.getWizardStepInfo(WizardStepType.SUPPLIER_INFO).isCompleted) {
                    wizardStepInfo(WizardStepStatus.NONE)
                } else {
                    wizardStepInfo(WizardStepStatus.EMPTY)
                }
            }
        }
    }

    private fun mockWizardStepsResolver(): WizardStepsResolver {
        return WizardStepsResolver(mapOf(
            WizardStepType.SUPPLIER_INFO to mockPrepayRequestCalculator(),
            WizardStepType.ASSORTMENT to mockAssortmentCalculator(),
            WizardStepType.MARKETPLACE to mockMarketplaceCalculator()
        ))
    }

    private fun mockBadWizardStepsResolver(): WizardStepsResolver {
        return WizardStepsResolver(mapOf(
            WizardStepType.SUPPLIER_INFO to mockBadPrepayRequestCalculator(),
            WizardStepType.ASSORTMENT to mockAssortmentCalculator(),
            WizardStepType.MARKETPLACE to mockMarketplaceCalculator()
        ))
    }

    private fun mockPrepayRequestCheck(): WizardCheck<MockCheckStatus> {
        return object : WizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST) {
            override fun doCheck(partnerInfo: PartnerInfo): WizardCheckResult<MockCheckStatus> {
                Thread.sleep(1000L)
                return WizardCheckResult(MockCheckStatus("prepay_ok"))
            }
        }
    }

    private fun mockPrepayRequestCheckWithError(): WizardCheck<MockCheckStatus> {
        return object : WizardCheck<MockCheckStatus>(WizardCheckType.PREPAY_REQUEST) {
            override fun doCheck(partnerInfo: PartnerInfo): WizardCheckResult<MockCheckStatus> {
                throw RuntimeException("Connection error!")
            }
        }
    }

    private fun mockPriceCheck(): WizardCheck<MockCheckStatus> {
        return object : WizardCheck<MockCheckStatus>(WizardCheckType.PRICE) {
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
