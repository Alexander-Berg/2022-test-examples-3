package ru.yandex.market.mapi.client.cms

import com.fasterxml.jackson.databind.node.ArrayNode
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.model.response.MapiResponseContext
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.core.util.optStr

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.01.2022
 */
class CmsResponseParserTest {

    @Test
    fun testParse() {
        checkParsing(
            "/client/cms/templatorResponse.json",
            "/client/cms/templatorParsedScreen.json"
        )
    }

    @Test
    fun testParseSections() {
        val screen = checkParsing(
            "/client/cms/templatorSectionsResponse.json",
            "/client/cms/templatorSectionsParsedScreen.json"
        )

        val resultRawSections = screen.sections.mapNotNull { it.rawSection }
        assertJson(resultRawSections, "/client/cms/templatorSectionsRawSections.json")
    }

    @Test
    fun testParseAliases() {
        checkParsing(
            "/client/cms/templatorAliasResponse.json",
            "/client/cms/templatorAliasResponseParsed.json"
        )
    }

    @Test
    fun testParseNoResults() {
        checkParsing(
            "/client/cms/templatorNoPage.json",
            "/client/cms/templatorNoPageScreen.json"
        )
    }

    @Test
    fun testParseInvalidPage() {
        checkParsing(
            "/client/cms/templatorInvalidPage.json",
            "/client/cms/templatorInvalidPageScreen.json"
        )
    }

    @Test
    fun testShowConditions() {
        checkParsing(
            "/client/cms/templatorShowConditionsResponse.json",
            "/client/cms/templatorShowConditionsParsedScreen.json"
        )
    }

    @Test
    fun testParseRawSections() {
        val tree = JsonHelper.parseTree("/client/cms/templatorSectionsResponse.json".asResource())
        val sectionsNode = tree.get("result").get(0).get("sections") as ArrayNode
        val rawSections = sectionsNode.mapNotNull { SectionToRefresh.simple(it) }

        val result = CmsResponseParser.convertRawSectionsToMapi(rawSections, MapiResponseContext().apply {
            cmsPageId = 111000
            resolverParams = mutableMapOf(
                "param" to "value",
                "answer" to 42
            )
        })

        // compare screen
        assertJson(result, "/client/cms/templatorSectionsRawScreen.json")

        // also compare raw sections - they should be same
        val resultRawSections = result.sections.mapNotNull { it.rawSection }
        assertJson(resultRawSections, "/client/cms/templatorSectionsRawSections.json")
    }

    @Test
    fun testParseRawAndHandleParams() {
        val sections = """
            {
              "sections": [
                {
                  "id": "123",
                  "type": "EngineTestSection",
                  "title": "Первая секция"
                },
                {
                  "id": "124",
                  "type": "EngineTestSection",
                  "title": "Подмена ID"
                }
              ]
            }
        """.trimIndent()

        val tree = JsonHelper.parseTree(sections)
        val sectionsNode = tree.get("sections") as ArrayNode
        val rawSections = sectionsNode.mapNotNull { SectionToRefresh.simple(it) }
        rawSections.forEach { section ->
            if (section.raw?.optStr("id") == "124") {
                section.refreshParams = mapOf(
                    SectionToRefresh.PARAM_REPLACE_ID to "42"
                )
            }
        }

        val result = CmsResponseParser.convertRawSectionsToMapi(rawSections, null)

        // compare screen
        val expected = """
            {
                "debug":{"rawSections":true},
                "sections":[
                    {"type":"EngineTestSection","id":"123","title":"Первая секция"},
                    {"type":"EngineTestSection","id":"42","title":"Подмена ID"}
                ]
            }
        """.trimIndent()
        assertJson(result, expected, isExpectedInFile = false)

        // also compare raw sections - they should be as sent
        val expectedRaw = """
            [
                {
                  "id": "123",
                  "type": "EngineTestSection",
                  "title": "Первая секция"
                },
                {
                  "id": "124",
                  "type": "EngineTestSection",
                  "title": "Подмена ID"
                }
            ]
        """.trimIndent()
        val resultRawSections = result.sections.mapNotNull { it.rawSection }
        assertJson(resultRawSections, expectedRaw, isExpectedInFile = false)
    }

    private fun checkParsing(cmsResponseFile: String, expectedFile: String): ScreenResponse {
        val json = cmsResponseFile.asResource()
        val response = CmsResponseParser.convertCmsResponseToMapi(json)

        assertJson(response, expectedFile)

        return response
    }
}
