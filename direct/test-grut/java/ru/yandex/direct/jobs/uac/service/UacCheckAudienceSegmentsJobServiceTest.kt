package ru.yandex.direct.jobs.uac.service

import com.google.common.collect.Lists
import com.nhaarman.mockitokotlin2.eq
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.AudienceSegmentsService.Companion.IS_PROCESSED_STATUSES
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.jobs.configuration.GrutJobsTest

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UacCheckAudienceSegmentsJobServiceTest {

    @Autowired
    private lateinit var uacCheckAudienceSegmentsJobService: UacCheckAudienceSegmentsJobService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var retargetingCondition: UacRetargetingCondition
    private lateinit var audienceGoal: Goal

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!
        grutSteps.createClient(clientInfo)
        audienceGoal = TestFullGoals.defaultAudience()
        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(listOf(audienceGoal))

        metrikaClientStub.addGoals(userInfo.uid, setOf(audienceGoal))
        retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = audienceGoal.id,
                            type = UacRetargetingConditionRuleGoalType.AUDIENCE,
                            time = 540
                        )
                    ),
                ),
            ),
            name = "Условие ретаргетинга"
        )
    }

    fun statuses() = arrayOf(
        arrayOf(true, SegmentStatus.PROCESSED),
        arrayOf(true, SegmentStatus.FEW_DATA),
        arrayOf(true, SegmentStatus.PROCESSING_FAILED),
        arrayOf(false, SegmentStatus.IS_PROCESSED),
        arrayOf(false, SegmentStatus.UPLOADED),
        arrayOf(false, SegmentStatus.FEW_DATA)
    )

    @ParameterizedTest(name = "Test uac check audience segments with campaign status {0} and segment status {1}")
    @MethodSource("statuses")
    fun testUpdate(
        statusShow: Boolean,
        segmentStatus: SegmentStatus
    ) {
        Mockito.`when`(yaAudienceClient.getSegments(eq(userInfo.login)))
            .thenReturn(
                listOf(
                    AudienceSegment()
                        .withStatus(segmentStatus)
                        .withId(audienceGoal.id)
                        .withName("Uploaded")
                )
            )

        val campaignId = createCampaignWithStatus(statusShow)
        val campaign = uacCheckAudienceSegmentsJobService.checkCampaignsAudienceSegments(
            campaignId, userInfo.clientId, userInfo.uid
        )
        val audienceSegmentSynced = segmentStatus !in IS_PROCESSED_STATUSES || !statusShow
        val soft = SoftAssertions()
        val expectedStatusShow = getNewStatusShow(statusShow, segmentStatus)
        soft.assertThat(campaign?.audienceSegmentsSynchronized).isEqualTo(audienceSegmentSynced)
        soft.assertThat(grutUacCampaignService.getCampaignStatusShow(userInfo.shard, campaignId.toIdLong())).isEqualTo(expectedStatusShow)
        soft.assertAll()
    }

    private fun createCampaignWithStatus(
        statusShow: Boolean
    ): String {
        val masterCampaignId = grutSteps.createMobileAppCampaign(
            clientInfo,
            createInDirect = true,
            retargetingCondition = retargetingCondition,
            audienceSegmentsSynchronized = false
        )
        if (!statusShow) {
            val campaign = grutUacCampaignService.getCampaignById(masterCampaignId.toIdString())!!
            grutUacCampaignService.updateDirectCampaignStatusShow(
                userInfo.user!!, userInfo.clientId, masterCampaignId, campaign, false
            )
        }
        return masterCampaignId.toIdString()
    }

    private fun getNewStatusShow(statusShow: Boolean, segmentStatus: SegmentStatus): Boolean {
        return if (!statusShow) {
            false
        } else segmentStatus !in listOf(SegmentStatus.FEW_DATA, SegmentStatus.PROCESSING_FAILED)
    }
}
