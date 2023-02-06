package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductComparisonSectionTest : AbstractSectionTest() {
    private val assembler = ProductComparisonAssembler()
    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/general/comparisonAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/general/comparisonWidget.json"
        )
    }

    private fun buildWidget(): ProductComparisonSection {
        return ProductComparisonSection().apply {
            addDefParams()
        }
    }
}
