package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import java.util.stream.Collectors
import one.util.streamex.StreamEx
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.nullValue
import org.hamcrest.core.IsNull
import org.jooq.DSLContext
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsExternalTrackerRepository
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.regions.Region.BY_REGION_ID
import ru.yandex.direct.regions.Region.MOSCOW_REGION_ID
import ru.yandex.direct.regions.Region.RUSSIA_REGION_ID
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.utils.FunctionalUtils
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.model.RetargetingConditionRequest
import ru.yandex.direct.web.entity.uac.model.history.GrutHistoryOutputCategory
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutCampaignHistoryTest : BaseGrutCreateCampaignTest() {
    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppGoalsExternalTrackerRepository: MobileAppGoalsExternalTrackerRepository

    companion object {
        private const val KEYWORD1 = "девон-рекс"
        private const val KEYWORD2 = "сфинкс"
        private const val RETARGETING_NAME = "Условие ретаргетинга 1"
        private const val DISABLED_PAGE_ID = 15
    }

    @Test
    fun getHistory_demographicAdjustment() {
        val adjustment = UacAdjustmentRequest(
            null,
            null,
            AgePoint.AGE_35,
            15,
            null
        )

        val adjustmentPatchRequest = emptyPatchRequest().copy(
            adjustments = listOf(adjustment)
        )

        val campaignId = createAndUpdateEntry(adjustmentPatchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_DEMOGRAPHIC_ADJUSTMENT)
            .andExpect(jsonPath("$.result[0].old_state").isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.size()").value(1))
            .andExpect(jsonPath("$.result[0].new_state[0].age").value("age_35"))
            .andExpect(jsonPath("$.result[0].new_state[0].gender").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.result[0].new_state[0].percent").value(15))
    }

    @Test
    fun getHistory_regionalAdjustment() {
        val adjustment = UacAdjustmentRequest(
            RUSSIA_REGION_ID,
            null,
            null,
            15,
            null
        )

        val adjustmentPatchRequest = emptyPatchRequest().copy(
            adjustments = listOf(adjustment)
        )

        val campaignId = createAndUpdateEntry(adjustmentPatchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_REGIONAL_ADJUSTMENT)
            .andExpect(jsonPath("$.result[0].old_state").isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.size()").value(1))
            .andExpect(jsonPath("$.result[0].new_state[0].region").value(RUSSIA_REGION_ID))
            .andExpect(jsonPath("$.result[0].new_state[0].percent").value(15))
    }

    @Test
    fun getHistory_regionHistory() {
        val patchRequest = emptyPatchRequest().copy(
            regions = listOf(RUSSIA_REGION_ID, BY_REGION_ID)
        )
        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_REGIONS)
            .andExpect(jsonPath("$.result[0].old_state")
                .value(containsInAnyOrder(BY_REGION_ID.toInt())))
            .andExpect(jsonPath("$.result[0].new_state")
                .value(containsInAnyOrder(RUSSIA_REGION_ID.toInt(), BY_REGION_ID.toInt())))
    }

    @Test
    fun getHistory_minusRegionHistory() {
        val patchRequest = emptyPatchRequest().copy(
            regions = listOf(RUSSIA_REGION_ID),
            minusRegions = listOf(MOSCOW_REGION_ID)
        )

        val campaignId = createAndUpdateEntry(patchRequest, listOf(RUSSIA_REGION_ID))

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_MINUS_REGIONS)
            .andExpect(jsonPath("$.result[0].old_state")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state")
                .value(containsInAnyOrder(MOSCOW_REGION_ID.toInt())))
    }

    @Test
    fun getHistory_keywordsHistory() {
        val patchRequest = emptyPatchRequest().copy(
            keywords = listOf(KEYWORD1, KEYWORD2)
        )

        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_KEYWORDS)
            .andExpect(jsonPath("$.result[0].old_state")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state")
                .value(containsInAnyOrder(KEYWORD2, KEYWORD1)))
    }

    @Test
    fun getHistory_retargetingConditionHistory() {
        val campaignAppInfo = defaultAppInfo(
            appId = "com.yandex.browser",
            bundleId = "com.yandex.browser",
        )
        uacAppInfoRepository.saveAppInfo(campaignAppInfo)
        val campaignStoreUrl = googlePlayAppInfoGetter.appPageUrl(
            campaignAppInfo.appId,
            campaignAppInfo.region,
            campaignAppInfo.language
        )
        val campaignApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, campaignStoreUrl)

        val campaignId = createAndUpdateEntry(appInfo = campaignAppInfo)

        addRetargetingToCampaign(
            userInfo.shard,
            campaignApp.mobileApp,
            campaignApp,
            campaignId,
        )

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_RETARGETING_CONDITION)
            .andExpect(jsonPath("$.result[0].old_state")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.name")
                .value(RETARGETING_NAME))
            .andExpect(jsonPath("$.result[0].new_state.condition_rules.size()")
                .value(1))
    }

    @Test
    fun getHistory_audienceConditionHistory() {
        val patchRequest = emptyPatchRequest().copy(
            deviceTypes = setOf(DeviceType.PHONE_ANDROID)
        )

        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_AUDIENCE_CONDITION)
            .andExpect(jsonPath("$.result[0].old_state.socdem")
                .value(nullValue()))
            .andExpect(jsonPath("$.result[0].old_state.time_target")
                .value(nullValue()))
            .andExpect(jsonPath("$.result[0].old_state.device_types")
                .value(nullValue()))
            .andExpect(jsonPath("$.result[0].new_state.socdem")
                .value(nullValue()))
            .andExpect(jsonPath("$.result[0].new_state.time_target")
                .value(nullValue()))
            .andExpect(jsonPath("$.result[0].new_state.device_types")
                .value(contains("phone_android")))
    }

    @Test
    fun getHistory_minusKeywordsHistory() {
        val patchRequest = emptyPatchRequest().copy(
            minusKeywords = listOf(KEYWORD1, KEYWORD2)
        )

        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_MINUS_KEYWORDS)
            .andExpect(jsonPath("$.result[0].old_state")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state")
                .value(containsInAnyOrder(KEYWORD2, KEYWORD1)))
    }

    @Test
    fun getHistory_disabledPlacesHistory() {
        val patchRequest = emptyPatchRequest().copy(
            uacDisabledPlaces = UacDisabledPlaces(
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(DISABLED_PAGE_ID.toLong())
            )
        )

        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_DISABLED_PLACES)
            .andExpect(jsonPath("$.result[0].old_state")
                .value(nullValue()))
            .andExpect(jsonPath("$.result[0].new_state.disabled_places")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.disabled_video_ads_places")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.disabled_ips")
                .isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.disallowed_page_ids")
                .value(contains(DISABLED_PAGE_ID)))
    }

    @Test
    fun getHistory_emptyHistory() {
        val campaignId = createAndUpdateEntry(null)

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${campaignId}/history")
                .param("ulogin", clientInfo.login)
                .param("to", UacYdbUtils.toEpochSecond(LocalDateTime.now().plusMinutes(1)).toString())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.result").isEmpty)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun getHistory_multipleChangesHistory() {
        val adjustment = UacAdjustmentRequest(
            RUSSIA_REGION_ID,
            null,
            null,
            15,
            null
        )
        val patchRequest = emptyPatchRequest().copy(
            regions = listOf(RUSSIA_REGION_ID, BY_REGION_ID),
            adjustments = listOf(adjustment)
        )
        val campaignId = createAndUpdateEntry(patchRequest)

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${campaignId}/history")
                .param("ulogin", clientInfo.login)
                .param("to", UacYdbUtils.toEpochSecond(LocalDateTime.now().plusMinutes(1)).toString())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.result.size()").value(2))
            .andExpect(jsonPath("$.result[*].type").value(containsInAnyOrder(
                GrutHistoryOutputCategory.CAMPAIGN_REGIONS.toString(),
                GrutHistoryOutputCategory.CAMPAIGN_REGIONAL_ADJUSTMENT.toString())))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result[?(@.type == 'CAMPAIGN_REGIONAL_ADJUSTMENT')].old_state[0]").isEmpty)
            .andExpect(jsonPath("$.result[?(@.type == 'CAMPAIGN_REGIONAL_ADJUSTMENT')].new_state.size()")
                .value(1))
            .andExpect(jsonPath("$.result[?(@.type == 'CAMPAIGN_REGIONAL_ADJUSTMENT')].new_state[0].region")
                .value(RUSSIA_REGION_ID.toInt()))
            .andExpect(jsonPath("$.result[?(@.type == 'CAMPAIGN_REGIONAL_ADJUSTMENT')].new_state[0].percent")
                .value(15))
            .andExpect(jsonPath("$.result[?(@.type == 'CAMPAIGN_REGIONS')].old_state")
                .value(containsInAnyOrder(listOf(BY_REGION_ID.toInt()))))
            .andExpect(jsonPath("$.result[?(@.type == 'CAMPAIGN_REGIONS')].new_state")
                .value(containsInAnyOrder(listOf(RUSSIA_REGION_ID.toInt(), BY_REGION_ID.toInt()))))
    }

    @Test
    fun getHistory_strategyHistory() {
        val goalId = RandomNumberUtils.nextPositiveInteger().toLong()
        val patchRequest = emptyPatchRequest().copy(
            goals = listOf(UacGoal(goalId, null))
        )
        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_STRATEGY_OPTIONS)
            .andExpect(jsonPath("$.result[0].old_state.goals").isEmpty)
            .andExpect(jsonPath("$.result[0].new_state.goals[*].goal_id")
                .value(contains(goalId.toInt())))
    }

    @Test
    fun getHistory_assetLinks() {
        val contentId = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        val patchRequest = emptyPatchRequest().copy(
            contentIds = listOf(imageContentId, contentId)
        )
        val campaignId = createAndUpdateEntry(patchRequest)

        getHistoryRequest(campaignId, GrutHistoryOutputCategory.CAMPAIGN_ASSET_LINKS)
            .andExpect(jsonPath("$.result[0].old_state[*].id")
                .value(hasItem(imageContentId)))
            .andExpect(jsonPath("$.result[0].new_state[*].id")
                .value(hasItems(imageContentId, contentId)))
    }

    private fun getHistoryRequest(
        campaignId: String,
        historyType: GrutHistoryOutputCategory,
        expectedSize: Int = 1
    ): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${campaignId}/history")
                .param("ulogin", clientInfo.login)
                .param("to", UacYdbUtils.toEpochSecond(LocalDateTime.now().plusMinutes(1)).toString())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.result.size()").value(expectedSize))
            .andExpect(jsonPath("$.result[0].type").value(historyType.toString()))
            .andExpect(jsonPath("$.success").value(true))
    }

    private fun createAndUpdateEntry(
        patchRequest: PatchCampaignRequest? = null,
        regions: List<Long>? = listOf(BY_REGION_ID),
        appInfo: UacYdbAppInfo? = null
    ): String {
        val request = createUacCampaignRequest(
            contentIds = listOf(imageContentId),
            regions = regions,
            app = appInfo
        )
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns")
                .param("ulogin", clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(response)["result"]["id"].asText()

        if (patchRequest != null) updateCampaign(campaignId, patchRequest)

        return campaignId
    }

    private fun updateCampaign(campaignId: String?, patchRequest: PatchCampaignRequest) {
        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$campaignId")
                .param("ulogin", clientInfo.login)
                .content(JsonUtils.toJson(patchRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun addRetargetingToCampaign(
        shard: Int,
        mobileApp: MobileApp,
        campaignApp: MobileAppInfo,
        campaignId: String,
    ) {
        val externalTrackerEventNames = ExternalTrackerEventName.values().take(10).map { it.name }

        val mapIds = listOf(mobileApp.id)
        val mapIdsWithGoals = FunctionalUtils.listToSet(
            mobileAppGoalsExternalTrackerRepository.getEventsByAppIds(shard, mapIds, false)
        ) { obj: MobileExternalTrackerEvent -> obj.mobileAppId }

        val mobileGoals = StreamEx.of(mapIds)
            .filter { id: Long -> !mapIdsWithGoals.contains(id) }
            .cross(ExternalTrackerEventName.values().filter { it.name in externalTrackerEventNames })
            .mapKeyValue { id: Long?, en: ExternalTrackerEventName? ->
                MobileExternalTrackerEvent()
                    .withMobileAppId(id)
                    .withEventName(en)
                    .withCustomName("")
                    .withIsDeleted(false)
            }
            .collect(Collectors.toList())
        mobileAppGoalsExternalTrackerRepository.addEvents(shard, mobileGoals)

        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(campaignApp.mobileApp)
        )
        val goalIds = mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(campaignApp.mobileApp)).map { it.id }

        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = goalIds[9],
                            type = UacRetargetingConditionRuleGoalType.LAL,
                            time = 540
                        )
                    ),
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = campaignApp.mobileAppId,
        )

        val retargetingCondition = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        val conditionId = JsonUtils.fromJson(retargetingCondition)["result"][0]["id"].asLong()

        val patch = emptyPatchRequest().copy(
            retargetingCondition = UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.ALL,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(
                                id = goalIds[9],
                                type = UacRetargetingConditionRuleGoalType.LAL,
                                time = 540
                            )
                        ),
                    )
                ),
                name = RETARGETING_NAME,
                id = conditionId,
            )
        )
        updateCampaign(campaignId, patch)
    }
}
