package ru.yandex.market.contentmapping.dto.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.contentmapping.testdata.TestDataUtils.nextShopModel

class ShopModelUpdateTest {

    @Test
    fun toModelIfNameIsEquals() {
        val shopModelUpdate = ShopModelUpdate(name = NAME, description = null, marketCategoryId = null)
        val result = shopModelUpdate.mergeToShopModel(ShopModel(name = NAME, shopSku = SHOP_SKU))
        assertThat(result.name).isEqualTo(NAME)
    }

    @Test
    fun toModelIfNameIsNotEquals() {
        val shopModelUpdate = ShopModelUpdate(name = NAME2, description = null, marketCategoryId = null)
        val result = shopModelUpdate.mergeToShopModel(ShopModel(name = NAME, shopSku = SHOP_SKU))
        assertThat(result.name).isEqualTo(NAME)
    }

    @Test
    fun toModelIfDescriptionIsEquals() {
        val shopModelUpdate = ShopModelUpdate(description = DESCRIPTION, name = null, marketCategoryId = null)
        val result = shopModelUpdate.mergeToShopModel(
                nextShopModel().copy(description = DESCRIPTION, shopSku = SHOP_SKU))
        assertThat(result.description).isEqualTo(DESCRIPTION)
    }

    @Test
    fun toModelIfDescriptionIsNotEquals() {
        val shopModelUpdate = ShopModelUpdate(description = DESCRIPTION2, name = null, marketCategoryId = null)
        val result = shopModelUpdate.mergeToShopModel(
                nextShopModel().copy(description = DESCRIPTION, shopSku = SHOP_SKU))
        assertThat(result.description).isEqualTo(DESCRIPTION)
    }

    companion object {
        const val NAME = "name"
        const val NAME2 = "name2"
        const val SHOP_SKU = "shopSku"
        const val DESCRIPTION = "description"
        const val DESCRIPTION2 = "description2"
    }
}
