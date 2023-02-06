package ru.yandex.market.mapi.core.model.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AssemblyResult

/**
 * @author: Anastasia Fakhrieva | afakhrieva@
 * Date: 28.07.2022
 */
@Service
class EngineTestPagedByPageNumWithSectionParamsAssembler: EngineTestPagedByPageNumAssembler() {
    override fun getName() = "EngineTestPagedByPageNumWithSectionParams"

    override fun doConvert(response: TestResponse, configNode: JsonNode?, builder: AssemblyResult.Builder) {
        super.doConvert(response, configNode, builder)
        builder.addSectionEventParams(AssemblyResult.NextPageSectionParams(mapOf(
            "testSectionParam" to "page"
        )))
    }
}
