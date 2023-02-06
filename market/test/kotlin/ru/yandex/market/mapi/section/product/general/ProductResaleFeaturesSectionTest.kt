package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveProductConfigByNavigationPathResponse
import ru.yandex.market.mapi.core.util.mockRearrs
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductResaleFeaturesSectionTest : AbstractSectionTest() {

    private val assembler = ProductResaleFeaturesAssembler()

    private val noResaleResolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json",
        ResolveProductConfigByNavigationPathResponse.RESOLVER to
            "/section/common/product/productConfigByNavigationPathResponse.json"
    )

    private val resaleResolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resale/resolveProductOffersResale.json",
        ResolveProductConfigByNavigationPathResponse.RESOLVER to
            "/section/common/product/productConfigByNavigationPathResponse.json"
    )

    @Test
    fun noSectionTestAssembly() {
        assembler.testAssembly(
            fileMap = noResaleResolversMap,
            expected = "/section/product/resale/resaleFeaturesNoSectionAssembly.json",
        )
    }

    @Test
    fun testSection() {
        mockRearrs("market_resale_goods_exp=1")
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resaleResolversMap,
            "/section/product/resale/resaleFeaturesSection.json",
        )
    }

    private fun buildWidget(): ProductResaleFeaturesSection {
        return ProductResaleFeaturesSection().apply {
            addDefParams()
        }
    }
}
