package ru.yandex.market.mapi.section.product.ugc

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductFactorsSectionTest : AbstractSectionTest() {
    private val assembler = ProductFactorsAssembler()
    val resolverMap = mapOf(
        ProductFactorsAssembler.FACTOR_RESOLVER to "/section/product/ugc/resolveProductReviewsFactorsSummary.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/ugc/factorsAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/ugc/factorsSection.json"
        )
    }

    private fun buildSection(): ProductFactorsSection {
        return ProductFactorsSection().apply {
            addDefParams()
        }
    }
}
