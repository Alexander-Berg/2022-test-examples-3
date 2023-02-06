package ru.yandex.direct.core.testing.data

import ru.yandex.direct.core.entity.feedfilter.converter.FeedFilterConverters
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterCondition
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterTab
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange
import ru.yandex.direct.core.entity.performancefilter.container.Exists
import ru.yandex.direct.core.entity.performancefilter.model.Operator
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault

class TestFeedFilters private constructor() {
    companion object {
        @JvmField
        val CONDITIONS: List<FeedFilterCondition<Any>> = with(FeedFilterConverters.DB_CONDITION_FACTORY) {
            listOf(
                create("categoryId", Operator.EQUALS, listOf(1L, 2L, 3L)),
                create("vendor", Operator.CONTAINS, listOf("ex", "yan")),
                create("model", Operator.NOT_CONTAINS, listOf("plus")),
                create("price", Operator.RANGE, listOf(DecimalRange("33.09-70.92"))),
                create("adult", Operator.EQUALS, listOf("1")),
                create("age", Operator.EQUALS, listOf("16")),
                create("market_category", Operator.EXISTS, Exists(true)),
                create("oldprice", Operator.GREATER, listOf(63.88)),
                create("available", Operator.EQUALS, true)
            )
        }

        @JvmField
        val FILTER_ALL_PRODUCTS: FeedFilter =
            FeedFilter().withTab(FeedFilterTab.ALL_PRODUCTS).withConditions(emptyList())

        @JvmField
        val FILTER_ONE_CONDITION: FeedFilter =
            FeedFilter().withTab(FeedFilterTab.CONDITION).withConditions(listOf(CONDITIONS.first()))


        @JvmField
        val FILTER_MANY_CONDITIONS: FeedFilter =
            FeedFilter().withTab(FeedFilterTab.CONDITION).withConditions(CONDITIONS)

        @JvmField
        val SCHEMA: FilterSchema = PerformanceDefault()
    }
}
