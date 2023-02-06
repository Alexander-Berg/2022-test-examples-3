package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.enums.ReasonToBuyType
import ru.yandex.market.mapi.client.fapi.model.FapiProduct
import ru.yandex.market.mapi.client.fapi.util.ReasonToBuyUtils.formatForInlineWithoutBestByFactor
import kotlin.test.assertEquals

class ReasonToBuyUtilsTest {

    @Test
    fun statisticsTest() {
        val reasonsToBuy = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BOUGHT_N_TIMES,
                value = 6745.toDouble(),
                factorPriority = 1.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.VIEWED_N_TIMES,
                value = 5000000.toDouble(),
                factorPriority = 2.0
            )
        )
        val result = reasonsToBuy.formatForInlineWithoutBestByFactor(2, false)
        assertEquals(
            message = "Не показываем viewed_n_times, если есть bought_n_times",
            expected = listOf(
                "6 745 покупок за 2 месяца"
            ),
            actual = result
        )

        val boughtNTimesReasonToBuy = listOf(
            FapiProduct.ReasonToBuy(id = ReasonToBuyType.BOUGHT_N_TIMES, value = 6745.toDouble()),
            FapiProduct.ReasonToBuy(id = ReasonToBuyType.BOUGHT_N_TIMES, value = 304.toDouble()),
            FapiProduct.ReasonToBuy(id = ReasonToBuyType.BOUGHT_N_TIMES, value = 5050505.toDouble())
        )
        val boughtNTimesResult = boughtNTimesReasonToBuy.formatForInlineWithoutBestByFactor(3, false)
        assertEquals(
            message = "Проверяем форматтирование для bought_n_times",
            expected = listOf(
                "6 745 покупок за 2 месяца",
                "304 покупки за 2 месяца",
                "5 050 505 покупок за 2 месяца"
            ),
            actual = boughtNTimesResult
        )

        val viewedNTimesReasonToBuy = listOf(
            FapiProduct.ReasonToBuy(id = ReasonToBuyType.VIEWED_N_TIMES, value = 6745.toDouble()),
            FapiProduct.ReasonToBuy(id = ReasonToBuyType.VIEWED_N_TIMES, value = 304.toDouble()),
            FapiProduct.ReasonToBuy(id = ReasonToBuyType.VIEWED_N_TIMES, value = 5050505.toDouble())
        )

        val viewedNTimesResult = viewedNTimesReasonToBuy.formatForInlineWithoutBestByFactor(3, false)
        assertEquals(
            message = "Проверяем форматтирование для viewed_n_times",
            expected = listOf(
                "6 745 человек интересовались за 2 месяца",
                "304 человека интересовались за 2 месяца",
                "5 050 505 человек интересовались за 2 месяца"
            ),
            actual = viewedNTimesResult
        )
    }

    @Test
    fun withoutBestByFactorTest() {
        val reasonsToBuy = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BOUGHT_N_TIMES,
                value = 6745.toDouble()
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BEST_BY_FACTOR,
                value = 0.9863013625,
                factorName = "Удобство использования",
                factorPriority = 1.toDouble()
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.HYPE_GOODS,
                value = 1.toDouble()
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.CUSTOMERS_CHOICE,
                value = 0.9307432175
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BESTSELLER,
                value = 0.2333333343
            )
        )

        val result = reasonsToBuy.formatForInlineWithoutBestByFactor(2, false)
        assertEquals(
            message = "Проверяем, что reason с типом best_by_factor, hype_goods, bestseller не отображается",
            expected = listOf(
                "6 745 покупок за 2 месяца",
                "93% рекомендуют"
            ),
            actual = result
        )
    }

    @Test
    fun testAddViewedNTimesWhenNoHyperText() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.VIEWED_N_TIMES,
                value = 7.0
            )
        ).formatForInlineWithoutBestByFactor(2, false)
        assertEquals(listOf("7 человек интересовались за 2 месяца"), result)
    }

    @Test
    fun testAddViewedNTimesWhenWithHyperText() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.VIEWED_N_TIMES,
                value = 7.0
            )
        ).formatForInlineWithoutBestByFactor(2, true)
        assertEquals(emptyList(), result)
    }

    @Test
    fun testVisibleReasonCountSkipCustomerChoice() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.VIEWED_N_TIMES,
                value = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.CUSTOMERS_CHOICE,
                value = 7.0
            )
        ).formatForInlineWithoutBestByFactor(1, false)
        assertEquals(listOf("7 человек интересовались за 2 месяца"), result)
    }

    @Test
    fun testVisibleReasonCountAddCustomerChoice() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.VIEWED_N_TIMES,
                value = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.CUSTOMERS_CHOICE,
                value = 7.0
            )
        ).formatForInlineWithoutBestByFactor(2, false)
        assertEquals(listOf("7 человек интересовались за 2 месяца", "700% рекомендуют"), result)
    }

    @Test
    fun testVisibleBestByFactor() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.CUSTOMERS_CHOICE,
                value = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BEST_BY_FACTOR,
                factorName = "ReasonToBuyType.BEST_BY_FACTOR 1",
            )
        ).formatForInlineWithoutBestByFactor(3, true)
        assertEquals(listOf("700% рекомендуют", "Нравится reasontobuytype.best_by_factor 1"), result)
    }

    @Test
    fun testVisibleBestByFactorByOrder() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.CUSTOMERS_CHOICE,
                value = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BEST_BY_FACTOR,
                factorName = "ReasonToBuyType.BEST_BY_FACTOR 1",
                factorPriority = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BEST_BY_FACTOR,
                factorName = "ReasonToBuyType.BEST_BY_FACTOR 2",
                factorPriority = 1.0
            )
        ).formatForInlineWithoutBestByFactor(3, true)
        assertEquals(
            listOf(
                "700% рекомендуют",
                "Нравятся reasontobuytype.best_by_factor 2 и reasontobuytype.best_by_factor 1"
            ), result
        )
    }

    @Test
    fun testVisibleBestByFactorByOrderLimitedVisibleReasonToBy() {
        val result = listOf(
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.CUSTOMERS_CHOICE,
                value = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BEST_BY_FACTOR,
                factorName = "ReasonToBuyType.BEST_BY_FACTOR 1",
                factorPriority = 7.0
            ),
            FapiProduct.ReasonToBuy(
                id = ReasonToBuyType.BEST_BY_FACTOR,
                factorName = "ReasonToBuyType.BEST_BY_FACTOR 2",
                factorPriority = 1.0
            )
        ).formatForInlineWithoutBestByFactor(2, true)
        assertEquals(listOf("700% рекомендуют", "Нравится reasontobuytype.best_by_factor 2"), result)
    }
}
