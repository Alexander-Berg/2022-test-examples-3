package ru.yandex.direct.core.entity.uac.service.shopinshop

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.uac.model.ShopInShopBusiness
import ru.yandex.direct.core.entity.uac.model.ShopInShopBusinessInfo
import ru.yandex.direct.core.entity.uac.model.Source
import ru.yandex.direct.core.entity.uac.repository.mysql.ShopInShopBusinessesRepository

@RunWith(JUnitParamsRunner::class)
class ShopInShopBusinessesServiceTest {

    @Mock
    lateinit var shopInShopBusinessesRepository: ShopInShopBusinessesRepository

    lateinit var shopInShopBusinessesService: ShopInShopBusinessesService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        shopInShopBusinessesService =
            ShopInShopBusinessesService(shopInShopBusinessesRepository)
    }

    @Test
    @Parameters(
        value = [
            "https://market.yandex.ru/business--name/123456, MARKET, 123456, market.yandex.ru, true, https://market.yandex.ru/store--name?businessId=123456, https://partner.market.yandex.ru/business/123456/direct-bonus",
            "https://m.market.yandex.ru/business--name/123456?abc=321, MARKET, 123456, market.yandex.ru, true, https://m.market.yandex.ru/store--name?businessId=123456, https://partner.market.yandex.ru/business/123456/direct-bonus",
            "https://m.market.yandex.ru/business--name/123456?abc=123&abc=321, MARKET, 123456, market.yandex.ru, true, https://m.market.yandex.ru/store--name?businessId=123456, https://partner.market.yandex.ru/business/123456/direct-bonus",
            "https://pokupki.market.yandex.ru/business--name/123456, MARKET, 123456, market.yandex.ru, true, https://pokupki.market.yandex.ru/store--name?businessId=123456, https://partner.market.yandex.ru/business/123456/direct-bonus",
            "https://market.yandex.ru/product--product-name/987654?skuId=sku-id&sku=sku&businessId=123456&show-uid=show-uid&offerid=offer-id&cpc=cpc, MARKET, 123456, market.yandex.ru, true, , https://partner.market.yandex.ru/business/123456/direct-bonus",
            "https://market.yandex.ru/offer/987654?cpc=cpc&hid=hid&hyperid=hyper-id&lr=2&modelid=model-id&nid=nid&show-uid=show-uid&businessId=123456, MARKET, 123456, market.yandex.ru, true, , https://partner.market.yandex.ru/business/123456/direct-bonus",
            "https://market.yandex.ru/store--store-name?businessId=123456, MARKET, 123456, market.yandex.ru, false, https://market.yandex.ru/store--store-name?businessId=123456, https://partner.market.yandex.ru/business/123456/direct-bonus"
        ]
    )
    fun getBusinessInfoByUrl_success(
        url: String,
        sourceValue: String,
        businessId: Long,
        domain: String,
        isAdditionalCommission: Boolean,
        suggestedUrlWithoutCommission: String,
        urlToShopInShopAccount: String
    ) {
        val source = Source.valueOf(sourceValue)
        val feedUrl = "feed_url"
        val counterId = 10000L
        whenever(shopInShopBusinessesRepository.getBySourceAndBusinessId(eq(source), eq(businessId)))
            .doReturn(
                ShopInShopBusiness()
                    .withFeedUrl(feedUrl)
                    .withCounterId(counterId)
                    .withSource(source)
                    .withBusinessId(businessId)
            )

        val result = shopInShopBusinessesService.getBusinessInfoByUrl(url)

        val expectedResult = ShopInShopBusinessInfo(
            businessId, feedUrl, counterId, source, domain, isAdditionalCommission,
            suggestedUrlWithoutCommission.ifBlank { null },
            urlToShopInShopAccount.ifBlank { null }
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Parameters(
        value = [
            "invalid_url",
            "https://market.yandex.ru/business--name",
            "https://market.yandex.ru/business--name/abcd",
            "https://shop.ru/business--name/123456",
            "https://market.yandex.ru/123456",
            "https://market.yandex.ru/product--product-name/987654?skuId=sku-id&sku=sku&show-uid=show-uid&offerid=offer-id&cpc=cpc",
            "https://market.yandex.ru/Business--name/123456",
            "https://market.yandex.ru/store--store-name?businessid=123456"
        ]
    )
    fun getBusinessInfoByUrl_invalidUrl(url: String) {
        whenever(shopInShopBusinessesRepository.getBySourceAndBusinessId(any(), any()))
            .doReturn(ShopInShopBusiness())

        val result = shopInShopBusinessesService.getBusinessInfoByUrl(url)

        assertThat(result, equalTo(null))
    }
}
