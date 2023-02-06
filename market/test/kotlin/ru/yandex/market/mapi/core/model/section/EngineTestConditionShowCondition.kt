package ru.yandex.market.mapi.core.model.section

import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.model.MapiError
import ru.yandex.market.mapi.core.model.MapiErrorDto
import ru.yandex.market.mapi.core.model.screen.AbstractShowCondition
import ru.yandex.market.mapi.core.model.screen.ShowConditionResult
import ru.yandex.market.mapi.core.model.screen.ShowResultType

class EngineTestConditionShowCondition(
    val showResult: ShowResultType
) : AbstractShowCondition() {
    var errorType: MapiError? = null
    var errorMessage: String? = null

    override fun needShow(context: MapiContext): ShowConditionResult {
        return ShowConditionResult(
            showResult = showResult,
            error = errorType?.toDto(errorMessage)
        )
    }
}
