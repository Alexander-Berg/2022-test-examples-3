package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.section.AbstractSectionTest

class RichContentSectionTest: AbstractSectionTest() {
    private val assembler = RichContentAssembler()

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(
                "resolveCmsV1" to "/section/product/resolveCmsRichContent.json"
            ),
            expected = "/section/product/general/richContentResult.json"
        )
    }

    private fun buildWidget(): RichContentSection {
        return RichContentSection().apply {
            addDefParams()
        }
    }
}
