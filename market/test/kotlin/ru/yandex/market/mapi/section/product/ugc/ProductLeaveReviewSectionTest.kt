package ru.yandex.market.mapi.section.product.ugc

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveMyReviewsResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductLeaveReviewSectionTest : AbstractSectionTest() {
    private val assembler = ProductLeaveReviewAssembler()
    val resolverMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveMyReviewsResponse.RESOLVER to "/section/product/ugc/resolveMyReviews.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/ugc/leaveReviewAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/ugc/leaveReviewSection.json"
        )
    }

    private fun buildWidget(): ProductLeaveReviewSection {
        return ProductLeaveReviewSection().apply {
            addDefParams()
        }
    }
}
