package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.core.util.mockRearrs
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductAlternativeOffersTest : AbstractSectionTest() {
    private val assembler = ProductAlternativeOffersAssembler()

    private val resolversMap = mapOf(
        ResolveProductOffersResponse.RESOLVER to "/section/product/alternativeoffers/resolveProductOffers.json",
    )

    private val resaleResolversMap = mapOf(
        ResolveProductOffersResponse.RESOLVER to "/section/product/alternativeoffers/resolveResaleProductOffers.json",
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/alternativeoffers/productAlternativeOffersAssembly.json",
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            expected = "/section/product/alternativeoffers/productAlternativeOffersAssemblyResult.json"
        )
    }

    @Test
    fun testSectionResult() {
        testSectionResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            expected = "/section/product/alternativeoffers/productAlternativeOffersSection.json",
        )
    }

    @Test
    fun testResaleAssembly() {
        mockRearrs("market_resale_goods_exp=1")
        assembler.testAssembly(
            fileMap = resaleResolversMap,
            expected = "/section/product/alternativeoffers/productResaleAlternativeOffersAssembly.json",
        )
    }

    @Test
    fun testResaleContentResult() {
        mockRearrs("market_resale_goods_exp=1")
        testContentResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resaleResolversMap,
            expected = "/section/product/alternativeoffers/productResaleAlternativeOffersAssemblyResult.json"
        )
    }

    private fun buildSection(): ProductAlternativeOffersSection {
        return ProductAlternativeOffersSection().apply {
            addDefParams()
        }
    }
}
