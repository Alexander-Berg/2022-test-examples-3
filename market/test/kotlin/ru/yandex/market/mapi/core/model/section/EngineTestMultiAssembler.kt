package ru.yandex.market.mapi.core.model.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.contract.ResolverClientResponse

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
@Service
class EngineTestMultiAssembler : AbstractAssembler<EngineTestSnippet>() {
    override fun getName() = "EngineTestMulti"
    override fun getSnippetClass() = EngineTestSnippet::class

    companion object {
        val RES_FIRST = "firstResolver"
        val RES_SECOND = "secondResolver"

        val RES_FIRST_WITH_VERSION = "${RES_FIRST}V1"
        val RES_SECOND_WITH_VERSION = "${RES_SECOND}V1"

        private val RES_TYPE_MAP = mapOf(
            RES_FIRST_WITH_VERSION to TestResponseFirst::class,
            RES_SECOND_WITH_VERSION to TestResponseSecond::class,
        )
    }

    override fun convert(
        rawResponseList: List<ResolverClientResponse>,
        configNode: JsonNode?,
        builder: AssemblyResult.Builder
    ) {
        // this is default approach to parse multi-resolver responses
        // key-type contract is specified above, result are always parsed correctly
        val responseMap = parseMap(rawResponseList, RES_TYPE_MAP, builder)
        val first = responseMap[RES_FIRST_WITH_VERSION] as TestResponseFirst?
        val second = responseMap[RES_SECOND_WITH_VERSION] as TestResponseSecond?

        if (first != null) {
            builder.tryAdd {
                EngineTestSnippet(
                    first.expectedField,
                    1
                )
            }
        }
        if (second != null) {
            builder.tryAdd {
                EngineTestSnippet(
                    second.expectedField,
                    2
                )
            }
        }
    }

    data class TestResponseFirst(
        val expectedField: String,
        var sku: String? = null
    )

    data class TestResponseSecond(
        val expectedField: String,
    )
}
