package ru.yandex.market.mapi.section

import com.fasterxml.jackson.annotation.JsonInclude
import ru.yandex.market.mapi.core.assembler.AssemblyResult

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.06.2022
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class TestAssemblyResult(result: AssemblyResult) {
    val content = result.content
    val errors = result.errors
    val nextPageToken = result.sectionEventParams
        ?.firstOrNull { param -> param is AssemblyResult.NextPageToken }
    val nextPageSectionParams = result.sectionEventParams
        ?.firstOrNull {param -> param is AssemblyResult.NextPageSectionParams}
}
