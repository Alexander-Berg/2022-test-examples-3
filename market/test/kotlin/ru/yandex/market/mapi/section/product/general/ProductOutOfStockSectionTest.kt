package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.product.general.ProductOutOfStockAssembler.Companion.OUT_OF_STOCK_RESOLVER

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductOutOfStockSectionTest : AbstractSectionTest() {
    private val assembler = ProductOutOfStockAssembler()

    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json",
        OUT_OF_STOCK_RESOLVER to "/section/product/resolveOutStockReturnDateBySkuIds.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/general/outOfStockAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/general/outOfStockSectionWithDefaultOffer.json"
        )
    }

    @Test
    fun showSectionIfOfferDoesntExists() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
                ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers_empty.json",
                OUT_OF_STOCK_RESOLVER to "/section/product/resolveOutStockReturnDateBySkuIds.json"
            ),
            "/section/product/general/outOfStockSection.json"
        )
    }

    private fun buildWidget(): ProductOutOfStockSection {
        return ProductOutOfStockSection().apply {
            addDefParams()
        }
    }
}
