package ru.yandex.direct.core.entity.strategy.type.withavgcpa

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
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
import ru.yandex.direct.core.entity.strategy.container.AbstractStrategyOperationContainer
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class StrategyWithAvgCpaValidatorProviderTest {

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)
    private val performanceCampaignConstants = PerformanceStrategyValidatorConstants(currency)
    private val commonCampaignConstants = CommonStrategyValidatorConstants(currency)

    fun testData(): List<List<Any?>> = listOf(
        listOf(CampaignType.TEXT, commonCampaignConstants.minAvgCpa, null),
        listOf(
            CampaignType.TEXT,
            commonCampaignConstants.minAvgCpa.minus(BigDecimal.TEN),
            NumberDefects.greaterThanOrEqualTo(commonCampaignConstants.minAvgCpa)
        ),
        listOf(CampaignType.TEXT, null, CommonDefects.notNull()),
        listOf(null, commonCampaignConstants.minAvgCpa, null),
        listOf(
            null,
            commonCampaignConstants.minAvgCpa.minus(BigDecimal.TEN),
            NumberDefects.greaterThanOrEqualTo(commonCampaignConstants.minAvgCpa)
        ),
        listOf(null, null, CommonDefects.notNull()),
        listOf(CampaignType.PERFORMANCE, performanceCampaignConstants.minAvgCpa, null),
        listOf(
            CampaignType.PERFORMANCE,
            performanceCampaignConstants.minAvgCpa.minus(BigDecimal.TEN),
            NumberDefects.greaterThanOrEqualTo(performanceCampaignConstants.minAvgCpa)
        ),
        listOf(CampaignType.PERFORMANCE, null, CommonDefects.notNull())
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaignType={0}, avgCpa={1}")
    fun `validation is correct`(campaignType: CampaignType?, avgCpa: BigDecimal?, defect: Defect<*>?) {
        val container = mockContainer(campaignType, currency)
        val validator = StrategyWithAvgCpaValidatorProvider.createStrategyValidator(container)
        val strategy = autobudgetAvgCpa()
            .withAvgCpa(avgCpa)

        val validationResult = validator.apply(strategy)

        val matcher: Matcher<ValidationResult<StrategyWithAvgCpa, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<StrategyWithAvgCpa>(
                Matchers.validationError(
                    PathHelper.path(field(StrategyWithAvgCpa.AVG_CPA)),
                    defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }

    private fun mockContainer(
        campaignType: CampaignType?,
        currency: Currency
    ): AbstractStrategyOperationContainer {
        val m = mock<AbstractStrategyOperationContainer>()
        whenever(m.campaignType(anyOrNull())).thenReturn(campaignType)
        whenever(m.currency).thenReturn(currency)
        return m
    }
}
