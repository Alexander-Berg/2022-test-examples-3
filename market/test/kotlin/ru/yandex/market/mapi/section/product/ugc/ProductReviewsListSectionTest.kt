package ru.yandex.market.mapi.section.product.ugc

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductReviewsResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.core.util.mockRegion
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductReviewsListSectionTest : AbstractSectionTest() {
    private val assembler = ProductReviewsListAssembler()
    val resolverMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductReviewsResponse.RESOLVER to "/section/product/ugc/resolveProductReviews.json",
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/ugc/reviewsListAssembly.json",
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolverMap,
            expected = "/section/product/ugc/reviewsListSectionContent.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/ugc/reviewsListWidget.json"
        )
    }

    private fun buildWidget(): ProductReviewsListSection {
        return ProductReviewsListSection().apply {
            addDefParams()
        }
    }
}
