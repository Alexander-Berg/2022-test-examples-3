package ru.yandex.market.mapi.engine.section

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.MapiConstants
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.model.action.CommonActions
import ru.yandex.market.mapi.core.model.action.SendAnalyticsAction
import ru.yandex.market.mapi.core.model.analytics.RecommendationLogParams
import ru.yandex.market.mapi.core.model.analytics.SnippetEventParams
import ru.yandex.market.mapi.core.model.analytics.SnippetProcessContext
import ru.yandex.market.mapi.core.util.AnalyticUtils.createSnippetAnalytics
import ru.yandex.market.mapi.core.util.daoJson
import ru.yandex.market.mapi.core.util.daoJsonTyped
import ru.yandex.market.mapi.engine.section.InteractionsTestAssembler.Response

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.03.2022
 */
@Service
class InteractionsTestAssembler : AbstractSimpleAssembler<Response, InteractionsTestSnippet>() {
    override fun getName() = "InteractionsTest"
    override fun getResponseClass() = Response::class
    override fun getSnippetClass() = InteractionsTestSnippet::class

    override fun doConvert(response: Response, configNode: JsonNode?, builder: AssemblyResult.Builder) {
        if (response.type == "fail-section-params") {
            builder.addSectionEventParams(SectionParamsToFail())
        } else if (response.type != null) {
            builder.addSectionEventParams(SectionParamsOk(response.type))
        }

        builder.addResolverEventParams(
            daoJson(
                "resParam" to "custom",
                "type" to response.type
            )
        )

        for (item in response.items) {
            builder.tryAdd {
                InteractionsTestSnippet(item.type, item.field).also { snippet ->
                    snippet.internal = InteractionsTestSnippet.Internal(item.custom)
                }
            }
        }
    }

    override fun fillStaticSnippet(snippet: InteractionsTestSnippet): Boolean {
        if (snippet.field == "invalid") {
            return false
        }
        return true
    }

    override fun postProcessSnippet(
        snippet: InteractionsTestSnippet,
        snippetContext: SnippetProcessContext
    ) {
        if (snippet.type == "fail-interaction") {
            throw RuntimeException("Oops, failed")
        }

        buildAppmetricaParams(snippetContext.snippetEventParams, snippet)

        val onShow = SendAnalyticsAction(
            events = listOfNotNull(
                snippetContext.createSnippetAnalytics("SHOW", once = true),
                snippetContext.buildRecomEvent("recomShow", buildRecomParams(snippet), once = true)
            )
        )
        val onClick = SendAnalyticsAction(
            events = listOfNotNull(
                snippetContext.createSnippetAnalytics("CLICK"),
                snippetContext.buildRecomEvent("recomShow", buildRecomParams(snippet))
            )
        )

        snippet.actions = daoJsonTyped(
            CommonActions.ON_SHOW to onShow,
            CommonActions.ON_CLICK to onClick,
        )
    }

    private fun buildAppmetricaParams(params: SnippetEventParams, snippet: InteractionsTestSnippet) {
        params.addParam("plusInfo", MapiContext.get().userPlusInfo?.plusInfo)

        val internal = snippet.internal ?: return
        params.addParam("customAppmetricaField", internal.custom)
    }

    private fun buildRecomParams(snippet: InteractionsTestSnippet): RecommendationLogParams? {
        val internal = snippet.internal ?: return null
        return RecommendationLogParams(
            item = daoJson(
                "recParam" to internal.custom,
                // checks pass-through of recomContext from cms response
                "recContext" to MapiContext.get().queryParams?.get(MapiConstants.PARAMS_RECOM_CONTEXT)
            )
        )
    }

    class Response(
        val items: List<ResponseItem>,
        val type: String? = null
    )

    class ResponseItem(
        val type: String,
        val field: String,
        val custom: String,
    )

    class SectionParamsToFail

    class SectionParamsOk(
        val type: String
    )
}
