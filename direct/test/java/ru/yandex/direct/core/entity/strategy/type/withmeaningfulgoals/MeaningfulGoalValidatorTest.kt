package ru.yandex.direct.core.entity.strategy.type.withmeaningfulgoals

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_CRR
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects.inCollection
import ru.yandex.direct.validation.defect.CommonDefects.isNull
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class MeaningfulGoalValidatorTest {

    fun testData(): Array<Array<TestData>> = arrayOf(
        //null goal_id and null conversionValue
        arrayOf(TestData(meaningFullGoal(), goalIdFieldDefect = notNull(), conversionValueFieldDefect = notNull())),
        //null goal_id
        arrayOf(TestData(meaningFullGoal(conversionValue = 10L), goalIdFieldDefect = notNull())),
        //is_metrika_source_value should be null
        arrayOf(
            TestData(
                meaningFullGoal(1L, false, 10L),
                strategyType = AUTOBUDGET_CRR,
                isMetrikaSourceValueFieldDefect = isNull()
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, true, 10L),
                strategyType = AUTOBUDGET_CRR,
                isMetrikaSourceValueFieldDefect = isNull()
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, false, 10L),
                allowMeaningfulGoalValueFromMetrika = false,
                isMetrikaSourceValueFieldDefect = isNull()
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, true, 10L),
                allowMeaningfulGoalValueFromMetrika = false,
                isMetrikaSourceValueFieldDefect = isNull()
            )
        ),
        //goal_id is not available
        arrayOf(TestData(meaningFullGoal(1L), goalIdFieldDefect = inCollection())),
        //conversion value not in range
        arrayOf(
            TestData(
                meaningFullGoal(conversionValue = CURRENCY.minPrice.toLong() - 1),
                conversionValueFieldDefect = inInterval(CURRENCY.minPrice, CURRENCY.maxAutobudget)
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(conversionValue = CURRENCY.maxAutobudget.toLong() + 1),
                conversionValueFieldDefect = inInterval(CURRENCY.minPrice, CURRENCY.maxAutobudget)
            )
        ),
        //valid meaningful goal
        arrayOf(
            TestData(
                meaningFullGoal(1L, conversionValue = CURRENCY.minPrice.toLong() + 1),
                availableGoalIds = setOf(1L)
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, conversionValue = CURRENCY.maxAutobudget.toLong() - 1),
                availableGoalIds = setOf(1L)
            )
        ),
        arrayOf(TestData(meaningFullGoal(1L, conversionValue = CURRENCY.maxAutobudget.toLong()), isCopy = true)),
        arrayOf(
            TestData(
                meaningFullGoal(1L, conversionValue = CURRENCY.maxAutobudget.toLong()),
                allGoalsAreAvailable = true
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, true, conversionValue = CURRENCY.minPrice.toLong() + 1),
                strategyType = AUTOBUDGET_CRR,
                allowMeaningfulGoalValueFromMetrika = true,
                availableGoalIds = setOf(1L)
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, true, conversionValue = CURRENCY.maxAutobudget.toLong() - 1),
                strategyType = AUTOBUDGET_CRR,
                allowMeaningfulGoalValueFromMetrika = true,
                availableGoalIds = setOf(1L)
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, false, conversionValue = CURRENCY.minPrice.toLong() + 1),
                strategyType = AUTOBUDGET_CRR,
                allowMeaningfulGoalValueFromMetrika = true,
                availableGoalIds = setOf(1L)
            )
        ),
        arrayOf(
            TestData(
                meaningFullGoal(1L, false, conversionValue = CURRENCY.maxAutobudget.toLong() - 1),
                strategyType = AUTOBUDGET_CRR,
                allowMeaningfulGoalValueFromMetrika = true,
                availableGoalIds = setOf(1L)
            )
        ),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        val validator = MeaningfulGoalValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.meaningfulGoal)
        if (!testData.hasDefects()) {
            validationResult.check(Matchers.hasNoDefectsDefinitions())
        }
        testData.conversionValueFieldDefect?.let {
            validationResult.check(matcher(it, MeaningfulGoal.CONVERSION_VALUE))
        }
        testData.goalIdFieldDefect?.let {
            validationResult.check(matcher(it, MeaningfulGoal.GOAL_ID))
        }
        testData.isMetrikaSourceValueFieldDefect?.let {
            validationResult.check(matcher(it, MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE))
        }
    }

    private fun <T> matcher(defect: Defect<T>, property: ModelProperty<MeaningfulGoal, *>) =
        Matchers.hasDefectDefinitionWith<MeaningfulGoal>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(property)),
                defect
            )
        )

    private fun meaningFullGoal(
        goalId: Long? = null,
        isMetrikaSourceValue: Boolean? = null,
        conversionValue: Long? = null
    ) =
        MeaningfulGoal()
            .withGoalId(goalId)
            .withIsMetrikaSourceOfValue(isMetrikaSourceValue)
            .withConversionValue(conversionValue?.toBigDecimal())

    companion object {
        private val CURRENCY = Currencies.getCurrency(CurrencyCode.RUB)

        data class TestData(
            val meaningfulGoal: MeaningfulGoal,
            val strategyType: StrategyName = StrategyName.AUTOBUDGET,
            val availableGoalIds: Set<Long> = emptySet(),
            val allowMeaningfulGoalValueFromMetrika: Boolean = false,
            val allGoalsAreAvailable: Boolean = false,
            val isCopy: Boolean = false,
            val goalIdFieldDefect: Defect<*>? = null,
            val isMetrikaSourceValueFieldDefect: Defect<*>? = null,
            val conversionValueFieldDefect: Defect<*>? = null
        ) {
            fun validationContainer() =
                MeaningfulGoalValidator.Companion.ValidationContainer(
                    CURRENCY,
                    availableGoalIds,
                    allGoalsAreAvailable,
                    allowMeaningfulGoalValueFromMetrika,
                    strategyType,
                    isCopy
                )

            fun hasDefects(): Boolean = goalIdFieldDefect != null ||
                isMetrikaSourceValueFieldDefect != null ||
                conversionValueFieldDefect != null
        }
    }
}
