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
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class StartValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf("invalid start: null", TestData(start = null, defect = CommonDefects.notNull())),
        arrayOf(
            "invalid start: before campaign start date",
            TestData(
                start = NOW.minusDays(1),
                defect = StrategyDefects.strategyStartDateIsBeforeCampaignStartDate()
            )
        ),
        arrayOf("valid start", TestData(start = NOW))
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val validator = StartValidator(testData.campaigns)
        val validationResult = validator.apply(testData.start)

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

        data class TestData(
            val start: LocalDate?,
            val campaigns: List<WithDates> = listOf(
                CpmBannerCampaign()
                    .withType(CampaignType.CPM_BANNER)
                    .withStartDate(NOW)
                    .withEndDate(
                        NOW.plusDays(CampaignWithCustomStrategyValidator.MAX_STRATEGY_PERIOD_DAYS_COUNT.toLong())
                    )
            ),
            val defect: Defect<*>? = null
        )
    }
}
