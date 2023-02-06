package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.uac.createAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdsLong
import ru.yandex.direct.feature.FeatureName.UC_CUSTOM_AUDIENCE_ENABLED
import ru.yandex.direct.jobs.configuration.GrutJobsTest

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobForUcWithRetargetingForAdGroupBriefTest : GrutUpdateAdsJobForUcWithRetargetingBaseTest() {

    @BeforeEach
    fun before() {
        init(true)
    }

    /**
     * Проверка разных кейсов сохранения ретаргетинга с custom audience
     */
    @ParameterizedTest(name = "Feature {1}: {0}")
    @MethodSource("casesForCustomAudience")
    fun updateUcCampaignTest(
        description: String,
        featureEnabled: Boolean,
        goals: List<Goal>?,
    ) {
        val retargetingCondition = getUacRetargetingCondition(goals)
        createAdGroupBriefGrutModel(retargetingCondition)

        steps.featureSteps().addClientFeature(clientId, UC_CUSTOM_AUDIENCE_ENABLED, featureEnabled)

        updateAdsJob.withShard(shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, uid)
        checkRetargeting(if (featureEnabled) goals else null)
    }

    /**
     * Проверка разных кейсов обновления ретаргетинга с custom audience, когда у группы уже был ретаргетинг
     */
    @ParameterizedTest(name = "Feature {1}: {0}")
    @MethodSource("casesForCustomAudience")
    fun updateUcCampaignTest_WithRetargetingAlready(
        description: String,
        featureEnabled: Boolean,
        newGoals: List<Goal>?,
    ) {
        val oldGoals = listOf(GOAL_INTEREST, GOAL_HOST)
        val retargetingCondition = getUacRetargetingCondition(oldGoals)
        val adGroupBriefId = createAdGroupBriefGrutModel(retargetingCondition)

        steps.featureSteps().addClientFeature(clientId, UC_CUSTOM_AUDIENCE_ENABLED, true)

        updateAdsJob.withShard(shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, uid)
        checkRetargeting(oldGoals)

        steps.featureSteps().addClientFeature(clientId, UC_CUSTOM_AUDIENCE_ENABLED, featureEnabled)

        // Change retargeting in ad group request
        val newRetargetingCondition = getUacRetargetingCondition(newGoals)
        val adGroupBrief = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)!!
            .copy(retargetingCondition = newRetargetingCondition)
        grutApiService.adGroupBriefGrutApi.createOrUpdateAdGroupBrief(adGroupBrief)

        // Change briefSynced in campaign request
        val ucCampaign = grutUacCampaignService.getCampaignByDirectCampaignId(ucCampaignId)!!
            .copy(briefSynced = false)
        grutUacCampaignService.updateCampaign(ucCampaign)
        testCampaignRepository.setSource(shard, ucCampaignId, CampaignSource.UAC)

        updateAdsJob.processGrabbedJob(uacCampaignId, uid)
        checkRetargeting(if (featureEnabled) newGoals else null)
    }

    /**
     * Проверка разных кейсов обновления ретаргетинга с custom audience, когда группы была но у нее небыло ретаргетинга
     */
    @ParameterizedTest(name = "Feature {1}: {0}")
    @MethodSource("casesForCustomAudience")
    fun updateUcCampaignTest_WithoutRetargetingBefore(
        description: String,
        featureEnabled: Boolean,
        newGoals: List<Goal>?,
    ) {
        val adGroupBriefId = createAdGroupBriefGrutModel()

        steps.featureSteps().addClientFeature(clientId, UC_CUSTOM_AUDIENCE_ENABLED, false)

        updateAdsJob.withShard(shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, uid)
        checkRetargeting(null)

        steps.featureSteps().addClientFeature(clientId, UC_CUSTOM_AUDIENCE_ENABLED, featureEnabled)

        // Change retargeting in ad group request
        val newRetargetingCondition = getUacRetargetingCondition(newGoals)
        val adGroupBrief = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)!!
            .copy(retargetingCondition = newRetargetingCondition)
        grutApiService.adGroupBriefGrutApi.createOrUpdateAdGroupBrief(adGroupBrief)

        // Change briefSynced in campaign request
        val ucCampaign = grutUacCampaignService.getCampaignByDirectCampaignId(ucCampaignId)!!
            .copy(briefSynced = false)
        grutUacCampaignService.updateCampaign(ucCampaign)
        testCampaignRepository.setSource(shard, ucCampaignId, CampaignSource.UAC)

        updateAdsJob.processGrabbedJob(uacCampaignId, uid)
        checkRetargeting(if (featureEnabled) newGoals else null)
    }

    private fun createAdGroupBriefGrutModel(
        retargetingCondition: UacRetargetingCondition? = null,
    ): Long {
        createUcCampaign()

        val adGroupBriefIds = grutApiService.adGroupBriefGrutApi.createAdGroupBriefs(
            listOf(
                createAdGroupBriefGrutModel(
                    uacCampaignId.toIdLong(),
                    retargetingCondition = retargetingCondition,
                )
            )
        )
        assertThat(adGroupBriefIds)
            .`as`("Групповая заявка создана в груте")
            .isNotEmpty

        val assetIds = listOf(
            grutSteps.createTitleAsset(clientId, "Title"),
            grutSteps.createTextAsset(clientId, "Text"),
        )
        val assets = grutApiService.assetGrutApi.getAssets(assetIds.toIdsLong())
        assertThat(assets)
            .`as`("Ассеты созданы в груте")
            .hasSize(assetIds.size)

        val adGroupBriefAssets = assetIds.map { createCampaignContent(contentId = it) }
        grutSteps.setCustomAssetLinksToAdGroupBrief(adGroupBriefIds[0], adGroupBriefAssets)

        return adGroupBriefIds[0]
    }

    private fun createUcCampaign() {
        val ucCampaign = createYdbCampaign(
            id = ucCampaignId.toString(),
            advType = AdvType.TEXT,
            accountId = clientId.toString(),
            isEcom = false,
            briefSynced = false,
            zenPublisherId = "someId",
            retargetingCondition = null,
            startedAt = LocalDateTime.now(),
        )
        uacCampaignId = ucCampaign.id
        grutSteps.createTextCampaign(clientInfo, ucCampaign)
    }
}
