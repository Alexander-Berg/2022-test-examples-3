package ru.yandex.direct.core.copyentity.preprocessors.campaign

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.defaultBetweenShardsCopyContainer
import ru.yandex.direct.core.entity.currency.service.CurrencyConverterFactory
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import java.math.BigDecimal

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CampaignWithDayBudgetCopyPreprocessorTest {

    private lateinit var preprocessor: CampaignWithDayBudgetCopyPreprocessor

    @Before
    fun before() {
        val currencyRateService = mock<CurrencyRateService>()
        val currencyConverterFactory = CurrencyConverterFactory(currencyRateService)

        whenever(currencyRateService.convertMoney(any(), any())).then {
            val money: Money = it.arguments[0] as Money
            val currency: CurrencyCode = it.arguments[1] as CurrencyCode
            Money.valueOf(money.bigDecimalValue() * BigDecimal.TEN, currency)
        }

        preprocessor = CampaignWithDayBudgetCopyPreprocessor(currencyConverterFactory)
    }

    fun dayBudgetParams() = arrayOf(
        arrayOf(
            "conversion",
            BigDecimal.valueOf(4_200),
            BigDecimal.valueOf(42_000)
        ),
        arrayOf(
            "less than min",
            BigDecimal.valueOf(29),
            Currencies
                .getCurrency(CurrencyCode.RUB)
                .minDayBudget,
        ),
        arrayOf(
            "more then max",
            BigDecimal.valueOf(1000000000),
            Currencies
                .getCurrency(CurrencyCode.RUB)
                .maxDailyBudgetAmount,
        ),
    )

    @Test
    @TestCaseName("{method}: {0}")
    @Parameters(method = "dayBudgetParams")
    fun testDayBudget(
        caseName: String,
        dayBudget: BigDecimal,
        expectedDayBudget: BigDecimal,
    ) {
        val campaign = fullTextCampaign()
            .withCurrency(CurrencyCode.USD)
            .withDayBudget(dayBudget)
        val copyContainer = defaultBetweenShardsCopyContainer(
            currencyFrom = CurrencyCode.USD,
            currencyTo = CurrencyCode.RUB,
        )

        preprocessor.preprocess(campaign, copyContainer)

        assertThat(campaign.dayBudget).isEqualTo(expectedDayBudget)
    }
}
