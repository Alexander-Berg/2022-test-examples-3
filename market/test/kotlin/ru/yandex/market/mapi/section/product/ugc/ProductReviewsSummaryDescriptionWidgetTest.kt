package ru.yandex.market.mapi.section.product.ugc

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.product.ugc.ProductReviewSummaryDescriptionAssembler.Companion.SUMMARY_RESOLER

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductReviewsSummaryDescriptionWidgetTest : AbstractSectionTest() {
    private val assembler = ProductReviewSummaryDescriptionAssembler()
    val resolverMap = mapOf(
        SUMMARY_RESOLER to "/section/product/ugc/resolveReviewSummaryOpinionsByProductId.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/ugc/reviewsSummaryDescriptionAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/ugc/reviewsSummaryDescriptionWidget.json",
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolverMap,
            expected = "/section/product/ugc/reviewsSummaryDescriptionResult.json"
        )
    }

    private fun buildWidget(): ProductReviewSummaryDescriptionSection {
        return ProductReviewSummaryDescriptionSection().apply {
            addDefParams()
        }
    }
}
