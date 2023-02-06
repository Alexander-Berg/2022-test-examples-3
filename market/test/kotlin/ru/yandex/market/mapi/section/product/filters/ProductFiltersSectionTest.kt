package ru.yandex.market.mapi.section.product.filters

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.core.util.mockRearrs
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductFiltersSectionTest : AbstractSectionTest() {
    private val assembler = ProductFiltersAssembler()
    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/filters/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/filters/resolveProductOffers.json"
    )
    private val resaleResolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/filters/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/filters/resolveResaleProductOffers.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/filters/productFiltersAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            section = buildSection(),
            assembler = assembler,
            resolver = buildAnyResolver(),
            resolverResponseMap = resolversMap,
            processSnippets = true,
            expected = "/section/product/filters/productFiltersSection.json"
        )
    }

    @Test
    fun testResaleAssembly() {
        mockRearrs("market_resale_goods_exp=1")
        assembler.testAssembly(
            fileMap = resaleResolversMap,
            expected = "/section/product/filters/productResaleFiltersAssembly.json",
        )
    }

    @Test
    fun testResaleSection() {
        mockRearrs("market_resale_goods_exp=1")
        testSectionResult(
            section = buildSection(),
            assembler = assembler,
            resolver = buildAnyResolver(),
            resolverResponseMap = resaleResolversMap,
            processSnippets = true,
            expected = "/section/product/filters/productResaleFiltersSection.json"
        )
    }

    private fun buildSection(): ProductFiltersSection {
        return ProductFiltersSection().apply {
            addDefParams()
        }
    }
}
