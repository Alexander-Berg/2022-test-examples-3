package ru.yandex.market.contentmapping.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.dto.model.statistics.ShopModelStatisticsItem
import ru.yandex.market.contentmapping.testdata.TestDataUtils
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class ShopModelStatisticsRepositoryTest : BaseAppTestClass() {
    val shopModelRepository: ShopModelRepository by bean()
    val shopRepository: ShopRepository by bean()
    val shopModelStatisticsRepository: ShopModelStatisticsRepository by bean()

    lateinit var shop: Shop
    lateinit var shopModel: ShopModel

    @Before
    fun setup() {
        shop = shopRepository.insert(TestDataUtils.testShop())
        shopModel = shopModelRepository.insertOrUpdate(TestDataUtils.nextShopModel())
        shopModelStatisticsRepository.updateStatistics(
            listOf(
                ShopModelStatisticsItem(
                    shopModelId = shopModel.id,
                    isValid = false,
                    blockExport = false,
                    shopModelCreationAllowed = false,
                    isFillRatingPoor = true,
                    withFormalization = false,
                    checksum = 0,
                    shopId = shop.id,
                    marketCategoryId = TestDataUtils.TEST_CATEGORY_ID,
                    availability = null,
                    processingStatus = null,
                    fillRating = 1.0,
                    shopModelVersion = 0,
                )
            )
        )
    }

    @Test
    fun `test category statistics`() {
        val shopFromDb = shopRepository.findById(shop.id)
        shopFromDb.id shouldBe shop.id

        val shopModelFromDb = shopModelRepository.findById(shopModel.id)
        shopModelFromDb.id shouldBe shopModel.id

        val shopModelStatisticsFromDb = shopModelStatisticsRepository.findAll()
        shopModelStatisticsFromDb shouldNotBe null
        shopModelStatisticsFromDb.size shouldBe 1
        shopModelStatisticsFromDb[0].shopModelId shouldBe shopModel.id

        val statistics = shopModelStatisticsRepository.shopCategoryStatistics(shop.id)
        statistics shouldHaveSize 1
        statistics[0].total shouldBe 1
        statistics[0].hid shouldBe shopModel.marketCategoryId
    }
}
