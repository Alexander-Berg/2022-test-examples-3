package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveEcomQuestionResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.survey.SurveySection
import ru.yandex.market.mapi.section.common.survey.SurveySnippetAssembler

/**
 * @author Rustam Kendzhaev / rkendzhaev@ / 05.07.2022
 */
class SurveySectionTest : AbstractSectionTest() {
    private val assembler = SurveySnippetAssembler()
    private val resolver = ResolveEcomQuestionResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/survey/fapiResponse.json"),
            expected = "/section/common/survey/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/survey/fapiResponse.json"),
            expected = "/section/common/survey/sectionResult.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/survey/fapiResponse.json"),
            expected = "/section/common/survey/contentResult.json"
        )
    }

    private fun buildWidget(): SurveySection {
        return SurveySection().apply {
            addDefParams()
        }
    }
}
