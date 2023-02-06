package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.switch.ChipsSwitchSection
import ru.yandex.market.mapi.section.common.switch.TabAssembler
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.05.2022
 */
class ChipsSwitchSectionTest : AbstractSectionTest() {
    private val assembler = TabAssembler()

    @Test
    fun testSection() {
        val section = ChipsSwitchSection().apply {
            addDefParams()

            // some tabs
            tabs = listOf(
                ChipsSwitchSection.Tab().also { tab ->
                    tab.title = "first"
                    tab.section = JsonHelper.parseTree(
                        """
                        {
                          "id": 111240982,
                          "type": "EngineTestSection",
                          "resources": [
                            {
                              "assembler": {
                                "type": "EngineTestAssembler"
                              },
                              "content": {
                                "testField": "some data",
                                "testData": 42
                              }
                            }
                          ]
                        }
                    """.trimIndent()
                    )
                },
                ChipsSwitchSection.Tab().also { tab ->
                    tab.title = "second"
                    tab.section = JsonHelper.parseTree(
                        """
                        {
                          "id": 111240982,
                          "type": "EngineTestSection",
                          "resources": []
                        }
                    """.trimIndent()
                    )
                }
            )
        }

        testSectionResult(
            section,
            assembler,
            buildAnyResolver(),
            resolverResponseMap = emptyMap(),
            expected = "/section/common/switch/sectionResult.json",
            processSnippets = true
        )

        assertEquals(2, section.additionalRawSections?.size)
        assertJson(section.additionalRawSections?.get("tab-987-0") ?: "{}", """
                        {
                          "id": "tab-987-0",
                          "type": "EngineTestSection",
                          "resources": [
                            {
                              "assembler": {
                                "type": "EngineTestAssembler"
                              },
                              "content": {
                                "testField": "some data",
                                "testData": 42
                              }
                            }
                          ]
                        }
        """, isExpectedInFile = false)
        assertJson(section.additionalRawSections?.get("tab-987-1") ?: "{}", """
                        {
                          "id": "tab-987-1",
                          "type": "EngineTestSection",
                          "resources": []
                        }
        """, isExpectedInFile = false)
    }

    @Test
    fun testSectionBad() {
        val section = ChipsSwitchSection().apply {
            addDefParams()

            // some valid tabs + some invalid
            tabs = listOf(
                ChipsSwitchSection.Tab(),
                ChipsSwitchSection.Tab().also { tab ->
                    tab.title = "first"
                    tab.section = JsonHelper.parseTree(
                        """
                        {
                          "id": 111240982,
                          "type": "EngineTestSection",
                          "resources": []
                        }
                    """.trimIndent()
                    )
                }
            )
        }

        testSectionResult(
            section,
            assembler,
            buildAnyResolver(),
            resolverResponseMap = emptyMap(),
            expected = "/section/common/switch/sectionBadResult.json",
            processSnippets = true
        )
    }

    @Test
    fun testSectionInvalid() {
        val section = ChipsSwitchSection().apply {
            addDefParams()
            // only invalid tabs
            tabs = listOf(
                ChipsSwitchSection.Tab(),
                ChipsSwitchSection.Tab().also { tab ->
                    tab.title = "first"
                    tab.section = JsonHelper.parseTree(
                        """
                        {
                          "id": 111240982,
                          "type": "undefined",
                          "resources": []
                        }
                    """.trimIndent()
                    )
                }
            )
        }

        testSectionResult(
            section,
            assembler,
            buildAnyResolver(),
            resolverResponseMap = emptyMap(),
            expected = "/section/common/switch/sectionInvalidResult.json",
            processSnippets = true
        )
    }
}
