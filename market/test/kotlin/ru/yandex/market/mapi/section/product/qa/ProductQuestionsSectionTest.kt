package ru.yandex.market.mapi.section.product.qa

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveQuestionsByProductIdResponse
import ru.yandex.market.mapi.core.MapiConstants
import ru.yandex.market.mapi.core.util.mockQueryParams
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Arsen Salimov / maetimo@ / 20.06.2022
 */
class ProductQuestionsSectionTest : AbstractSectionTest() {
    private val assembler = ProductQuestionsAssembler()
    val resolverMap = mapOf(
        ResolveQuestionsByProductIdResponse.RESOLVER to "/section/product/resolveQuestionsByProductId.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/qa/questionsAssembly.json",
        )
    }

    @Test
    fun testSection() {
        mockQueryParams(
            mapOf(
                MapiConstants.SKU_ID to "0",
                MapiConstants.PRODUCT_ID to "1"
            )
        )

        testSectionResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/qa/questionsSection.json"
        )
    }

    private fun buildSection(): ProductQuestionsSection {
        return ProductQuestionsSection().apply {
            addDefParams()
        }
    }
}
