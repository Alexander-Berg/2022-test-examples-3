package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductDescriptionSectionTest: AbstractSectionTest() {
    private val assembler = ProductDescriptionAssembler()

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
                ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json"
            ),
            expected = "/section/product/general/descriptionAssembly.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
                ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json"
            ),
            "/section/product/general/descriptionSection.json"
        )
    }

    private fun buildWidget(): ProductDescriptionSection {
        return ProductDescriptionSection().apply {
            addDefParams()
        }
    }
}
