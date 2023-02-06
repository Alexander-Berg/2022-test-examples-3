package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveProductConfigByNavigationPathResponse
import ru.yandex.market.mapi.core.util.mockRearrs
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductResaleFAQSectionTest : AbstractSectionTest() {

    private val assembler = ProductResaleFAQAssembler()

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
            expected = "/section/product/resale/resaleFaqNoSectionAssembly.json",
        )
    }

    @Test
    fun testAssembly() {
        mockRearrs("market_resale_goods_exp=1")
        assembler.testAssembly(
            fileMap = resaleResolversMap,
            expected = "/section/product/resale/resaleFaqAssembly.json",
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resaleResolversMap,
            expected = "/section/product/resale/resaleFaqAssemblyResult.json"
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
            "/section/product/resale/resaleFaqSection.json",
        )
    }

    private fun buildWidget(): ProductResaleFAQSection {
        return ProductResaleFAQSection().apply {
            addDefParams()
        }
    }
}
