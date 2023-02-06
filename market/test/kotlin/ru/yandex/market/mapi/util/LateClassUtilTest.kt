package ru.yandex.market.mapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.model.section.EngineTestSection
import ru.yandex.market.mapi.core.model.section.UnimplementedSection
import ru.yandex.market.mapi.core.util.findAllSections
import ru.yandex.market.mapi.core.util.findAllSnippets
import ru.yandex.market.mapi.section.common.product.ProductSnippet
import ru.yandex.market.mapi.section.common.product.ProductsScrollboxSection
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.02.2022
 */
class LateClassUtilTest {
    @Test
    fun testSectionParsing() {
        val classMap = findAllSections()
        assertTrue { classMap.containsKey(UnimplementedSection::class.simpleName) }
        assertTrue { classMap.containsKey(EngineTestSection::class.simpleName) }
        assertTrue { classMap.containsKey(ProductsScrollboxSection::class.simpleName) }
    }

    @Test
    fun testSnippetParsing() {
        val snippetsMap = findAllSnippets()
        assertTrue { snippetsMap.containsKey(ProductSnippet::class.simpleName) }
    }
}