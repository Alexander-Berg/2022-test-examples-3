package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.time.LocalDate
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.WithDates
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignWithCustomStrategyValidator
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.DateDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
class FinishValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf(
            "valid finish",
            TestData(finish = DEFAULT_START.plusDays(1))
        ),
        arrayOf("invalid finish: null", TestData(finish = null, defect = CommonDefects.notNull())),
        arrayOf(
            "invalid finish: after the campaign end date",
            TestData(
                finish = DEFAULT_END.plusDays(1),
                defect = StrategyDefects.strategyEndDateIsAfterCampaignEndDate()
            )
        ),
        arrayOf(
            "invalid finish: before the start date",
            TestData(
                finish = DEFAULT_START.minusDays(1),
                defect = DateDefects.endDateMustBeGreatherThanOrEqualToStartDate()
            )
        ),
        arrayOf(
            "invalid finish: strategy period days count less than min",
            TestData(
                finish = DEFAULT_START,
                defect = StrategyDefects.strategyPeriodDaysCountLessThanMin(CampaignWithCustomStrategyValidator.MIN_STRATEGY_PERIOD_DAYS_COUNT)
            )
        ),
        arrayOf(
            "invalid finish: strategy period days count more than max",
            TestData(
                finish = DEFAULT_START.plusDays(CampaignWithCustomStrategyValidator.MAX_STRATEGY_PERIOD_DAYS_COUNT.toLong() + 1),
                defect = StrategyDefects.strategyPeriodDaysCountMoreThanMax(CampaignWithCustomStrategyValidator.MAX_STRATEGY_PERIOD_DAYS_COUNT),
            )
        )
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val validator = FinishValidator(testData.now, testData.start, testData.campaigns)
        val validationResult = validator.apply(testData.finish)

        testData.defect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(PathHelper.emptyPath(), it)
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }

    companion object {
        private val NOW = LocalDate.now()
        private val DEFAULT_START = NOW
        private val DEFAULT_END = NOW.plusDays(7)

        data class TestData(
            val finish: LocalDate?,
            val start: LocalDate = DEFAULT_START,
            val now: LocalDate = NOW,
            val campaigns: List<WithDates> = listOf(
                CpmBannerCampaign()
                    .withType(CampaignType.CPM_BANNER)
                    .withStartDate(DEFAULT_START)
                    .withEndDate(DEFAULT_END)
            ),
            val defect: Defect<*>? = null
        )
    }
}
