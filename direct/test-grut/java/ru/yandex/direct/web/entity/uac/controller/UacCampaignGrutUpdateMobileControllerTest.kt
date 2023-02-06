package ru.yandex.direct.web.entity.uac.controller

import com.google.common.collect.Lists
import com.nhaarman.mockitokotlin2.eq
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.Assert
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toEAgePoint
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toEGender
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.AltAppStore
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest
import java.math.BigDecimal
import java.util.EnumSet

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignGrutUpdateMobileControllerTest : UacCampaignUpdateControllerTestBase() {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    private fun addCampaignAggregatedStatus(campaignId: Long, aggrData: AggregatedStatusCampaignData) {
        dslContextProvider.ppc(shardHelper.getShardByCampaignId(campaignId))
            .insertInto(
                Tables.AGGR_STATUSES_CAMPAIGNS,
                Tables.AGGR_STATUSES_CAMPAIGNS.CID,
                Tables.AGGR_STATUSES_CAMPAIGNS.AGGR_DATA
            )
            .values(campaignId, JsonUtils.toJson(aggrData))
            .execute()
    }

    @Before
    fun grutBefore() {
        steps.featureSteps().addClientFeature(
            clientInfo.clientId, FeatureName.UC_UAC_CREATE_MOBILE_CONTENT_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true
        )
        grutSteps.createClient(clientInfo.clientId!!)

        uacCampaignId =
            grutSteps.createMobileAppCampaign(clientInfo, createInDirect = true, createImageContent = true).toString()
        addCampaignAggregatedStatus(
            uacCampaignId.toIdLong(), AggregatedStatusCampaignData(
                listOf(),
                CampaignCounters(
                    1,
                    mapOf(GdSelfStatusEnum.RUN_WARN to 1),
                    mapOf(AdGroupStatesEnum.BS_RARELY_SERVED to 1)
                ),
                GdSelfStatusEnum.RUN_OK,
                GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS,
            )
        )

        draftUacCampaignId =
            grutSteps.createMobileAppCampaign(clientInfo, createInDirect = true, startedAt = null).toString()
        moderatingCampaignId =
            grutSteps.createMobileAppCampaign(clientInfo, createInDirect = true).toString()
        addCampaignAggregatedStatus(
            moderatingCampaignId.toIdLong(), AggregatedStatusCampaignData(
                listOf(),
                CampaignCounters(
                    1,
                    mapOf(GdSelfStatusEnum.RUN_WARN to 1),
                    mapOf(AdGroupStatesEnum.BS_RARELY_SERVED to 1)
                ),
                GdSelfStatusEnum.ON_MODERATION,
                GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS,
            )
        )
        uacCampaignWithoutCpaId =
            grutSteps.createMobileAppCampaign(clientInfo, createInDirect = true, cpa = null).toString()
    }

    fun demographicsParameters() = Lists.cartesianProduct(
        AgePoint.values().toList(), Gender.values().toList()
    )

    @Test
    @TestCaseName("Update mobile campaign doesnt affect adjustments with {0} age and {1} gender")
    @Parameters(method = "demographicsParameters")
    fun testUpdate_DontAffectsAdjustments(age: AgePoint, gender: Gender) {
        var request = emptyPatchRequest().copy(
            adjustments = listOf(
                UacAdjustmentRequest(
                    region = 225,
                    age = null,
                    gender = null,
                    percent = 50,
                    retargetingConditionId = null
                ),
                UacAdjustmentRequest(
                    region = null,
                    age = age,
                    gender = gender,
                    percent = 100,
                    retargetingConditionId = null
                ),
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        request = emptyPatchRequest().copy(
            regions = listOf(1L, 213L)
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        grutApiService.briefGrutApi.getBrief(uacCampaignId.toIdLong())!!.apply {
            spec.campaignBrief.apply {
                adjustmentsList.checkSize(2)
                adjustmentsList[0].age.checkEquals(ru.yandex.grut.objects.proto.AgePoint.EAgePoint.AP_NOT_SPECIFIED)
                adjustmentsList[0].gender.checkEquals(ru.yandex.grut.objects.proto.Gender.EGender.G_UNKNOWN)
                adjustmentsList[0].region.checkEquals(225L)
                adjustmentsList[0].percent.checkEquals(50)

                adjustmentsList[1].age.checkEquals(age.toEAgePoint())
                adjustmentsList[1].gender.checkEquals(gender.toEGender())
                adjustmentsList[1].region.checkEquals(0L)
                adjustmentsList[1].percent.checkEquals(100)
            }
        }
    }

    @Test
    fun test_UpdateWithShowTitleAndBody() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DISABLE_VIDEO_CREATIVE, true)
        val request = emptyPatchRequest().copy(
            showTitleAndBody = true
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        grutApiService.briefGrutApi.getBrief(uacCampaignId.toIdLong())!!.apply {
            spec.campaignBrief.apply {
                showTitleAndBody.checkEquals(true)
            }
        }
    }

    @Test
    fun test_UpdateWithShowTitleAndBody_WithoutFlag() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DISABLE_VIDEO_CREATIVE, false)
        val request = emptyPatchRequest().copy(
            showTitleAndBody = true
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Suppress("unused")
    fun testData() = listOf(
        listOf(
            true,
            setOf(
                AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE,
                AltAppStore.SAMSUNG_GALAXY_STORE, AltAppStore.XIAOMI_GET_APPS
            ),
            EnumSet.of(
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE,
                MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE, MobileAppAlternativeStore.XIAOMI_GET_APPS
            ),
            false,
            null
        ),
        listOf(true, null, null, false, null),
        listOf(false, null, null, false, null),
        listOf(
            true,
            null,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS),
            true,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS)
        ),
        listOf(
            true,
            setOf(AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE),
            EnumSet.of(MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE),
            true,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS)
        ),
        listOf(
            true,
            emptySet<AltAppStore>(),
            EnumSet.noneOf(MobileAppAlternativeStore::class.java),
            true,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS)
        ),
        listOf(
            true,
            emptySet<AltAppStore>(),
            EnumSet.noneOf(MobileAppAlternativeStore::class.java),
            false,
            null
        ),
    )

    @Test
    @Parameters(method = "testData")
    fun testUpdateCampaignWithAltAppStores(
        enableAltAppStores: Boolean,
        altAppStores: Set<AltAppStore>?,
        expectedAltAppStores: Set<MobileAppAlternativeStore>?,
        campaignWithAltAppStores: Boolean,
        altAppStoresBefore: Set<MobileAppAlternativeStore>?,
    ) {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.ENABLE_ALTERNATIVE_STORES_IN_UAC, enableAltAppStores)
        val request = emptyPatchRequest().copy(
            altAppStores = altAppStores,
            targetId = TargetType.CPC,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CLICK,
                UacStrategyData(
                    payForConversion = true,
                    avgBid = BigDecimal.valueOf(15)
                )
            )
        )

        if (campaignWithAltAppStores) {
            uacCampaignId = grutSteps.createMobileAppCampaign(
                clientInfo,
                createInDirect = true,
                createImageContent = true,
                altAppStores = altAppStoresBefore
            ).toString()

            val directCampaignBefore = getDirectCampaign(clientInfo.shard, uacCampaignId)
            assertThat(directCampaignBefore.alternativeAppStores).isEqualTo(EnumSet.copyOf(altAppStoresBefore))
        }

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)

        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign!!.id)
        assertThat(directCampaign.alternativeAppStores).isEqualTo(expectedAltAppStores)
    }

    fun isFeatureEnabled() = listOf(true, false)

    @Test
    @TestCaseName("Test case for feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun testUpdate_WithProcessedSegment(isFeatureEnabled: Boolean) {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.CHECK_AUDIENCE_SEGMENTS_DEFERRED, isFeatureEnabled)
        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS)
        dbQueueSteps.clearQueue(DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS)

        val audienceGoal = TestFullGoals.defaultAudience()
        mockAudienceRequest(SegmentStatus.UPLOADED, audienceGoal.id)
        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(listOf(audienceGoal))

        metrikaClientStub.addGoals(clientInfo.uid, setOf(audienceGoal))
        val retargetingCondition = UacRetargetingCondition(
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
        )
        val directRetargetingCondition = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition,
            clientInfo.clientId!!.asLong(),
            type = ConditionType.metrika_goals
        )
        val id = retargetingConditionService.addRetargetingConditions(
            listOf(directRetargetingCondition), clientInfo.clientId
        )[0].result

        val request = emptyPatchRequest().copy(
            retargetingCondition = UacRetargetingCondition(
                conditionRules = retargetingCondition.conditionRules,
                name = retargetingCondition.name,
                id = id
            )
        )
        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)

        grutApiService.briefGrutApi.getBrief(uacCampaignId.toIdLong())!!.apply {
            spec.campaignBrief.apply {
                Assert.assertThat("Поле правильно обновилось", audienceSegmentsSynchronized, Matchers.`is`(!isFeatureEnabled))
            }
        }
        val jobId = dbQueueSteps.getLastJobByType(clientInfo.shard, DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS)
        if (!isFeatureEnabled) {
            Assert.assertThat("джоба не должна была поставиться в очередь", jobId, Matchers.nullValue())
        } else {
            Assert.assertThat("джоба должна была появиться в очереди", jobId, Matchers.notNullValue())
            val dbJob = dbQueueRepository.findJobById(clientInfo.shard, DbQueueJobTypes.UAC_CHECK_AUDITION_SEGMENTS, jobId)
            Assert.assertThat("джоба должна была появиться в очереди", dbJob, Matchers.notNullValue())
            Assert.assertThat(
                "джоба в очереди имеет правильный идентификатор кампании в параметрах",
                dbJob?.args?.uacCampaignId,
                Matchers.`is`(uacCampaignId)
            )
        }
    }

    @Suppress("unused")
    fun testData1() = listOf(
        // не было сторов (null) и добавили новые сторы, statusBsSynced у баннеров сбрасываем
        listOf(
            true,
            null,
            setOf(
                AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE,
                AltAppStore.SAMSUNG_GALAXY_STORE, AltAppStore.XIAOMI_GET_APPS
            ),
            EnumSet.of(
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE,
                MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE, MobileAppAlternativeStore.XIAOMI_GET_APPS
            ),
            StatusBsSynced.NO
        ),
        // ничего не поменялось, флаг ENABLE_ALTERNATIVE_STORES_IN_UAC включен, statusBsSynced у баннеров не сбрасываем
        listOf(true, null, null, null, StatusBsSynced.YES),
        // ничего не поменялось, флаг ENABLE_ALTERNATIVE_STORES_IN_UAC выключен, statusBsSynced у баннеров не сбрасываем
        listOf(false, null, null, null, StatusBsSynced.YES),
        // было XIAOMI_GET_APPS, осталось XIAOMI_GET_APPS, statusBsSynced у баннеров не сбрасываем
        listOf(
            true,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS),
            null,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS),
            StatusBsSynced.YES
        ),
        // было XIAOMI_GET_APPS, обновили на HUAWEI_APP_GALLERY, VIVO_APP_STORE, statusBsSynced у баннеров сбрасываем
        listOf(
            true,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS),
            setOf(AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE),
            EnumSet.of(MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE),
            StatusBsSynced.NO
        ),
        // было XIAOMI_GET_APPS, обновили на пустой лист, statusBsSynced у баннеров сбрасываем
        listOf(
            true,
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS),
            emptySet<AltAppStore>(),
            EnumSet.noneOf(MobileAppAlternativeStore::class.java),
            StatusBsSynced.NO
        ),
    )

    @Test
    @Parameters(method = "testData1")
    fun testResetBannersWhenUpdateCampaignWithAltAppStores(
        enableAltAppStores: Boolean,
        altAppStoresBefore: Set<MobileAppAlternativeStore>?,
        altAppStoresToUpdate: Set<AltAppStore>?,
        expectedAltAppStores: Set<MobileAppAlternativeStore>?,
        expectedStatusBsSynced: StatusBsSynced,
    ) {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.ENABLE_ALTERNATIVE_STORES_IN_UAC, enableAltAppStores)
        val request = emptyPatchRequest().copy(
            altAppStores = altAppStoresToUpdate,
            targetId = TargetType.CPC,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CLICK,
                UacStrategyData(
                    payForConversion = true,
                    avgBid = BigDecimal.valueOf(15)
                )
            )
        )

        uacCampaignId = grutSteps.createMobileAppCampaign(
                clientInfo,
                createInDirect = true,
                createImageContent = true,
                altAppStores = altAppStoresBefore
            ).toString()

        val directCampaignBefore = getDirectCampaign(clientInfo.shard, uacCampaignId)
        // начальные альтернативные сторы
        assertThat(directCampaignBefore.alternativeAppStores?.toSet()).isEqualTo(altAppStoresBefore)

        val uacCampaign = campaignRepository.getCampaigns(
            clientInfo.shard,
            listOf(uacCampaignId.toIdLong())
        )[0]
        val campaignInfo = CampaignInfo(clientInfo, uacCampaign)
        val adGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo)
        val mobileAppBanner = TestBanners.activeMobileAppBanner(uacCampaignId.toIdLong(), adGroup.adGroupId)
            .withStatusBsSynced(StatusBsSynced.YES)

        steps.bannerSteps().createActiveMobileAppBanner(mobileAppBanner, adGroup)

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign.id)

        val soft = SoftAssertions()
        // альтернативные сторы обновились
        soft.assertThat(directCampaign.alternativeAppStores).isEqualTo(expectedAltAppStores)

        val banner = bannerTypedRepository.getTyped(
            clientInfo.shard,
            listOf(mobileAppBanner.id)
        ).first() as MobileAppBanner

        //statusBsSynced на всех баннерах кампании сбросился
        soft.assertThat(
            banner.statusBsSynced
        ).isEqualTo(expectedStatusBsSynced)

        soft.assertAll()
    }

    private fun mockAudienceRequest(segmentStatus: SegmentStatus, audienceGoalId: Long) {
        Mockito.`when`(yaAudienceClient.getSegments(eq(clientInfo.login)))
            .thenReturn(
                listOf(
                    AudienceSegment()
                        .withStatus(segmentStatus)
                        .withId(audienceGoalId)
                        .withName("Uploaded")
                )
            )
    }

    override fun getCampaign(id: String): UacYdbCampaign {
        return grutApiService.briefGrutApi.getBrief(id.toIdLong())!!.toUacYdbCampaign()
    }

    override fun getDirectCampaignId(uacCampaignId: String): Long {
        return uacCampaignId.toLong()
    }

    override fun createBanner(uacCampaignId: String) {
        val adGroupId = 123123L
        val bannerId = 321321L
        grutSteps.createAdGroup(uacCampaignId.toIdLong(), adGroupId)
        grutSteps.createBanner(uacCampaignId.toIdLong(), adGroupId, bannerId)
    }

    override fun createHtml5Content(): String {
        return grutSteps.createDefaultHtml5Asset(clientInfo.clientId!!)
    }

    override fun getDirectCampaign(shard: Int, campaignId: String) =
        campaignTypedRepository.getTyped(shard, listOf(campaignId.toIdLong()))[0] as MobileContentCampaign

    override fun generateMoney(value: Long) = BigDecimal.valueOf(value * 1_000_000, 6)
}
