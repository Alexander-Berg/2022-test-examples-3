package ru.yandex.market.contentmapping.benchmark.state

import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import ru.yandex.market.contentmapping.benchmark.helper.ResourceLoadHelper
import ru.yandex.market.contentmapping.benchmark.mock.ComplexCategoryDataServiceMock
import ru.yandex.market.contentmapping.services.ShopModelRatingService
import ru.yandex.market.contentmapping.services.ShopModelRequiredParamsRatingCalculatorService
import ru.yandex.market.contentmapping.services.rules.MarketValuesResolver

@State(Scope.Benchmark)
open class ShopModelRatingServiceState {
    var shopModelRatingService: ShopModelRatingService? = null

    @Setup
    open fun setup() {
        println("building ShopModelRatingService")
        shopModelRatingService = ShopModelRatingService(
                ComplexCategoryDataServiceMock(ResourceLoadHelper.loadData(RuleEngineServiceState.resourceUrl, RuleEngineServiceState.typeReference)),
                ShopModelRequiredParamsRatingCalculatorService(MarketValuesResolver())

        )
        println("building ShopModelRatingService complete")
    }

    @TearDown
    open fun shutdown() {
        shopModelRatingService = null
    }
}
