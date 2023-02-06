package ru.yandex.direct.core.entity.strategy.type.withpayforconversion

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_BANNER
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BY_ALL_GOALS_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.payForConversionDoesNotAllowAllGoals
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPI
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_CRR
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.CPA_STRATEGY_IN_CPM_BANNER_CAMPAIGN_ENABLED
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class PayForConversionValidatorTest {

    fun testData(): Array<Array<TestData>> = arrayOf(
        //not allowed for MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPI,
                true,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                true,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                true,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_CRR,
                true,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_FILTER,
                true,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals(),
                isValidatePayForConversionInAutobudgetAvgCpaPerFilter = true
            )
        ),
        //not allowed for BY_ALL_GOALS_GOAL_ID
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPI,
                true,
                BY_ALL_GOALS_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                true,
                BY_ALL_GOALS_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                true,
                BY_ALL_GOALS_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals()
            )
        ),
        arrayOf(TestData(AUTOBUDGET_CRR, true, BY_ALL_GOALS_GOAL_ID, defect = payForConversionDoesNotAllowAllGoals())),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_FILTER,
                true,
                BY_ALL_GOALS_GOAL_ID,
                defect = payForConversionDoesNotAllowAllGoals(),
                isValidatePayForConversionInAutobudgetAvgCpaPerFilter = true
            )
        ),
        //isPayForConversionEnabled=null allowable
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, null)),
        arrayOf(TestData(AUTOBUDGET_CRR, null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, null)),
        //isPayForConversionEnabled=false allowable
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, null)),
        arrayOf(TestData(AUTOBUDGET_CRR, null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, null)),

        //isPayForConversionEnabled=true allowable for campaignType=text
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, true, 1L)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, true, 1L)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, true, 1L)),
        arrayOf(TestData(AUTOBUDGET_CRR, true, 1L)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, true, 1L)),

        //isPayForConversionEnabled=true not allowable for campaignType=CPM_BANNER if feature disabled
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPI,
                true,
                1L,
                campaignType = CPM_BANNER,
                defect = inconsistentStrategyToCampaignType()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                true,
                1L,
                campaignType = CPM_BANNER,
                defect = inconsistentStrategyToCampaignType()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                true,
                1L,
                campaignType = CPM_BANNER,
                defect = inconsistentStrategyToCampaignType()
            )
        ),

        //isPayForConversionEnabled=true  allowable for campaignType=CPM_BANNER if feature enabled
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPI,
                true,
                1L,
                campaignType = CPM_BANNER,
                availableFeatures = setOf(CPA_STRATEGY_IN_CPM_BANNER_CAMPAIGN_ENABLED)
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                true,
                1L,
                campaignType = CPM_BANNER,
                availableFeatures = setOf(CPA_STRATEGY_IN_CPM_BANNER_CAMPAIGN_ENABLED)
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                true,
                1L,
                campaignType = CPM_BANNER,
                availableFeatures = setOf(CPA_STRATEGY_IN_CPM_BANNER_CAMPAIGN_ENABLED)
            )
        ),

        //isPayForConversionEnabled=true allowable for campaignType=null
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, true, 1L, campaignType = null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, true, 1L, campaignType = null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, true, 1L, campaignType = null)),
        arrayOf(TestData(AUTOBUDGET_CRR, true, 1L, campaignType = null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, true, 1L, campaignType = null)),

        arrayOf(
            TestData(
                AUTOBUDGET_CRR,
                true,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                availableFeatures = setOf(FeatureName.ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES)
            )
        ),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        val validator = PayForConversionValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.isPayForConversionEnabled)
        validationResult.check(testData.matcher())
    }

    companion object {
        data class TestData(
            val strategyType: StrategyName,
            val isPayForConversionEnabled: Boolean?,
            val goalId: Long? = null,
            val campaignType: CampaignType? = CampaignType.TEXT,
            val availableFeatures: Set<FeatureName> = emptySet(),
            val isValidatePayForConversionInAutobudgetAvgCpaPerFilter: Boolean = false,
            val defect: Defect<*>? = null
        ) {
            fun validationContainer() =
                PayForConversionValidator.Companion.ValidationContainer(
                    strategyType,
                    goalId,
                    campaignType,
                    availableFeatures,
                    isValidatePayForConversionInAutobudgetAvgCpaPerFilter
                )

            fun matcher(): Matcher<ValidationResult<Boolean?, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith<Boolean?>(
                    Matchers.validationError(
                        PathHelper.emptyPath(),
                        defect
                    )
                )
            } else {
                Matchers.hasNoDefectsDefinitions()
            }

            override fun toString(): String {
                return "TestData(strategyType=$strategyType, isPayForConversionEnabled=$isPayForConversionEnabled, goalId=$goalId, campaignType=$campaignType, availableFeatures=${availableFeatures.map { it.name }}, isValidatePayForConversionInAutobudgetAvgCpaPerFilter=$isValidatePayForConversionInAutobudgetAvgCpaPerFilter, defect=$defect)"
            }

        }
    }

}
