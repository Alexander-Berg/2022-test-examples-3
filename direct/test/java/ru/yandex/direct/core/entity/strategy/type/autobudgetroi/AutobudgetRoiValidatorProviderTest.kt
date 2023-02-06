package ru.yandex.direct.core.entity.strategy.type.autobudgetroi

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.AbstractStrategyOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetRoiStrategy.autobudgetRoi
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
class AutobudgetRoiValidatorProviderTest {
    private val provider = AutobudgetRoiValidatorProvider

    companion object {
        data class Defects(
            val roiCoeffDefectId: Defect<*>? = null,
            val profitabilityDefectId: Defect<*>? = null,
            val reserveReturnDefectId: Defect<*>? = null
        )

        data class TestData(
            val roiCoeff: Long?,
            val profitability: Long?,
            val reserveReturn: Long?,
            val expectedDefects: Defects
        ) {
            override fun toString(): String {
                return "TestData(roiCoeff=$roiCoeff, profitability=$profitability, reserveReturn=$reserveReturn)"
            }
        }

        @JvmStatic
        fun testData(): List<List<TestData>> =
            listOf(
                TestData(
                    null,
                    null,
                    null,
                    Defects(CommonDefects.notNull(), null, CommonDefects.notNull())
                ),
                TestData(1L, 50L, 50, Defects()),
                TestData(1L, 50L, 55, Defects(reserveReturnDefectId = StrategyDefects.incorrectReserveReturn())),
                TestData(
                    1L,
                    50L,
                    110,
                    Defects(
                        reserveReturnDefectId = NumberDefects.inInterval(
                            CampaignConstants.RESERVE_RETURN_MIN,
                            CampaignConstants.RESERVE_RETURN_MAX
                        )
                    )
                ),
                TestData(
                    1L,
                    -1L,
                    50,
                    Defects(
                        profitabilityDefectId = NumberDefects.inInterval(
                            CampaignConstants.PROFITABILITY_MIN,
                            CampaignConstants.PROFITABILITY_MAX
                        )
                    )
                ),
                TestData(
                    1L,
                    101L,
                    50,
                    Defects(
                        profitabilityDefectId = NumberDefects.inInterval(
                            CampaignConstants.PROFITABILITY_MIN,
                            CampaignConstants.PROFITABILITY_MAX
                        )
                    )
                ),
                TestData(
                    -2L,
                    50L,
                    50,
                    Defects(roiCoeffDefectId = NumberDefects.greaterThan(CampaignConstants.ROI_COEF_MIN))
                ),
            ).map { listOf(it) }
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        val strategy = strategy(testData.roiCoeff, testData.profitability, testData.reserveReturn)
        val container = Mockito.mock(AbstractStrategyOperationContainer::class.java)
        val validator = AutobudgetRoiValidatorProvider.createStrategyValidator(container)
        val vr = validator.apply(strategy)
        check(vr, testData.expectedDefects)
    }

    private fun check(vr: ValidationResult<AutobudgetRoi, Defect<*>>, expectedDefects: Defects) {
        val roiCoefMatcher = expectedDefects.roiCoeffDefectId?.let { matcher(AutobudgetRoi.ROI_COEF, it) }
        val profitabilityMatcher =
            expectedDefects.profitabilityDefectId?.let { matcher(AutobudgetRoi.PROFITABILITY, it) }
        val reserveReturnMatcher =
            expectedDefects.reserveReturnDefectId?.let { matcher(AutobudgetRoi.RESERVE_RETURN, it) }
        val matchers = listOf(roiCoefMatcher, profitabilityMatcher, reserveReturnMatcher).filterNotNull()
        if (matchers.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            matchers.forEach {
                vr.check(it)
            }
        }
    }

    private fun matcher(modelProperty: ModelProperty<*, *>, defect: Defect<*>) =
        Matchers.hasDefectDefinitionWith<AutobudgetRoi>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(modelProperty)),
                defect
            )
        )

    private fun strategy(roiCoeff: Long?, profitability: Long?, reserveReturn: Long?): AutobudgetRoi =
        autobudgetRoi()
            .withRoiCoef(roiCoeff?.toBigDecimal())
            .withProfitability(profitability?.toBigDecimal())
            .withReserveReturn(reserveReturn)
}
