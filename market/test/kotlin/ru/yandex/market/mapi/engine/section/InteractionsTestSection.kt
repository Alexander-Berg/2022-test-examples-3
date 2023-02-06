package ru.yandex.market.mapi.engine.section

import ru.yandex.market.mapi.core.assembler.AbstractAssembler
import ru.yandex.market.mapi.core.model.MapiError
import ru.yandex.market.mapi.core.model.action.SendAnalyticsAction
import ru.yandex.market.mapi.core.model.analytics.RecommendationLogParams
import ru.yandex.market.mapi.core.model.analytics.SectionProcessContext
import ru.yandex.market.mapi.core.model.screen.AbstractSection
import ru.yandex.market.mapi.core.util.AnalyticUtils.createSectionAnalytics
import ru.yandex.market.mapi.core.util.daoJsonTyped
import ru.yandex.market.mapi.engine.section.InteractionsTestAssembler.SectionParamsOk
import ru.yandex.market.mapi.engine.section.InteractionsTestAssembler.SectionParamsToFail
import kotlin.reflect.KClass

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.03.2022
 */
class InteractionsTestSection : AbstractSection() {

    override fun defaultAssembler(): KClass<out AbstractAssembler<*>> {
        return InteractionsTestAssembler::class
    }

    override fun mapSectionAnalyticName(): String {
        return "TEST_INTERACTION"
    }

    override fun doPostProcess(context: SectionProcessContext) {
        if (context.tryGetSectionParams(SectionParamsToFail::class) != null) {
            throw RuntimeException("Oops, section failed")
        }

        if (context.content.find { x -> x is InteractionsTestSnippet && x.type == "hideSection" } != null) {
            context.hideSection = true
            context.errors.add(MapiError.SECTION_HIDDEN_BY_CODE_RULES.toDto("Decided to hide section"))
            context.errors.add(MapiError.SECTION_PARSE_ERROR.toDto("Test error to log in metrics"))
            return
        }

        val assemblyParam = context.tryGetSectionParams(SectionParamsOk::class)

        val onCustom = SendAnalyticsAction(
            events = listOfNotNull(
                context.createSectionAnalytics(this, "CUSTOM"),
                context.buildRecomEvent("customRecom", buildRecomParams(assemblyParam))
            )
        )

        actions = daoJsonTyped(
            "onCustom" to onCustom
        )
    }

    private fun buildRecomParams(assemblyParam: SectionParamsOk?): RecommendationLogParams? {
        if (assemblyParam == null) {
            return null
        }

        return RecommendationLogParams(
            item = mapOf("customType" to assemblyParam.type)
        )
    }
}
