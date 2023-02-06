package ru.yandex.direct.core.entity.strategy.type.withmeaningfulgoals

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_MAX_COUNT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.incorrectSetOfMobileGoals
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.unableToUseCurrentMeaningfulGoalsForOptimization
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.model.OsType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement
import ru.yandex.direct.validation.defect.CollectionDefects.inCollection
import ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class MeaningfulGoalsValidatorTest {

    fun testData(): Array<Array<TestData>> = arrayOf(
        //MEANINGFUL_GOALS_MAX_COUNT exceeded
        arrayOf(
            TestData(
                List(MEANINGFUL_GOALS_MAX_COUNT + 1) { i -> validMeaningFullGoal(i.toLong()) },
                defect = maxCollectionSize(MEANINGFUL_GOALS_MAX_COUNT)
            )
        ),
        //not unique goal ids
        arrayOf(
            TestData(
                listOf(validMeaningFullGoal(1L), validMeaningFullGoal(1L)),
                defect = duplicatedElement(),
                defectPath = path(index(0))
            )
        ),
        //not available goal
        arrayOf(
            TestData(
                listOf(validMeaningFullGoal(1L)),
                defect = inCollection(),
                defectPath = path(index(0), field(MeaningfulGoal.GOAL_ID))
            )
        ),
        //unableToUseCurrentMeaningfulGoalsForOptimization
        arrayOf(
            TestData(
                listOf(validMeaningFullGoal(ENGAGED_SESSION_GOAL_ID)),
                defect = unableToUseCurrentMeaningfulGoalsForOptimization(),
                strategyGoalId = MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
            )
        ),
        arrayOf(
            TestData(
                null,
                defect = unableToUseCurrentMeaningfulGoalsForOptimization(),
                strategyGoalId = MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
            )
        ),
        //incorrectSetOfMobileGoals
        arrayOf(
            TestData(
                listOf(validMeaningFullGoal(mobileGoal.id)),
                goals = setOf(mobileGoal),
                mobileContents = listOf(mobileContentIOS, mobileContentIOS),
                defect = incorrectSetOfMobileGoals()
            )
        ),
        //validation success
        arrayOf(
            TestData(
                listOf(validMeaningFullGoal(mobileGoal.id)),
                goals = setOf(mobileGoal),
                mobileContents = listOf(mobileContentIOS, mobileContentAndroid)
            )
        ),
        arrayOf(TestData(listOf(validMeaningFullGoal(goal.id)), goals = setOf(goal))),
        arrayOf(TestData(listOf(validMeaningFullGoal(ENGAGED_SESSION_GOAL_ID)))),
        arrayOf(TestData(emptyList(), currency = null))
    )

    private val goal = defaultGoalByType(GoalType.GOAL)
    private val mobileAppId = RandomNumberUtils.nextPositiveLong()
    private val mobileGoal = defaultGoalByType(GoalType.MOBILE)
        .withIsMobileGoal(true)
        .withMobileAppId(mobileAppId) as Goal

    private val mobileContentIOS = MobileContent().withOsType(OsType.IOS)
    private val mobileContentAndroid = MobileContent().withOsType(OsType.ANDROID)

    private fun validMeaningFullGoal(goalId: Long) =
        MeaningfulGoal()
            .withGoalId(goalId)
            .withConversionValue(10L.toBigDecimal())

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun `meaningful goal validation is correct`(testData: TestData) {
        val validator = MeaningfulGoalsValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.meaningfulGoals)
        validationResult.check(testData.matcher())
    }

    companion object {
        data class TestData(
            val meaningfulGoals: List<MeaningfulGoal>?,
            val currency: Currency? = Currencies.getCurrency(CurrencyCode.RUB),
            val availableFeatures: Set<FeatureName> = setOf(),
            val goals: Set<Goal> = emptySet(),
            val isMeaningfulGoalsOptimizationSelected: Boolean = false,
            val mobileContents: List<MobileContent> = emptyList(),
            val strategyGoalId: Long? = null,
            val defect: Defect<*>? = null,
            val defectPath: Path? = null,
        ) {
            fun validationContainer() =
                MeaningfulGoalsValidator.Companion.ValidationContainer(
                    currency,
                    goals,
                    availableFeatures,
                    StrategyName.AUTOBUDGET,
                    { _ -> mobileContents },
                    false,
                    false,
                    strategyGoalId
                )

            fun matcher(): Matcher<ValidationResult<List<MeaningfulGoal>, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(
                        defectPath ?: PathHelper.emptyPath(),
                        defect
                    )
                )
            } else {
                Matchers.hasNoDefectsDefinitions()
            }

            override fun toString(): String {
                return "TestData(currency=$currency,isMeaningfulGoalsOptimizationSelected=$isMeaningfulGoalsOptimizationSelected)"
            }
        }
    }
}
