package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Dasha Rednikina / dashared@ / 16.06.2022
 */
class ProductInstallmentsSectionTest : AbstractSectionTest() {
    private val assembler = ProductInstallmentsAssembler()
    private val resolverMap = mapOf(
        ResolveProductOffersResponse.RESOLVER to "/section/installments/resolveProductOffers.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/general/installmentsAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/installmentsSection.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/installmentsContentResult.json"
        )
    }

    private fun buildWidget(): ProductInstallmentsSection {
        return ProductInstallmentsSection().apply {
            addDefParams()
        }
    }
}
