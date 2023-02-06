package ru.yandex.market.mapi.engine.section.replaceId

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.model.screen.AbstractSnippet

/**
 * @author Arsen Salimov / maetimo@ / 04.07.2022
 */
@Service
class SectionIdReplaceTestAssembler : AbstractSimpleAssembler<SectionIdReplaceTestAssembler.Response, AbstractSnippet>() {
    override fun getName() = "SectionIdReplaceTestAssembler"
    override fun getResponseClass() = Response::class
    override fun getSnippetClass() = AbstractSnippet::class

    override fun doConvert(response: Response, configNode: JsonNode?, builder: AssemblyResult.Builder) {
        if (response.type == "replace-id-section-params") {
            builder.addSectionEventParams(SectionParamsToReplaceId())
        }
    }

    class Response(
        val type: String? = null
    )

    class SectionParamsToReplaceId
}
