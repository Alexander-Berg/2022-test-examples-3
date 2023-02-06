package ru.yandex.direct.core.entity.strategy.type.withavgcpa

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CommonStrategyValidatorConstants
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.PerformanceStrategyValidatorConstants
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class AvgCpaValidatorTest {

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)
    private val performanceCampaignConstants = PerformanceStrategyValidatorConstants(currency)
    private val commonCampaignConstants = CommonStrategyValidatorConstants(currency)

    fun testData(): List<List<Any?>> = listOf(
        listOf(CampaignType.TEXT, commonCampaignConstants.minAvgCpa, null),
        listOf(
            CampaignType.TEXT,
            commonCampaignConstants.minAvgCpa.minus(BigDecimal.TEN),
            greaterThanOrEqualTo(commonCampaignConstants.minAvgCpa)
        ),
        listOf(CampaignType.TEXT, null, notNull()),
        listOf(null, commonCampaignConstants.minAvgCpa, null),
        listOf(
            null,
            commonCampaignConstants.minAvgCpa.minus(BigDecimal.TEN),
            greaterThanOrEqualTo(commonCampaignConstants.minAvgCpa)
        ),
        listOf(null, null, notNull()),
        listOf(CampaignType.PERFORMANCE, performanceCampaignConstants.minAvgCpa, null),
        listOf(
            CampaignType.PERFORMANCE,
            performanceCampaignConstants.minAvgCpa.minus(BigDecimal.TEN),
            greaterThanOrEqualTo(performanceCampaignConstants.minAvgCpa)
        ),
        listOf(CampaignType.PERFORMANCE, null, notNull())
    )


    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun `validation is correct`(campaignType: CampaignType?, avgCpa: BigDecimal?, defect: Defect<*>?) {
        val validationContainer = AvgCpaValidator.Companion.ValidationContainer(
            currency,
            campaignType
        )

        val validator = AvgCpaValidator(validationContainer)

        val validationResult = validator.apply(avgCpa)

        val matcher: Matcher<ValidationResult<BigDecimal?, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<BigDecimal?>(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }
}
