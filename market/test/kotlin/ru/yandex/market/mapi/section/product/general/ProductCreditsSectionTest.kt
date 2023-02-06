package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductCreditsSectionTest : AbstractSectionTest() {
    private val assembler = ProductCreditsAssembler()

    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/general/creditsAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/general/creditsSection.json"
        )
    }

    private fun buildWidget(): ProductCreditsSection {
        return ProductCreditsSection().apply {
            addDefParams()
        }
    }
}
