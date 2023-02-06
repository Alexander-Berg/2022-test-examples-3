package ru.yandex.market.mapi.core.util

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import ru.yandex.market.mapi.core.model.screen.AbstractSection
import ru.yandex.market.mapi.core.model.section.EngineTestSection
import ru.yandex.market.mapi.core.model.section.EngineTestSnippet
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2022
 */
class TypeResolverTest {
    @Test
    fun testSectionParsing() {
        val json = """
            {
              "id": "111240982",
              "type": "EngineTestSection",
              "content": [
                {
                  "@c": "EngineTestSnippet",
                  "testField": "some data",
                  "testData": 42
                }
              ]
            }
        """.trimIndent()

        val section = JsonHelper.parse<AbstractSection>(json)
        assertTrue(section is EngineTestSection)

        assertEquals(EngineTestSection::class.simpleName, section.type)
        assertEquals("111240982", section.id)
        assertEquals(1, section.content?.size)

        val snippet = section.content?.first()
        assertTrue(snippet != null)
        assertTrue(snippet is EngineTestSnippet)
        assertEquals(EngineTestSnippet::class.simpleName, snippet.internalType)
        assertEquals(42, snippet.testData)
        assertEquals(42, snippet.testData)
        assertEquals("some data", snippet.testField)

        // check that value written to json is same
        JSONAssert.assertEquals(json, JsonHelper.toString(section), JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    fun testSnippetParsing() {
        val json = """
            {
              "testField": "some data",
              "testData": 42
            }
        """.trimIndent()

        val snippet = JsonHelper.parse<EngineTestSnippet>(json)
        assertEquals(EngineTestSnippet::class.simpleName, snippet.internalType)
        assertEquals(42, snippet.testData)
        assertEquals(42, snippet.testData)
        assertEquals("some data", snippet.testField)

        val jsonWithType = """
            {
              "@c": "EngineTestSnippet",
              "testField": "some data",
              "testData": 42
            }
        """.trimIndent()

        // check that value written to json is as expected
        JSONAssert.assertEquals(jsonWithType, JsonHelper.toString(snippet), JSONCompareMode.NON_EXTENSIBLE)
    }
}