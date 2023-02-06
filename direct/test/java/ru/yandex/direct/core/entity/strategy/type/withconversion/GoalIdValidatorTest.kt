package ru.yandex.direct.core.entity.strategy.type.withconversion

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.MCB
import ru.yandex.direct.core.entity.campaign.model.CampaignType.MOBILE_CONTENT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BY_ALL_GOALS_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.allGoalsOptimizationProhibited
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPI
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_CRR
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_ROI
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.SOCIAL_ADVERTISING
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class GoalIdValidatorTest {

    private val metrikaGoal = defaultGoalByType(GoalType.GOAL)
    private val anotherMetrikaGoal = defaultGoalByType(GoalType.GOAL)
    private val mobileGoal = setIsMobileGoal(defaultGoalByType(GoalType.MOBILE))
    private val anotherMobileGoal = setIsMobileGoal(defaultGoalByType(GoalType.MOBILE))

    fun testData(): Array<Array<TestData>> = arrayOf(
        //допустимые кейсы goal_id == null
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, null)),
        arrayOf(TestData(AUTOBUDGET, null)),
        arrayOf(TestData(AUTOBUDGET, null, campaignType = MOBILE_CONTENT)),
        //goal_id == null недопустимо
        arrayOf(TestData(AUTOBUDGET, null, campaignType = CampaignType.PERFORMANCE, defect = notNull())),
        arrayOf(TestData(AUTOBUDGET_CRR, null, defect = notNull())),
        arrayOf(TestData(AUTOBUDGET_ROI, null, defect = notNull())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, null, defect = notNull())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, null, defect = notNull())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, null, defect = notNull())),
        //not available goal
        arrayOf(TestData(AUTOBUDGET_CRR, 1L, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, 1L, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET, 1L, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_ROI, 1L, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, 1L, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, 1L, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, 1L, defect = objectNotFound())),
        //inconsistentStrategyToCampaignType
        arrayOf(
            TestData(
                AUTOBUDGET,
                metrikaGoal.id,
                availableGoals = setOf(metrikaGoal),
                availableFeatures = setOf(SOCIAL_ADVERTISING),
                defect = inconsistentStrategyToCampaignType()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET,
                metrikaGoal.id,
                availableGoals = setOf(metrikaGoal),
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                availableFeatures = setOf(SOCIAL_ADVERTISING),
                defect = inconsistentStrategyToCampaignType()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET,
                null,
                availableFeatures = setOf(SOCIAL_ADVERTISING)
            )
        ),
        //metrika_goals available goal
        arrayOf(TestData(AUTOBUDGET_CRR, metrikaGoal.id, availableGoals = setOf(metrikaGoal))),
        arrayOf(TestData(AUTOBUDGET_ROI, metrikaGoal.id, availableGoals = setOf(metrikaGoal))),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, metrikaGoal.id, availableGoals = setOf(metrikaGoal))),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, metrikaGoal.id, availableGoals = setOf(metrikaGoal))),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, metrikaGoal.id, availableGoals = setOf(metrikaGoal))),

        //mobile goal available
        arrayOf(TestData(AUTOBUDGET_AVG_CPI, mobileGoal.id, availableGoals = setOf(mobileGoal))),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPI,
                mobileGoal.id,
                availableGoals = setOf(anotherMobileGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                mobileGoal.id,
                campaignType = null,
                availableGoals = setOf(mobileGoal)
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_ROI,
                metrikaGoal.id,
                availableGoals = setOf(anotherMetrikaGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                metrikaGoal.id,
                availableGoals = setOf(anotherMetrikaGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                metrikaGoal.id,
                availableGoals = setOf(anotherMetrikaGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_FILTER,
                metrikaGoal.id,
                availableGoals = setOf(anotherMetrikaGoal),
                defect = objectNotFound()
            )
        ),
        //BY_ALL_GOALS_GOAL_ID prohibited
        arrayOf(TestData(AUTOBUDGET_CRR, BY_ALL_GOALS_GOAL_ID, defect = allGoalsOptimizationProhibited())),
        arrayOf(TestData(AUTOBUDGET, BY_ALL_GOALS_GOAL_ID, defect = allGoalsOptimizationProhibited())),
        arrayOf(TestData(AUTOBUDGET_ROI, BY_ALL_GOALS_GOAL_ID, defect = allGoalsOptimizationProhibited())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, BY_ALL_GOALS_GOAL_ID, defect = allGoalsOptimizationProhibited())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, BY_ALL_GOALS_GOAL_ID, defect = allGoalsOptimizationProhibited())),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_FILTER,
                BY_ALL_GOALS_GOAL_ID,
                defect = allGoalsOptimizationProhibited()
            )
        ),
        //MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID is allowable
        arrayOf(TestData(AUTOBUDGET_CRR, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)),
        arrayOf(TestData(AUTOBUDGET_ROI, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)),
        //mobile_content campaign type not available mobile goals
        arrayOf(
            TestData(
                AUTOBUDGET_CRR,
                metrikaGoal.id,
                campaignType = MOBILE_CONTENT,
                availableGoals = setOf(metrikaGoal, mobileGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET,
                metrikaGoal.id,
                campaignType = MOBILE_CONTENT,
                availableGoals = setOf(metrikaGoal, mobileGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_ROI,
                metrikaGoal.id,
                campaignType = MOBILE_CONTENT,
                availableGoals = setOf(metrikaGoal, mobileGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA,
                metrikaGoal.id,
                campaignType = MOBILE_CONTENT,
                availableGoals = setOf(metrikaGoal, mobileGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                metrikaGoal.id,
                campaignType = MOBILE_CONTENT,
                availableGoals = setOf(metrikaGoal, mobileGoal),
                defect = objectNotFound()
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_FILTER,
                metrikaGoal.id,
                campaignType = MOBILE_CONTENT,
                availableGoals = setOf(metrikaGoal, mobileGoal),
                defect = objectNotFound()
            )
        ),
        //all metrika goals available isCopy==true
        arrayOf(TestData(AUTOBUDGET_CRR, metrikaGoal.id, isCopy = true)),
        arrayOf(TestData(AUTOBUDGET_ROI, metrikaGoal.id, isCopy = true)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, metrikaGoal.id, isCopy = true)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, metrikaGoal.id, isCopy = true)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, metrikaGoal.id, isCopy = true)),
        //all metrika goals available isRequestFromInternalNetwork==true
        arrayOf(TestData(AUTOBUDGET_CRR, metrikaGoal.id, isRequestFromInternalNetwork = true)),
        arrayOf(TestData(AUTOBUDGET_ROI, metrikaGoal.id, isRequestFromInternalNetwork = true)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, metrikaGoal.id, isRequestFromInternalNetwork = true)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, metrikaGoal.id, isRequestFromInternalNetwork = true)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, metrikaGoal.id, isRequestFromInternalNetwork = true)),
        //metrika goals not allowable for unsupported campaign types
        arrayOf(TestData(AUTOBUDGET_CRR, metrikaGoal.id, campaignType = MCB, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_ROI, metrikaGoal.id, campaignType = MCB, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, metrikaGoal.id, campaignType = MCB, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_CAMP, metrikaGoal.id, campaignType = MCB, defect = objectNotFound())),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA_PER_FILTER, metrikaGoal.id, campaignType = MCB, defect = objectNotFound())),
        //metrika goals  allowable for campaign_type == null
        arrayOf(TestData(AUTOBUDGET_CRR, metrikaGoal.id, availableGoals = setOf(metrikaGoal), campaignType = null)),
        arrayOf(TestData(AUTOBUDGET_ROI, metrikaGoal.id, availableGoals = setOf(metrikaGoal), campaignType = null)),
        arrayOf(TestData(AUTOBUDGET_AVG_CPA, metrikaGoal.id, availableGoals = setOf(metrikaGoal), campaignType = null)),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_CAMP,
                metrikaGoal.id,
                availableGoals = setOf(metrikaGoal),
                campaignType = null
            )
        ),
        arrayOf(
            TestData(
                AUTOBUDGET_AVG_CPA_PER_FILTER,
                metrikaGoal.id,
                availableGoals = setOf(metrikaGoal),
                campaignType = null
            )
        ),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun `goal id validation is correct`(testData: TestData) {
        val validator = GoalIdValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.goalId)
        assertThat(validationResult).`is`(Conditions.matchedBy(testData.matcher()))
    }

    private fun setIsMobileGoal(goal: Goal): Goal {
        goal.isMobileGoal = true
        return goal
    }

    companion object {
        data class TestData(
            val strategyType: StrategyName,
            val goalId: Long?,
            val availableGoals: Set<Goal> = emptySet(),
            val campaignType: CampaignType? = CampaignType.TEXT,
            val availableFeatures: Set<FeatureName> = emptySet(),
            val isRequestFromInternalNetwork: Boolean = false,
            val isCopy: Boolean = false,
            val defect: Defect<*>? = null
        ) {
            fun validationContainer() =
                GoalIdValidator.Companion.ValidationContainer(
                    availableGoals,
                    campaignType,
                    strategyType,
                    availableFeatures,
                    isRequestFromInternalNetwork,
                    isCopy
                )

            fun matcher(): Matcher<ValidationResult<Long?, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith<Long?>(
                    Matchers.validationError(
                        PathHelper.emptyPath(),
                        defect
                    )
                )
            } else {
                Matchers.hasNoDefectsDefinitions()
            }

            override fun toString(): String {
                return "TestData(campaignType=$campaignType, availableFeatures=$availableFeatures, isRequestFromInternalNetwork=$isRequestFromInternalNetwork, isCopy=$isCopy, defect=$defect, strategyType=$strategyType)"
            }
        }
    }
}
