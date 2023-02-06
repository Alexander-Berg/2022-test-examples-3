package ru.yandex.market.mapi.core.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.model.response.SectionUiDto
import ru.yandex.market.mapi.core.model.section.EngineTestSection
import ru.yandex.market.mapi.core.model.section.EngineTestSnippet
import ru.yandex.market.mapi.core.model.section.UnimplementedSection
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.02.2022
 */
class ClassUtilTest {
    @Test
    fun testSectionParsing() {
        val classMap = findAllSections()
        assertTrue { classMap.containsKey(UnimplementedSection::class.simpleName) }
        assertTrue { classMap.containsKey(EngineTestSection::class.simpleName) }
    }

    @Test
    fun testSnippetParsing() {
        val snippetsMap = findAllSnippets()
        assertTrue { snippetsMap.containsKey(EngineTestSnippet::class.simpleName) }
    }

    @Test
    fun testUiDtoParsing() {
        val typeMap = findAllUiDto()
        assertTrue { typeMap.containsKey(SectionUiDto::class.simpleName) }
        assertEquals(SectionUiDto::class.java, typeMap[SectionUiDto::class.simpleName])
    }
}