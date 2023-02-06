package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.eq
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.web.configuration.GrutDirectWebTest

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignGrutTargetStatusControllerTest : UacCampaignTargetStatusControllerTestBase() {

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
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Before
    fun grutBefore() {
        steps.featureSteps().addClientFeature(
            userInfo.clientId,
            FeatureName.UC_UAC_CREATE_MOBILE_CONTENT_BRIEF_IN_GRUT_INSTEAD_OF_YDB,
            true
        )
        grutSteps.createClient(userInfo.clientId)
    }

    fun isFeatureEnabled() = listOf(true, false)

    @Test
    @TestCaseName("Test case for feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun startUacDraftCampaign_WithProcessedAudienceSegment(isFeatureEnabled: Boolean) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.CHECK_AUDIENCE_SEGMENTS_DEFERRED, isFeatureEnabled)
        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS)
        dbQueueSteps.clearQueue(DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS)

        val retargetingCondition = createRetargetingCondition(segmentStatus = SegmentStatus.IS_UPDATED)
        val campaignId = createDefaultUacContentCampaignReturnId(retargetingCondition = retargetingCondition)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = true,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
        )

        val jobId = dbQueueSteps.getLastJobByType(userInfo.shard, DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS)
        if (!isFeatureEnabled) {
            Assert.assertThat("джоба не должна была поставиться в очередь", jobId, Matchers.nullValue())
        } else {
            Assert.assertThat("джоба должна была появиться в очереди", jobId, Matchers.notNullValue())
            val dbJob = dbQueueRepository.findJobById(userInfo.shard, DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS, jobId)
            Assert.assertThat("джоба должна была появиться в очереди", dbJob, Matchers.notNullValue())
            Assert.assertThat(
                "джоба в очереди имеет правильный идентификатор кампании в параметрах",
                dbJob?.args?.uacCampaignId,
                Matchers.`is`(campaignId)
            )
        }
    }

    private fun createRetargetingCondition(
        segmentStatus: SegmentStatus
    ): UacRetargetingCondition {
        val audienceGoal = TestFullGoals.defaultAudience()
        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(listOf(audienceGoal))

        metrikaClientStub.addGoals(userInfo.uid, setOf(audienceGoal))
        Mockito.`when`(yaAudienceClient.getSegments(eq(userInfo.login)))
            .thenReturn(
                listOf(
                    AudienceSegment()
                        .withStatus(segmentStatus)
                        .withId(audienceGoal.id)
                        .withName("Uploaded")
                )
            )
        return UacRetargetingCondition(
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
            name = "Условие ретаргетинга",
            id = 1
        )
    }

    override fun getCampaign(campaignId: String): UacYdbCampaign {
        return grutApiService.briefGrutApi.getBrief(campaignId.toIdLong())!!.toUacYdbCampaign()
    }

    override fun getDirectCampaignId(uacCampaignId: String): Long {
        return uacCampaignId.toLong()
    }

    override fun getDirectCampaignStatus(campaign: UacYdbCampaign): DirectCampaignStatus? {
        return campaign.directCampaignStatus
    }

    override fun createAsset(mediaType: ru.yandex.direct.core.entity.uac.model.MediaType): String {
        return when (mediaType) {
            ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE -> grutSteps.createDefaultImageAsset(userInfo.clientId)
            ru.yandex.direct.core.entity.uac.model.MediaType.HTML5 -> grutSteps.createDefaultHtml5Asset(userInfo.clientId)
            else -> grutSteps.createDefaultVideoAsset(userInfo.clientId)
        }
    }
}
