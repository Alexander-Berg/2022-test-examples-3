package ru.yandex.market.contentmapping.dto.model

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.contentmapping.dto.model.mboc.ApprovedSkuMapping
import ru.yandex.market.contentmapping.dto.model.mboc.SkuType
import ru.yandex.market.contentmapping.services.category.info.CategoryInfoService
import ru.yandex.market.contentmapping.testdata.TestDataUtils

class ShopModelTest {

    @Test
    fun testEffectiveAllowModelCreateOrUpdate() {
        val mock = Mockito.mock(CategoryInfoService::class.java)
        Mockito.`when`(mock.goodContentCategoryIds).thenReturn(setOf(1L))

        var m = TestDataUtils.testShopModel().copy(
                externalCategoryId = 1L,
                allowModelCreateUpdate = true,
        )
        m.effectiveAllowModelCreateOrUpdate(mock::goodContentCategoryIds) shouldBe true

        m = TestDataUtils.testShopModel().copy(
                externalCategoryId = 1L,
                approvedSkuMapping = ApprovedSkuMapping(1, SkuType.TYPE_PARTNER),
                allowModelCreateUpdate = true,
        )
        m.effectiveAllowModelCreateOrUpdate(mock::goodContentCategoryIds) shouldBe true

        m = TestDataUtils.testShopModel().copy(
                externalCategoryId = 2L,
                allowModelCreateUpdate = true,
        )
        m.effectiveAllowModelCreateOrUpdate(mock::goodContentCategoryIds) shouldBe true

        m = TestDataUtils.testShopModel().copy(
                externalCategoryId = 1L,
                allowModelCreateUpdate = false,
        )
        m.effectiveAllowModelCreateOrUpdate(mock::goodContentCategoryIds) shouldBe false
    }

    @Test
    fun effectiveShopValues() {
        val sourceMap = mapOf("k1" to "v1", "k2" to "v2")
        val expectedMap = sourceMap.toMutableMap()
        expectedMap[ShopModel.SHOP_OFFER_VENDOR] = SHOP_VENDOR
        expectedMap[ShopModel.SHOP_OFFER_BARCODE] = SHOP_BARCODE
        expectedMap[ShopModel.SHOP_OFFER_DESCRIPTION] = SHOP_DESCRIPTION
        expectedMap[ShopModel.SHOP_OFFER_NAME] = SHOP_NAME
        expectedMap[ShopModel.SHOP_OFFER_CATEGORY] = SHOP_CATEGORY_NAME

        val shopModel = ShopModel(
                shopSku = SHOP_SKU,
                shopValues = sourceMap,
                shopVendor = SHOP_VENDOR,
                name = SHOP_NAME,
                description = SHOP_DESCRIPTION,
                shopCategoryName = SHOP_CATEGORY_NAME,
                barCode = SHOP_BARCODE
        )
        Assertions.assertThat(shopModel.effectiveShopValues).isEqualTo(expectedMap)
    }

    companion object {
        private const val SHOP_VENDOR = "sv"
        private const val SHOP_NAME = "sn"
        private const val SHOP_DESCRIPTION = "sd"
        private const val SHOP_CATEGORY_NAME = "scn"
        private const val SHOP_SKU = "shopSku"
        private const val SHOP_BARCODE = "barcode"
    }
}
