package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveQuestionsByProductIdResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductSummarySectionTest : AbstractSectionTest() {
    private val assembler = ProductSummaryAssembler()
    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json",
        ResolveQuestionsByProductIdResponse.RESOLVER to "/section/product/resolveQuestionsByProductId.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/general/summaryAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/general/summarySection.json"
        )
    }

    private fun buildWidget(): ProductSummarySection {
        return ProductSummarySection().apply {
            addDefParams()
        }
    }
}
