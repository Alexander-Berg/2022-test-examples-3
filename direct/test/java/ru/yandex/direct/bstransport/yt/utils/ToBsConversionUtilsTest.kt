package ru.yandex.direct.bstransport.yt.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_BANNER
import ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.currency.Percent
import ru.yandex.direct.currency.currencies.CurrencyByn
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.currency.currencies.CurrencyTry
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToBsConversionUtilsTest {
    val RUB = CurrencyRub.getInstance()
    val TRY = CurrencyTry.getInstance()
    val BYN = CurrencyByn.getInstance()

    @ParameterizedTest(name="[{index}] {displayName}")
    @MethodSource("roundParameters")
    fun testConversionOk(sum: BigDecimal, currency: Currency, campaignType: CampaignType, expected: Int) {
        assertThat(ToBsConversionUtils.bsPreparePrice(sum, currency, campaignType))
            .isEqualTo(expected)
    }

    fun roundParameters() =
        listOf(
            Arguments.of("2.21", RUB, TEXT, 22000),
            Arguments.of("12.3456", RUB, TEXT, 123000),
            Arguments.of("0", RUB, TEXT, 0),
            Arguments.of("1", RUB, TEXT, 10000),
            Arguments.of("-2.21", RUB, TEXT, -22000),
            Arguments.of("-2.21", RUB, CPM_BANNER, -22),
            Arguments.of("2.21", TRY, TEXT, 2210000),
            Arguments.of("0.25", BYN, CPM_BANNER, 3),
        )

    @ParameterizedTest(name="[{index}] {displayName}")
    @MethodSource("performanceParameters")
    fun testBsMoneyForPerformance(p: PerfParams) {
        assertThat(
            ToBsConversionUtils.bsMoneyForPerformance(
                p.value?.toBigDecimal(),
                p.campValue?.toBigDecimal(),
                p.currency,
                p.nds?.let { Percent.fromPercent(it.toBigDecimal()) }
            )
        ).isEqualTo(p.expected)
    }

    data class PerfParams(
        val value: String?,
        val campValue: String?,
        val currency: Currency,
        val nds: Int?,
        val expected: Int
    )

    fun performanceParameters(): List<PerfParams> {
        return listOf(
            PerfParams("1", null, RUB, 13, 11_300),
            PerfParams("1", null, RUB, null, 10_000),
            PerfParams("1", null, RUB, 0, 10_000),
            PerfParams(null, "3", TRY, 15, 3_450_000),
        )
    }
}
