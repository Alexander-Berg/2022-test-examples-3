package ru.yandex.market.mapi.core.model.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.05.2022
 */
@Service
open class EngineTestPagedByPageNumAssembler : AbstractSimpleAssembler<EngineTestPagedByPageNumAssembler.TestResponse, EngineTestSnippet>() {

    override fun getName() = "EngineTestPagedByPageNumAssembler"
    override fun getResponseClass() = TestResponse::class
    override fun getSnippetClass() = EngineTestSnippet::class

    override fun doConvert(response: TestResponse, configNode: JsonNode?, builder: AssemblyResult.Builder) {

        for (resultItem in response.result) {
            builder.tryAdd {
                EngineTestSnippet(
                    testField = resultItem.field,
                    testData = resultItem.data
                )
            }
        }
    }

    data class TestResponse(
        val result: List<ResultItem>
    )

    data class ResultItem(
        val field: String,
        val data: Int
    )
}
