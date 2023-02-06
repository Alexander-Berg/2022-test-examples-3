package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveCompetitiveCardResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductSponsoredOfferSectionTest : AbstractSectionTest() {

    private val assembler = ProductSponsoredOfferAssembler()

    private val resolversMap = mapOf(
        ResolveCompetitiveCardResponse.RESOLVER to "/section/product/resolveCompetitiveCard.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/general/sponsoredOfferAssembly.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            section = ProductSponsoredOfferSection().apply { addDefParams() },
            assembler = assembler,
            resolver = buildAnyResolver(),
            resolverResponseMap = resolversMap,
            expected = "/section/product/general/sponsoredOfferSection.json"
        )
    }

}
