package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.eq
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.common.net.NetAcl
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.model.UacSegmentInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbqueue.LimitOffset.maxLimited
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.RetargetingConditionRequest
import java.net.InetAddress

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacRetargetingUpdateControllerTest : UacRetargetingControllerTestBase() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var uacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var netAcl: NetAcl

    @Before
    fun localBefore() {
        grutSteps.createClient(userInfo.clientInfo!!)

        dbQueueSteps.registerJobType(UAC_CHECK_AUDITION_SEGMENTS)
        dbQueueSteps.clearQueue(UAC_CHECK_AUDITION_SEGMENTS)
        Mockito.`when`(yaAudienceClient.getSegments(eq(userInfo.login)))
            .thenReturn(
                listOf(
                    AudienceSegment()
                        .withStatus(SegmentStatus.IS_PROCESSED)
                        .withId(audienceGoals[0].id)
                        .withName("Uploaded")
                )
            )
        Mockito.doReturn(false).`when`(netAcl).isInternalIp(ArgumentMatchers.any(InetAddress::class.java))
    }

    @After
    fun localAfter() {
        Mockito.reset(yaAudienceClient, netAcl)
    }

    @Suppress("unused")
    private fun isFeatureEnabled() = listOf(true, false)

    @Test
    @TestCaseName("Test case for multiple ad groups feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun `update campaign retargeting conditions`(isFeatureEnabled: Boolean) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, isFeatureEnabled)

        val campaignRetargetingCondition = createRetargetingCondition()
        val id = campaignRetargetingCondition.id!!
        val campaignId1 = grutSteps
            .createMobileAppCampaign(userInfo.clientInfo!!, retargetingCondition = campaignRetargetingCondition)
        val campaignId2 = grutSteps
            .createMobileAppCampaign(userInfo.clientInfo!!, retargetingCondition = campaignRetargetingCondition)

        val requestGoals = listOf(
            createUacRetargetingConditionRuleGoal(
                id = mobileAppGoalIds[1],
                type = UacRetargetingConditionRuleGoalType.LAL
            ),
            createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0]),
            createUacRetargetingConditionRuleGoal(
                id = audienceGoals[0].id,
                type = UacRetargetingConditionRuleGoalType.AUDIENCE
            ),
            UacRetargetingConditionRuleGoal(
                type = UacRetargetingConditionRuleGoalType.LAL_SEGMENT,
                segmentInfo = UacSegmentInfo(
                    name = null,
                    counterId = 123,
                    domain = null,
                ),
                id = segment.id.toLong(),
            ),
        )
        val updateRequest = createRequest(requestGoals)

        val response = getResponse(updateRequest, id)

        val expectedResponse = listOf(
            UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.OR,
                        goals = listOf(
                            requestGoals[0].copy(name = mobileAppGoalNameById[mobileAppGoalIds[1]]),
                            requestGoals[1].copy(name = mobileAppGoalNameById[mobileAppGoalIds[0]]),
                            requestGoals[2],
                            requestGoals[3].copy(time = 540),
                        ),
                    ),
                ),
                name = updateRequest.name, id = id
            )
        )

        val soft = SoftAssertions()
        soft.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(expectedResponse))
        )

        val lalGoal = lalSegmentRepository.getLalSegmentsByParentIds(listOf(mobileAppGoalIds[1])).first()
        val lalSegment = lalSegmentRepository.getLalSegmentsByParentIds(listOf(segment.id.toLong())).first()
        val directRetargetingCondition = getDirectRetargetingCondition()
        soft.assertThat(directRetargetingCondition.rules).hasSize(1)
        val directRule = directRetargetingCondition.rules[0]
        soft.assertThat(directRule.goals).hasSize(4)
        directRule.goals[0].assert(soft, lalGoal.id, GoalType.LAL_SEGMENT)
        directRule.goals[1].assert(soft, mobileAppGoalIds[0], GoalType.MOBILE)
        directRule.goals[2].assert(soft, audienceGoals[0].id, GoalType.AUDIENCE)
        directRule.goals[3].assert(soft, lalSegment.id, GoalType.LAL_SEGMENT)

        val campaign1 = uacCampaignService.getCampaignById(campaignId1.toIdString())!!
        val campaign2 = uacCampaignService.getCampaignById(campaignId2.toIdString())!!

        for (campaign in listOf(campaign1, campaign2)) {
            soft.assertThat(campaign.retargetingCondition?.name).isEqualTo("Измененное условие ретаргетинга")
            soft.assertThat(campaign.retargetingCondition?.conditionRules).hasSize(1)
            val rule = campaign.retargetingCondition?.conditionRules!![0]
            soft.assertThat(rule.type)
                .isEqualTo(UacRetargetingConditionRule.RuleType.OR)
            soft.assertThat(rule.goals).hasSize(4)

            rule.goals[0].assert(soft, mobileAppGoalIds[1], UacRetargetingConditionRuleGoalType.LAL)
            rule.goals[1].assert(soft, mobileAppGoalIds[0], UacRetargetingConditionRuleGoalType.MOBILE)
            rule.goals[2].assert(soft, audienceGoals[0].id, UacRetargetingConditionRuleGoalType.AUDIENCE)
            rule.goals[3].assert(soft, segment.id.toLong(), UacRetargetingConditionRuleGoalType.LAL_SEGMENT)
        }
        soft.assertAll()
    }

    @Test
    @TestCaseName("Test case for check audience segments feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun `update ad group brief retargeting conditions`(isFeatureEnabled: Boolean) {
        steps.featureSteps().addClientFeature(
            userInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, true
        )
        steps.featureSteps().addClientFeature(
            userInfo.clientId, FeatureName.CHECK_AUDIENCE_SEGMENTS_DEFERRED, isFeatureEnabled
        )

        val retargetingCondition1 = createRetargetingCondition()
        val campaignId1 = grutSteps.createMobileAppCampaign(
            userInfo.clientInfo!!,
            retargetingCondition = retargetingCondition1
        )
        val adGroupBriefId11 = grutSteps.createAdGroupBrief(campaignId1, retargetingCondition1)
        val adGroupBriefId12 = grutSteps.createAdGroupBrief(campaignId1, retargetingCondition1)

        val retargetingCondition2 = createRetargetingCondition()
        val campaignId2 = grutSteps.createMobileAppCampaign(userInfo.clientInfo!!)
        val adGroupBriefId21 = grutSteps.createAdGroupBrief(campaignId2, retargetingCondition1)
        val adGroupBriefId22 = grutSteps.createAdGroupBrief(campaignId2, retargetingCondition2)

        val updateRequest = createRequestWithOneGoal(
            audienceGoals[0].id, UacRetargetingConditionRuleGoalType.AUDIENCE
        )
        val updatingRetargetingConditionId = retargetingCondition1.id!!
        val response = getResponse(updateRequest, updatingRetargetingConditionId)
        val expectedResponse = listOf(
            UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.OR,
                        goals = listOf(
                            updateRequest.conditionRules[0].goals[0].copy(time = 540),
                        ),
                    ),
                ),
                name = updateRequest.name, id = updatingRetargetingConditionId
            )
        )
        val soft = SoftAssertions()
        soft.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(expectedResponse))
        )

        val directRetargetingCondition = getDirectRetargetingCondition()
        soft.assertThat(directRetargetingCondition.rules).hasSize(1)
        soft.assertThat(directRetargetingCondition.rules[0].goals).hasSize(1)
        directRetargetingCondition.rules[0].goals[0].assert(soft, audienceGoals[0].id, GoalType.AUDIENCE)

        val campaign1 = uacCampaignService.getCampaignById(campaignId1.toIdString())!!
        soft.assertThat(campaign1.retargetingCondition?.name).isEqualTo("Измененное условие ретаргетинга")
        soft.assertThat(campaign1.retargetingCondition?.conditionRules)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("goals.time")
            .containsExactlyInAnyOrderElementsOf(updateRequest.conditionRules)

        val adGroupBriefsById = grutApiService.adGroupBriefGrutApi.getAdGroupBriefs(
            setOf(adGroupBriefId11, adGroupBriefId12, adGroupBriefId21, adGroupBriefId22)
        ).associateBy { it.id!! }
        soft.assertThat(adGroupBriefsById).`as`("В груте 4 групповых заявки").hasSize(4)

        soft.assertThat(adGroupBriefsById[adGroupBriefId22]!!.retargetingCondition)
            .`as`("В групповой заявке с необновляемым условием не должно поменяться условие")
            .isEqualTo(retargetingCondition2)

        adGroupBriefsById.filterKeys {
            setOf(adGroupBriefId11, adGroupBriefId12, adGroupBriefId21).contains(it)
        }.forEach { (_, adGroupBrief) ->
            soft.assertThat(adGroupBrief.retargetingCondition?.name).isEqualTo("Измененное условие ретаргетинга")
            soft.assertThat(adGroupBrief.retargetingCondition?.conditionRules)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("goals.time")
                .containsExactlyInAnyOrderElementsOf(updateRequest.conditionRules)
        }

        val jobs = dbQueueRepository.getJobsByJobTypeAndClientIds(
            userInfo.shard, UAC_CHECK_AUDITION_SEGMENTS, listOf(userInfo.clientId.asLong()), maxLimited()
        )
        if (!isFeatureEnabled) {
            soft.assertThat(jobs).`as`("джоба не должна была поставиться в очередь").isEmpty()
        } else {
            soft.assertThat(jobs).`as`("должны были появиться в очереди две джобы").hasSize(2)
            val jobCampaignIds = jobs.map { it.args.uacCampaignId.toIdLong() }
            soft.assertThat(jobCampaignIds)
                .`as`("джобы в очереди имеют правильный идентификатор кампании в параметрах")
                .containsExactlyInAnyOrder(campaignId1, campaignId2)
        }
        soft.assertAll()
    }

    @Test
    fun testUpdate_WithLalAudienceAndNullTime() {
        val campaignRetargetingCondition = createRetargetingCondition()
        val id = campaignRetargetingCondition.id!!
        val campaignId = grutSteps
            .createMobileAppCampaign(userInfo.clientInfo!!, retargetingCondition = campaignRetargetingCondition)

        val updateRequest = createRequestWithOneGoal(
            audienceGoals[0].id, UacRetargetingConditionRuleGoalType.LAL_AUDIENCE
        )

        val response = getResponse(updateRequest, id)

        val expectedResponse = listOf(
            UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.OR,
                        goals = listOf(
                            updateRequest.conditionRules[0].goals[0].copy(time = 540),
                        ),
                    ),
                ),
                name = updateRequest.name, id = id
            )
        )
        val soft = SoftAssertions()
        soft.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(expectedResponse))
        )

        val directRetargetingCondition = getDirectRetargetingCondition()
        soft.assertThat(directRetargetingCondition.rules).hasSize(1)
        val lalAudience = lalSegmentRepository.getLalSegmentsByParentIds(listOf(audienceGoals[0].id.toLong())).first()
        directRetargetingCondition.rules[0].goals[0].assert(soft, lalAudience.id, GoalType.LAL_SEGMENT)

        val campaign = uacCampaignService.getCampaignById(campaignId.toIdString())!!

        soft.assertThat(campaign.retargetingCondition?.name).isEqualTo("Измененное условие ретаргетинга")
        soft.assertThat(campaign.retargetingCondition?.conditionRules).hasSize(1)

        campaign.retargetingCondition?.conditionRules!![0].goals[0].assert(
            soft, audienceGoals[0].id, UacRetargetingConditionRuleGoalType.LAL_AUDIENCE
        )

        soft.assertAll()
    }

    @Test
    @TestCaseName("Test case for check audience segments feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun testUpdate_WithProcessedSegments(isFeatureEnabled: Boolean) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.CHECK_AUDIENCE_SEGMENTS_DEFERRED, isFeatureEnabled)

        val campaignRetargetingCondition = createRetargetingCondition()
        val id = campaignRetargetingCondition.id!!
        val campaignId = grutSteps.createMobileAppCampaign(
            userInfo.clientInfo!!,
            retargetingCondition = campaignRetargetingCondition
        )

        val updateRequest = createRequestWithOneGoal(
            audienceGoals[0].id, UacRetargetingConditionRuleGoalType.AUDIENCE
        )
        getResponse(updateRequest, id)
        val jobId = dbQueueSteps.getLastJobByType(userInfo.shard, UAC_CHECK_AUDITION_SEGMENTS)
        if (!isFeatureEnabled) {
            Assert.assertThat("джоба не должна была поставиться в очередь", jobId, Matchers.nullValue())
        } else {
            Assert.assertThat("джоба должна была появиться в очереди", jobId, Matchers.notNullValue())
            val dbJob = dbQueueRepository.findJobById(userInfo.shard, UAC_CHECK_AUDITION_SEGMENTS, jobId)
            Assert.assertThat("джоба должна была появиться в очереди", dbJob, Matchers.notNullValue())
            Assert.assertThat(
                "джоба в очереди имеет правильный идентификатор кампании в параметрах",
                dbJob?.args?.uacCampaignId,
                Matchers.`is`(campaignId.toIdString())
            )
        }
    }

    private fun getResponse(
        updateRequest: RetargetingConditionRequest,
        id: Long
    ): JsonNode? {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/retargeting/condition/${id}?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(updateRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        return JsonUtils.MAPPER.readTree(result)["result"]
    }

    private fun createRetargetingCondition(): UacRetargetingCondition {
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[1])),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val directRetargetingCondition = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val id = retargetingConditionService
            .addRetargetingConditions(listOf(directRetargetingCondition), userInfo.clientId)[0].result
        return UacRetargetingCondition(
            conditionRules = retargetingCondition.conditionRules,
            name = retargetingCondition.name,
            id = id,
        )
    }

    private fun createRequestWithOneGoal(goalId: Long, goalType: UacRetargetingConditionRuleGoalType) =
        createRequest(listOf(
            createUacRetargetingConditionRuleGoal(
                id = goalId,
                type = goalType,
                time = null
            )
        ))

    private fun createRequest(requestGoals: List<UacRetargetingConditionRuleGoal>) =
        RetargetingConditionRequest(
            mobileAppId = mobileApp.id,
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = requestGoals,
                ),
            ),
            name = "Измененное условие ретаргетинга"
        )

    private fun Goal.assert(
        soft: SoftAssertions,
        expectedId: Long,
        expectedType: GoalType,
    ) {
        soft.assertThat(this.id).isEqualTo(expectedId)
        soft.assertThat(this.type).isEqualTo(expectedType)
    }

    private fun UacRetargetingConditionRuleGoal.assert(
        soft: SoftAssertions,
        expectedId: Long,
        expectedType: UacRetargetingConditionRuleGoalType
    ) {
        soft.assertThat(this.id).isEqualTo(expectedId)
        soft.assertThat(this.type).isEqualTo(expectedType)
    }

    private fun getDirectRetargetingCondition() =
        retargetingConditionService.getRetargetingConditions(userInfo.clientId, null, LimitOffset.maxLimited())[0]
}
