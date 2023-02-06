package ru.yandex.market.contentmapping.benchmark

import org.openjdk.jmh.infra.Blackhole
import ru.yandex.market.contentmapping.benchmark.state.ModelsState
import ru.yandex.market.contentmapping.benchmark.state.ShopModelRatingServiceState

class ShopModelRatingServiceRunner {
    companion object {
        @JvmStatic
        fun calculateRating(
                modelsState: ModelsState,
                shopModelRatingServiceState: ShopModelRatingServiceState,
                blackhole: Blackhole,
        ) {
            val shopModelRatingService = shopModelRatingServiceState.shopModelRatingService
                    ?: throw IllegalStateException("no shopModelRatingServiceState")
            modelsState.shopModels.asSequence()
                    .map { shopModelRatingService.calculateRating(it) }
                    .forEach { blackhole.consume(it) }
        }
    }
}
