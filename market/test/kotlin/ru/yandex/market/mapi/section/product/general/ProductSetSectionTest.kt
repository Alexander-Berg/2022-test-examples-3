package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductSetSectionTest: AbstractSectionTest() {
    private val assembler = ProductSetAssembler(randomStringGenerator = {" "})

    private val resolverMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo_set.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers_set.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            resolverMap,
            "/section/product/general/setAssembly.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/setSection.json"
        )
    }

    @Test
    fun testContent() {
        testContentResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/setContent.json"
        )
    }

    private fun buildSection(): ProductSetSection {
        return ProductSetSection().apply {
            addDefParams()
        }
    }
}
