package ru.yandex.market.mapi.core.model.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.mapiContext
import ru.yandex.market.mapi.core.model.screen.AbstractSection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.02.2022
 */
@Service
class EngineTestAssembler : AbstractSimpleAssembler<EngineTestAssembler.TestResponse, EngineTestSnippet>() {

    companion object {
        const val FLAG_GENERATE_TEST = "generate_test_snippet"
    }

    override fun getName() = "EngineTestAssembler"
    override fun getResponseClass() = TestResponse::class
    override fun getSnippetClass() = EngineTestSnippet::class

    override fun generateSnippets(section: AbstractSection?): List<EngineTestSnippet>? {
        if (!mapiContext().hasFlag(FLAG_GENERATE_TEST)) {
            return null
        }

        return listOf(
            EngineTestSnippet(
                testField = "generated snippet",
                testData = 42
            )
        )
    }

    override fun doConvert(response: TestResponse, configNode: JsonNode?, builder: AssemblyResult.Builder) {
        if (response.expectedField == "error-response") {
            builder.addError("Found invalid response, stop assembly")
            return
        }

        if (response.expectedField == "error-exception") {
            throw RuntimeException("Check exception")
        }

        builder.tryAdd {
            EngineTestSnippet(
                testField = response.expectedField,
                testData = response.moreData.optParam ?: response.moreData.param
            )
        }
    }

    data class TestResponse(
        val expectedField: String,
        val moreData: LinkedData
    )

    data class LinkedData(
        val param: Int,
        val optParam: Int?
    )
}