package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.section.AbstractSectionTest

class ProductLinkSectionTest : AbstractSectionTest() {

    @Test
    fun testSection()
    {
        testSectionResult(
            section = buildSection(),
            expected = "/section/product/general/productLinkSection.json",
            assembler = null,
            resolver = buildAnyResolver(),
            resolverResponseMap = emptyMap(),
            config = null,
            processSnippets = false,
        )
    }

    private fun buildSection(): ProductLinkSection {
        return ProductLinkSection().apply {
            id = Companion.DEFAULT_SECTION_ID
        }
    }
}
