package ru.yandex.market.contentmapping.dto.mapping

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.contentmapping.dto.model.ShopModel

class ParamMappingTest {
    @Test
    fun containsRequiredShopParams() {
        Assertions.assertThat(ParamMapping().containsRequiredShopParams()).isFalse
        Assertions.assertThat(
                ParamMapping(shopParams = listOf(ShopParam(ShopModel.SHOP_OFFER_VENDOR, null)))
                        .containsRequiredShopParams()
        ).isTrue
        Assertions.assertThat(
                ParamMapping(shopParams = listOf(ShopParam(ShopModel.SHOP_OFFER_BARCODE, null)))
                        .containsRequiredShopParams()
        ).isTrue
        Assertions.assertThat(
                ParamMapping(shopParams = listOf(ShopParam(ShopModel.SHOP_OFFER_NAME, null)))
                        .containsRequiredShopParams()
        ).isTrue
        Assertions.assertThat(
                ParamMapping(shopParams = listOf(ShopParam(ShopModel.SHOP_OFFER_DESCRIPTION, null)))
                        .containsRequiredShopParams()
        ).isTrue
        Assertions.assertThat(
                ParamMapping(shopParams = listOf(ShopParam(ShopModel.SHOP_OFFER_CATEGORY, null)))
                        .containsRequiredShopParams()
        ).isTrue
    }
}
