package ru.yandex.direct.grid.processing.util

import graphql.ExecutionResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.yandex.altay.model.language.LanguageOuterClass
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.grid.model.GdStatPreset
import ru.yandex.direct.grid.model.GdStatRequirements
import ru.yandex.direct.grid.model.campaign.GdCampaignType
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.PUBLIC_GRAPH_QL_PROCESSOR
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.TestGdPromoExtension
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContext
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsWeeklyBudget
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCountersByDomain
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCountersByDomainPayload
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCountersInfoContainer
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCountersInfoPayload
import ru.yandex.direct.grid.processing.model.client.GdRequestMetrikaCountersAccess
import ru.yandex.direct.grid.processing.model.client.GdRequestMetrikaCountersAccessPayload
import ru.yandex.direct.grid.processing.model.client.GdShowCpmUacPromoParams
import ru.yandex.direct.grid.processing.model.client.GdSuggestDataByUrl
import ru.yandex.direct.grid.processing.model.client.GdSuggestDataByUrlPayload
import ru.yandex.direct.grid.processing.model.client.GdSuggestMetrikaDataByUrl
import ru.yandex.direct.grid.processing.model.client.GdSuggestMetrikaDataByUrlPayload
import ru.yandex.direct.grid.processing.model.goal.GdCampaignGoalFilter
import ru.yandex.direct.grid.processing.model.goal.GdCampaignGoalsContainer
import ru.yandex.direct.grid.processing.model.goal.GdCampaignGoalsRecommendedCostPerActionForNewCampaign
import ru.yandex.direct.grid.processing.model.goal.GdCampaignGoalsRecommendedCostPerActionInputItem
import ru.yandex.direct.grid.processing.model.goal.GdCampaignsGoalsRecommendedCostPerActionInput
import ru.yandex.direct.grid.processing.model.goal.GdGoal
import ru.yandex.direct.grid.processing.model.goal.GdGoalsContext
import ru.yandex.direct.grid.processing.model.goal.mutation.GdGoalsForUpdateCampaigns
import ru.yandex.direct.grid.processing.model.goal.mutation.GdGoalsForUpdateCampaignsPayload
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaGoalsByCounter
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaGoalsByCounterPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionsContainer
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneCommitContainer
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneCommitPayload
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneSubmitContainer
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneSubmitPayload
import ru.yandex.direct.grid.processing.model.userphone.GdUpdateUserVerifiedPhonePayload
import ru.yandex.direct.grid.processing.model.userphone.GdUpdateVerifiedUserPhoneContainer
import ru.yandex.direct.grid.processing.model.userphone.GdUserPhonesPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper.buildContext
import ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize
import java.math.BigDecimal
import kotlin.reflect.KClass

@Service
class KtGraphQLTestExecutor(
    @Qualifier(GRAPH_QL_PROCESSOR) val processor: GridGraphQLProcessor,
    @Qualifier(PUBLIC_GRAPH_QL_PROCESSOR) val publicProcessor: GridGraphQLProcessor,
    val gridContextProvider: GridContextProvider) {

    fun withDefaultGraphQLContext(operator: User): KtGraphQLTestExecutor {
        return withGraphQLContext(buildContext(operator).apply { fetchedFieldsReslover = null })
    }

    fun withGraphQLContext(context: GridGraphQLContext): KtGraphQLTestExecutor {
        gridContextProvider.gridContext = context
        return this
    }

    fun getUserPhones(): GdUserPhonesPayload {
        return processor.doGraphQLRequest(
            buildQuery = { USER_PHONES_QUERY_TEMPLATE },
            fetchPayload = { data -> data["userPhones"] },
            payloadClass = GdUserPhonesPayload::class)
    }

    fun getUserPhonesPublic(): GdUserPhonesPayload {
        return publicProcessor.doGraphQLRequest(
            buildQuery = { USER_PHONES_QUERY_TEMPLATE },
            fetchPayload = { data -> data["userPhones"] },
            payloadClass = GdUserPhonesPayload::class)
    }

    fun updateUserVerifiedPhone(phoneId: Long): GdUpdateUserVerifiedPhonePayload {
        return processor.doGraphQLRequest(
            input = GdUpdateVerifiedUserPhoneContainer().apply { this.phoneId = phoneId },
            buildQuery = { serializedInput -> String.format(UPDATE_USER_VERIFIED_PHONE_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> data["updateUserVerifiedPhone"] },
            payloadClass = GdUpdateUserVerifiedPhonePayload::class)
    }

    fun updateUserVerifiedPhoneRaw(phoneId: Long): ExecutionResult {
        val request = GdUpdateVerifiedUserPhoneContainer().apply { this.phoneId = phoneId }
        val query = String.format(UPDATE_USER_VERIFIED_PHONE_QUERY_TEMPLATE, graphQlSerialize(request))

        return processor.processQuery(null, query, null, gridContextProvider.gridContext)
    }

    fun confirmAndBindPhoneSubmit(phone: String): GdConfirmAndBindPhoneSubmitPayload {
        return publicProcessor.doGraphQLRequest(
            input = GdConfirmAndBindPhoneSubmitContainer().apply { this.phone = phone },
            buildQuery = { serializedInput -> String.format(CONFIRM_AND_BIND_SUBMIT_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> data["confirmAndBindPhoneSubmit"] },
            payloadClass = GdConfirmAndBindPhoneSubmitPayload::class)
    }

    fun confirmAndBindPhoneCommit(trackId: String, code: String): GdConfirmAndBindPhoneCommitPayload {
        return publicProcessor.doGraphQLRequest(
            input = GdConfirmAndBindPhoneCommitContainer().apply {
                this.trackId = trackId
                this.code = code
            },
            buildQuery = { serializedInput -> String.format(CONFIRM_AND_BIND_COMMIT_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> data["confirmAndBindPhoneCommit"] },
            payloadClass = GdConfirmAndBindPhoneCommitPayload::class)
    }

    fun metrikaCountersByDomain(clientLogin: String, url: String): GdMetrikaCountersByDomainPayload {
        return processor.doGraphQLRequest(
            input = GdMetrikaCountersByDomain().apply { this.url = url },
            buildQuery = { serializedInput -> String.format(METRIKA_COUNTERS_BY_DOMAIN_QUERY_TEMPLATE, clientLogin, serializedInput) },
            fetchPayload = { data -> (data["client"] as LinkedHashMap<*, *>)["metrikaCountersByDomain"] },
            payloadClass = GdMetrikaCountersByDomainPayload::class)
    }

    fun showCpmUacParams(clientLogin: String): GdShowCpmUacPromoParams {
        return processor.doGraphQLRequest(
            buildQuery = { serializedInput -> String.format(SHOW_CPM_UAC_PROMO_PARAMS_QUERY_TEMPLATE, clientLogin) },
            fetchPayload = { data -> (data["client"] as LinkedHashMap<*, *>)["showCpmUacPromoParams"] },
            payloadClass = GdShowCpmUacPromoParams::class)
    }

    fun cpmUacNewCamp(clientLogin: String): Boolean {
        return processor.doGraphQLRequest(
            buildQuery = { String.format(CPM_UAC_NEW_CAMP_QUERY_TEMPLATE, clientLogin) },
            fetchPayload = { data -> (data["client"] as LinkedHashMap<*, *>)["cpmUacNewCamp"] },
            payloadClass = Boolean::class)
    }

    fun metrikaCountersInfo(counterIds: Set<Long>): GdMetrikaCountersInfoPayload {
        return processor.doGraphQLRequest(
            input = GdMetrikaCountersInfoContainer().apply { this.counterIds = counterIds },
            buildQuery = { serializedInput -> String.format(METRIKA_COUNTERS_INFO_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> data["metrikaCountersInfo"] },
            payloadClass = GdMetrikaCountersInfoPayload::class)
    }

    fun requestMetrikaCountersAccess(counterIds: Set<Long>): GdRequestMetrikaCountersAccessPayload {
        return processor.doGraphQLRequest(
            input = GdRequestMetrikaCountersAccess().apply { this.counterIds = counterIds },
            buildQuery = { serializedInput -> String.format(REQUEST_METRIKA_COUNTERS_ACCESS_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> data["requestMetrikaCountersAccess"] },
            payloadClass = GdRequestMetrikaCountersAccessPayload::class)
    }

    fun getCampaignIdToPromoExtension(login: String, ids: Set<Long>): Map<Long, TestGdPromoExtension?> {
        val campaignsContainer: GdCampaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
        campaignsContainer.filter.campaignIdIn = ids
        return processor.doGraphQLRequest(
            input = campaignsContainer,
            buildQuery = { serializedInput -> String.format(CAMPAIGNS_PROMO_EXTENSION_QUERY_TEMPLATE, login, serializedInput) },
            fetchPayload = { data -> ((data["client"] as Map<*, *>)["campaigns"] as Map<*, *>)["rowset"] },
            payloadClass = List::class
        ).associate { (it as Map<*, *>)["id"] as Long to
            it["promoExtension"]?.let{ promo -> convertValue(promo, TestGdPromoExtension::class.java) }
        }
    }

    fun getPromoExtensionList(
        login: String,
        promoExtensionContainer: GdPromoExtensionsContainer,
    ): List<TestGdPromoExtension> {
        return processor.doGraphQLRequest(
            input = promoExtensionContainer,
            buildQuery = { serializedInput -> String.format(PROMO_EXTENSIONS_QUERY_TEMPLATE, login, serializedInput) },
            fetchPayload = { data -> ((data["client"] as Map<*, *>)["promoExtensions"] as Map<*, *>)["rowset"] },
            payloadClass = List::class
        ).map {
            promo -> convertValue(promo, TestGdPromoExtension::class.java)
        }
    }

    fun getGeoSuggest(login: String): List<Long> {
        return processor.doGraphQLRequest(
            buildQuery = { String.format(GEO_SUGGEST_TEMPLATE, login) },
            fetchPayload = { data -> (data["client"] as LinkedHashMap<*, *>)["geoSuggest"] },
            payloadClass = List::class) as List<Long>
    }

    fun suggestDataByUrl(url: String): GdSuggestDataByUrlPayload {
        return processor.doGraphQLRequest(
            input = GdSuggestDataByUrl().apply { this.url = url; language = LanguageOuterClass.Language.RU },
            buildQuery = { serializedInput -> String.format(SUGGEST_DATA_BY_URL_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> getDataWithGoalType(data["suggestDataByUrl"] as Map<*, *>) },
            payloadClass = GdSuggestDataByUrlPayload::class)
    }

    fun suggestMetrikaDataByUrl(
        url: String,
        additionalCounterIds: Set<Long>? = null
    ): GdSuggestMetrikaDataByUrlPayload {
        val input = GdSuggestMetrikaDataByUrl().apply {
            this.url = url
            this.additionalCounterIds = additionalCounterIds
        }
        return processor.doGraphQLRequest(
            input = input,
            buildQuery = { serializedInput -> String.format(SUGGEST_METRIKA_DATA_BY_URL_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> getDataWithGoalType(data["suggestMetrikaDataByUrl"] as Map<*, *>) },
            payloadClass = GdSuggestMetrikaDataByUrlPayload::class)
    }

    fun getTextCampaignsWithStatsAndAccess(
        uid: Long,
        campaignIds: Set<Long>,
        useCampaignGoalIds: Boolean = true,
        goalIds: Set<Long> = setOf()
    ): GdCampaignsContext {
        val campaignsContainer: GdCampaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
        campaignsContainer.filter.campaignIdIn = campaignIds
        campaignsContainer.statRequirements = GdStatRequirements().apply {
            this.preset = GdStatPreset.LAST_30DAYS
            this.useCampaignGoalIds = useCampaignGoalIds
            this.goalIds = goalIds
        }
        return processor.doGraphQLRequest(
            input = campaignsContainer,
            buildQuery = { serializedInput -> String.format(CAMPAIGNS_WITH_STATS_AND_ACCESS_QUERY_TEMPLATE, uid, serializedInput) },
            fetchPayload = { data ->
                val newMap = HashMap((data["client"] as Map<*, *>)["campaigns"] as Map<*, *>)
                newMap.computeIfPresent("rowset") { _, campaigns ->
                    (campaigns as List<*>).map { campaignMap ->
                        val mapWithType = HashMap(campaignMap as Map<*, *>)
                        mapWithType["_type"] = "GdTextCampaign"
                        mapWithType
                    }.toList()
                }
                newMap
            },
            payloadClass = GdCampaignsContext::class
        )
    }

    fun getMetrikaGoalsByCounter(vararg counterIds: Long, campaignId: Long? = null): GdMetrikaGoalsByCounterPayload {
        val input = GdMetrikaGoalsByCounter().apply {
            this.counterIds = counterIds.toList()
            this.campaignId = campaignId
            campaignType = GdCampaignType.TEXT
        }
        return processor.doGraphQLRequest(
            input = input,
            buildQuery = { serializedInput -> String.format(GET_METRIKA_GOALS_BY_COUNTER_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> getDataWithGoalType(data["getMetrikaGoalsByCounter"] as Map<*, *>) },
            payloadClass = GdMetrikaGoalsByCounterPayload::class)
    }

    fun getGoalsForUpdateCampaigns(vararg campaignIds: Long): GdGoalsForUpdateCampaignsPayload {
        val input = GdGoalsForUpdateCampaigns().apply {
            this.campaignIds = campaignIds.toList()
        }
        return processor.doGraphQLRequest(
            input = input,
            buildQuery = { serializedInput -> String.format(GET_GOALS_FOR_UPDATE_CAMPAIGNS_QUERY_TEMPLATE, serializedInput) },
            fetchPayload = { data -> getDataWithGoalType(data["getGoalsForUpdateCampaigns"] as Map<*, *>) },
            payloadClass = GdGoalsForUpdateCampaignsPayload::class)
    }

    fun getRecommendedGoalsCostPerActionForNewCampaign(
        uid: Long,
        goalIds: List<Long>,
    ): Map<Long, BigDecimal> {
        val input = GdCampaignGoalsRecommendedCostPerActionForNewCampaign().apply {
            this.goalIds = goalIds
        }
        return processor.doGraphQLRequest(
            input = input,
            buildQuery = { serializedInput -> String.format(
                GET_RECOMMENDED_GOALS_COST_PER_ACTION_FOR_NEW_CAMPAIGN_QUERY_TEMPLATE, uid, serializedInput) },
            fetchPayload = { data -> ((data["client"] as Map<*, *>)
                ["getRecommendedGoalsCostPerActionForNewCampaign"] as Map<*, *>)["recommendedGoalsCostPerAction"] },
            payloadClass = List::class)
            .associate {
                (it as Map<*, *>)["id"] as Long to it["costPerAction"] as BigDecimal
            }
    }

    fun getRecommendedCampaignsGoalsCostPerAction(
        uid: Long,
        campaignId: Long,
        goalIds: List<Long>,
    ): Map<Long, BigDecimal> {
        val input = GdCampaignsGoalsRecommendedCostPerActionInput().apply {
            items = listOf(GdCampaignGoalsRecommendedCostPerActionInputItem().apply {
                this.campaignId = campaignId
                this.goalIds = goalIds
                this.url = "https://somesite.com"
            })
        }
        return processor.doGraphQLRequest(
            input = input,
            buildQuery = { serializedInput -> String.format(
                GET_RECOMMENDED_CAMPAIGNS_GOALS_COST_PER_ACTION_QUERY_TEMPLATE, uid, serializedInput) },
            fetchPayload = { data -> ((data["client"] as Map<*, *>)
                ["getRecommendedCampaignsGoalsCostPerAction"] as Map<*, *>)["recommendedCampaignsGoalsCostPerAction"] },
            payloadClass = List::class)[0]
            .let {
                val goalsCpa = ((it as Map<*, *>)["recommendedGoalsCostPerAction"] as List<*>)
                goalsCpa.associate { goalCpa ->
                    (goalCpa as Map<*, *>)["id"] as Long to goalCpa["costPerAction"] as BigDecimal
                }
            }
    }

    fun getCampaignGoals(uid: Long, campaignIds: Set<Long>?): List<GdGoal> {
        val input = GdCampaignGoalsContainer().apply {
            filter = GdCampaignGoalFilter().apply {
                campaignIdIn = campaignIds
            }
        }
        return processor.doGraphQLRequest(
            input = input,
            buildQuery = { serializedInput -> String.format(GET_CAMPAIGN_GOALS_QUERY_TEMPLATE, uid, serializedInput) },
            fetchPayload = { data -> getDataWithGoalType((data["client"] as Map<*, *>)["campaignGoals"] as Map<*, *>) },
            payloadClass = GdGoalsContext::class).rowset
    }

    fun addTextAdGroup(adGroup: GdAddTextAdGroupItem): GdAddAdGroupPayload {
        return processor.doGraphQLRequest(
            input = GdAddTextAdGroup().withAddItems(listOf(adGroup)),
            buildQuery = { serializedInput -> String.format(ADD_ADGROUP_TEMPLATE, "addTextAdGroups", serializedInput) },
            fetchPayload = { data -> data["addTextAdGroups"] },
            payloadClass = GdAddAdGroupPayload::class)
    }

    fun updateTextAdGroup(adGroup: GdUpdateTextAdGroupItem): GdUpdateAdGroupPayload {
        return processor.doGraphQLRequest(
            input = GdUpdateTextAdGroup().withUpdateItems(listOf(adGroup)),
            buildQuery = { serializedInput -> String.format(UPDATE_ADGROUP_TEMPLATE, "updateTextAdGroup", serializedInput) },
            fetchPayload = { data -> data["updateTextAdGroup"] },
            payloadClass = GdUpdateAdGroupPayload::class)
    }

    private fun getDataWithGoalType(data: Map<*, *>): Map<*, *> {
        val newMap = HashMap(data)
        newMap.computeIfPresent("goals") { _, goals -> getGoalsWithType(goals as List<*>) }
        newMap.computeIfPresent("rowset") { _, goals -> getGoalsWithType(goals as List<*>) }
        return newMap
    }

    private fun getGoalsWithType(goals: List<*>) =
        goals.map { goalMap ->
            val mapWithType = HashMap(goalMap as Map<*, *>)
            mapWithType["_type"] = "GdGoal"
            mapWithType
        }.toList()

    fun updateWeeklyBudgetRequest(updateBudgetRequest : GdUpdateCampaignsWeeklyBudget): GdUpdateCampaignPayload {
        return processor.doGraphQLRequest(
            input = updateBudgetRequest,
            buildQuery = { serializedInput -> String.format(UPDATE_WEEKLY_BUDGET_MUTATION_TEMPLATE, serializedInput) },
            fetchPayload = { data -> data["updateCampaignsWeeklyBudget"] },
            payloadClass = GdUpdateCampaignPayload::class)
    }

    private fun <PayloadT : Any> GridGraphQLProcessor.doGraphQLRequest(
        input: Any? = null,
        buildQuery: (request: Any) -> String,
        fetchPayload: (payloadRaw: Map<String, Any>) -> Any?,
        payloadClass: KClass<PayloadT>
    ): PayloadT {
        val serializedInput = graphQlSerialize(input)
        val query = buildQuery(serializedInput)

        val result = processQuery(null, query, null, gridContextProvider.gridContext)
        checkErrors(result.errors)

        val data: Map<String, Any> = result.getData()
        val payloadRaw = fetchPayload(data)
        return convertValue(payloadRaw, payloadClass.java)
    }
}
