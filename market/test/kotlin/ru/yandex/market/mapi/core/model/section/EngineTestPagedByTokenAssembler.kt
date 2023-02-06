package ru.yandex.market.mapi.core.model.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.util.daoJsonTree

@Service
open class EngineTestPagedByTokenAssembler: AbstractSimpleAssembler<EngineTestPagedByTokenAssembler.TestResponse, EngineTestSnippet>() {
    override fun getName() = "EngineTestPagedByTokenAssembler"
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

        val page = response.page
        val pagesCount = response.pagesCount

        val tokenJson = when {
            (page < pagesCount) -> daoJsonTree(
                "test.page" to page+1,
                "test.color" to response.nextPageColor
            )
            else -> null
        }
        tokenJson?.let { token ->
            builder.addSectionEventParams(AssemblyResult.NextPageToken(token))
        }
    }

    data class TestResponse(
        val result: List<ResultItem>,
        val total: Int,
        val page: Int,
        val pagesCount: Int,
        var nextPageColor: String? = null
    )

    data class ResultItem(
        val field: String,
        val data: Int
    )
}
