package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveHasCmsResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductLinksSectionTest : AbstractSectionTest() {
    private val assembler = ProductLinksAssembler()
    private val resolverMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveHasCmsResponse.RESOLVER to "/section/product/hasCms.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/general/linksAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/linksSection.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/linksContentResult.json"
        )
    }

    private fun buildWidget(): ProductLinksSection {
        return ProductLinksSection().apply {
            addDefParams()
        }
    }
}
