package ru.yandex.market.mapi.core.model.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.util.optInt

@Service
class EngineTestResolverToAssemblyResultBuilderAssembler
    : AbstractSimpleAssembler<EngineTestResolverToAssemblyResultBuilderAssembler.TestResponse, EngineTestSnippet>()
{
    override fun getSnippetClass() = EngineTestSnippet::class
    override fun getResponseClass() = TestResponse::class
    override fun getName() = "EngineTestResolverToAssemblyResultBuilder"

    override fun doConvert(response: TestResponse, configNode: JsonNode?, builder: AssemblyResult.Builder) {
        val param = builder.resolvers?.firstOrNull()?.params?.optInt("testParam") ?: return builder.hideAndFail()
        for (resultItem in response.result) {
            builder.tryAdd {
                EngineTestSnippet(
                    testField = resultItem.field,
                    testData = resultItem.data,
                    testResolverParam = param
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
