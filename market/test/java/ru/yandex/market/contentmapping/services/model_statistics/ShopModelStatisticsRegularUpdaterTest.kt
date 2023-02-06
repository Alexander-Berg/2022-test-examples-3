package ru.yandex.market.contentmapping.services.model_statistics

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import ru.yandex.market.contentmapping.modules.keyvalue.KeyValueService
import ru.yandex.market.contentmapping.repository.ShopModelRepository
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.testdata.TestDataUtils
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class ShopModelStatisticsRegularUpdaterTest : BaseAppTestClass() {
    val shopModelStatisticsRegularUpdater: ShopModelStatisticsRegularUpdater by bean()
    val shopRepository: ShopRepository by bean()
    val shopModelRepository: ShopModelRepository by bean()
    val keyValueService: KeyValueService by bean()

    @Before
    fun setup() {
        shopRepository.insert(TestDataUtils.activeShop(1))
        shopRepository.insert(TestDataUtils.activeShop(2))

        shopModelRepository.insert(TestDataUtils.nextShopModel().copy(shopId = 1))
        shopModelRepository.insert(TestDataUtils.nextShopModel().copy(shopId = 2))
    }

    @Test
    fun `test it updates stat`() {
        shopModelStatisticsRegularUpdater.recalculateStat() shouldBe 2
    }

    @Test
    fun `test it doesn't update if did it already today`() {
        shopModelStatisticsRegularUpdater.recalculateStat() shouldBe 2
        shopModelStatisticsRegularUpdater.recalculateStat() shouldBe 0
    }

    @Test
    fun `test it updates from set shop`() {
        shopModelStatisticsRegularUpdater.recalculateStat() shouldBe 2
        keyValueService["ShopModelStatisticsRegularUpdater.lastShopId"] = 1L
        shopModelStatisticsRegularUpdater.recalculateStat() shouldBe 1
    }
}
